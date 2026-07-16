# CLAUDE.md — Impostor Game

Source of truth. Read fully before changing anything.

## Project

Multiplayer word game. Everyone gets a secret word; one impostor gets a similar-but-different
word. Players discuss and vote out the impostor. Guests and registered users both play.

## Stack

Java 25, Spring Boot 4.0.x, Maven monorepo. Eureka (discovery), Spring Cloud Config (Slice 5).
REST via OpenFeign, async via Kafka. Redis + PostgreSQL (one DB per service), Flyway migrations.
WebSocket STOMP. JUnit 5 + Mockito + Testcontainers. Observability in Slice 5.

> **Boot 4: library ≠ autoconfig module.** A jar on the classpath doesn't autoconfigure itself.
> E.g. `flyway-core` alone runs no migrations (silently); you need `spring-boot-starter-flyway`
> + `flyway-database-postgresql`. Diagnose with
    > `find ~/.m2/repository/org/springframework/boot -name "*<tech>*"`.

## Architecture — Hexagonal + DDD (every service)

```
domain/          ← pure Java, ZERO framework imports. model/ event/ exception/ port/{in,out}/
application/      ← implements port/in, orchestrates domain. service/ security/
infrastructure/   ← all Spring/JPA/Kafka/Redis. adapter/{in/web, out/{persistence,messaging,cache}} config/
```

**Dependency rule:** `domain ← application ← infrastructure`. Never reversed.

- Redis in `out/persistence/` when it's the source of truth; `out/cache/` only for derived data.
- Aggregates: `create()` applies birth invariants; `restore()` rehydrates without re-applying them.

## Services

| Service | Slice | Port | | Service | Slice | Port |
|---|---|---|---|---|---|---|
| discovery-server | 1 | 8761 | | word-service | 2 | 8083 |
| auth-service | 1 | 8081 | | voting-service | 3 | 8084 |
| game-service | 1+3 | 8082 | | player-service | 3 | 8085 |
| notification-service | 4 | 8086 | | api-gateway | 5 | 8080 |
| config-server | 5 | 8888 | | | | |

## Auth — JWT RS256 + JWKS

- **auth-service holds the only RSA private key** and signs tokens. It's an *authorization
  server*, not a resource server: login is a use case (`LoginService`), not a Security filter.
  No `UserDetailsService`/`AuthenticationManager`/`JwtDecoder`; stateless; chain ends in
  `denyAll()` (fail closed).
- **All other services are resource servers.** They validate via the JWKS endpoint
  (`/.well-known/jwks.json`, `kid: auth-key-1`) and never hold a signing key.
  `RS256 over HMAC`: a shared secret would let any service forge tokens.
- **`RequiredClaimsValidator`** rejects malformed JWTs at the `JwtDecoder` (missing
  `sub`/`displayName`/`role`, or unknown `role`), combined with `JwtValidators.createDefault()`
  via `DelegatingOAuth2TokenValidator` — keep the defaults or expired tokens pass. Validate at
  the edge, once, not per controller.
- **Refresh tokens hashed (SHA-256) before persistence.** Plaintext goes to the client once,
  never stored. Lookup by hash (`findByTokenHash`). `TokenHasher` in `application/security/`;
  `RefreshTokenIssuer` centralizes generate→hash→persist→return-plaintext.
- **JWT expirations are `Duration`** in `JwtProperties` (`24h`/`4h`/`30d`), not millis.

Claims consumed downstream: `sub`→playerId, `displayName`, `role` (`GUEST`|`USER`).

## Guest vs registered

| | Guest | Registered |
|---|---|---|
| role / expiry | `GUEST`, 4h, no refresh | `USER`, 24h + refresh |
| persisted / stats / reconnect | never / ❌ / ❌ | yes / ✅ / ✅ |

`PlayerContext` sealed interface (`id()`, `displayName()`, `isGuest()`) permits
`RegisteredPlayer`, `GuestPlayer`. **Duplicated per service, never shared.** `Role` too.
`RoomPlayer` factories: `host(PlayerContext)` / `member(PlayerContext)` for creation (intent in
the name, not a boolean); `restore(...)` for rehydration.

## Game state machine

`LOBBY → WORD_ASSIGNMENT → DISCUSSION → VOTING → RESULTS → LOBBY`. Manual FSM (switch
expression, not Spring State Machine), in `game-service` domain. Others never call it — they
publish Kafka events and game-service reacts.

## Kafka — each service publishes only what it owns

| Topic | Producer | Events |
|---|---|---|
| `game.events` | game-service | `GAME_STARTED`, `PHASE_CHANGED`, `PLAYER_ELIMINATED`, `GAME_ENDED` |
| `voting.results` | voting-service | `VOTING_FINISHED` (neutral tally — doesn't decide who's out) |
| `player.actions` | game-service | `PLAYER_JOINED`, `PLAYER_LEFT` |

game-service consumes `VOTING_FINISHED`, applies rules, emits consequences.
notification-service translates neutral events into client messages; publishes to no domain
topic. Consumers must be idempotent. Kafka carries domain facts, not UI messages.

## Data storage

DB per service, no cross-service DB access. game-service: rooms in Redis (TTL) during play,
completed round summaries in Postgres.

- **Domain owns id + timestamps.** `create()` generates `UUID.randomUUID()` and `createdAt`.
  So JPA entities: no `@GeneratedValue`; implement `Persistable<UUID>` with a `@Transient isNew`
  flag (`@Builder.Default` mandatory — Lombok ignores field initializers) or Spring Data does
  `merge` (redundant SELECT) instead of `persist`.
- **FKs are plain UUID columns, not `@ManyToOne`** (no object navigation needed).
  `ON DELETE CASCADE` lives in the SQL migration.

## Testing

- Unit: `@Tag("unit")`, `MockitoExtension`, no Spring context. Integration: `@Tag("integration")`,
  `@SpringBootTest` + Testcontainers.
- Tests mirror package structure. **TDD from word-service onwards.**
- `assertThatThrownBy`: extract setup to locals before the lambda (SonarQube).
- Don't mock value objects — construct them real.
- Jakarta Validation lives in `application` DTOs; `domain` framework-free; web DTOs not
  duplicated (deliberate tradeoff).

## Environment

`.yaml` not `.yml`. `application.yaml` (shared), `-local` (localhost), `-docker` (service names),
`-test` (Testcontainers, `eureka.client.enabled: false`). `.env` never committed.
Run local: `set -a; source ../../.env; set +a` then `mvn spring-boot:run -Dspring-boot.run.profiles=local`.

> Startup failures between sessions are almost always execution context — missing `.env`,
> missing profile, wrong dir — not code.

## Hard rules

- No service touches another's DB. No shared business logic (versions only in root pom).
- `domain/` has zero Spring/JPA/Kafka imports.
- Controllers call `port/in`, never application services. Never return domain entities — map to DTOs.
- `PlayerContext` / `Role` duplicated per service, never shared.
- FSM only called inside game-service. Guests never persisted. Kafka consumers idempotent.
- Only auth-service holds the private key. Each service emits only its own events.
- Generic 500 handlers never expose `ex.getMessage()`.

**Injection:** explicit constructor, `private final`, no `@RequiredArgsConstructor`.
**Lombok** only on structural types (DTOs, JPA/Redis entities — data, no invariants); forbidden
in `domain/` and on any injectable class. `@Repository` on all persistence adapters.

**Exceptions:** domain invariants throw domain exceptions (one per aggregate:
`InvalidUserException`, `InvalidRoomException`…), never raw `IllegalArgumentException` (libraries
throw that too). `@RestControllerAdvice` maps: domain → 400, validation → 400,
`DataIntegrityViolationException` → 409, `Exception` → 500 (fixed message, log full exception).

**Commits:** Conventional Commits, atomic = tree compiles at every commit. A refactor spanning
domain→port→adapter→services is *one* commit, not one per layer.

## Known debt

- No rate limiting / lockout on login (tradeoff of skipping the Security filter).
- `expiresIn` recomputed in each service instead of returned by `JwtPort`.
- Password `@Size(max=72)` counts chars; BCrypt truncates by bytes (fine in practice).
- game-service tests in `game/unit/` instead of mirroring packages.
- `RefreshTokenRepository.deleteByUserId` unused (reserved for logout).