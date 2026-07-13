# CLAUDE.md — Impostor Game

This file is the source of truth for this project. Read it fully before making any changes.

---

## What is this project?

A multiplayer word-guessing game. All players receive a secret word. One player (the impostor)
receives a similar but different word. Players discuss and vote to find the impostor.
Players do not need to register — guests and registered users both play.

---

## Tech stack

- **Language:** Java 25 — **Framework:** Spring Boot 4.0.x
- **Build:** Maven monorepo with parent pom
- **Discovery:** Eureka — **Config:** Spring Cloud Config (Slice 5)
- **Sync:** REST via OpenFeign — **Async:** Apache Kafka
- **Cache:** Redis — **DB:** PostgreSQL (one per service) — **Migrations:** Flyway
- **Real-time:** WebSocket STOMP — **Testing:** JUnit 5, Mockito, Testcontainers
- **CI/CD:** GitHub Actions — **Observability:** Micrometer + Zipkin, Prometheus + Grafana (Slice 5)

> **Spring Boot 4 note — autoconfiguration is modularized.**
> The library on the classpath is no longer enough; you also need the Boot autoconfig module.
> Example: `flyway-core` alone does **not** autoconfigure Flyway (migrations silently never run,
> not even a log line). You must declare `spring-boot-starter-flyway` — which brings
> `FlywayAutoConfiguration` — plus `flyway-database-postgresql` for the dialect.
> `flyway-core` then arrives as a transitive of the starter; do not declare it explicitly.
> Same pattern applies to other technologies: **library ≠ Boot autoconfig module**.
> Diagnose by inspecting jars: `find ~/.m2/repository/org/springframework/boot -name "*<tech>*"`.

---

## Architecture

### All services — Hexagonal Architecture + DDD

Every service follows this structure:

```
{service}/
└── src/main/java/com/impostorgame/{service}/
    ├── domain/                    ← pure Java, zero framework dependencies
    │   ├── model/                 ← aggregates, entities, value objects
    │   ├── event/                 ← domain events
    │   ├── exception/             ← domain exceptions
    │   └── port/
    │       ├── in/                ← use case interfaces (driving ports)
    │       └── out/               ← repository + external service interfaces (driven ports)
    │
    ├── application/               ← implements port/in, orchestrates domain
    │   └── service/               ← one class per use case
    │
    └── infrastructure/            ← all Spring/JPA/Kafka/Redis code
        ├── adapter/
        │   ├── in/web/            ← REST controllers
        │   └── out/
        │       ├── persistence/   ← primary store: JPA (Postgres) or Redis when Redis is the source of truth
        │       ├── messaging/     ← Kafka producers + consumers
        │       └── cache/         ← Redis used only as a derived cache (data whose truth lives elsewhere)
        └── config/                ← Spring beans
```

**Dependency rule:** `domain` ← `application` ← `infrastructure`. Never reversed.
`domain` has zero Spring/JPA/Kafka imports. It is pure Java.

---

## Services

| Service              | Slice | Port | Architecture        |
|----------------------|-------|------|---------------------|
| discovery-server     | 1     | 8761 | Infrastructure only |
| auth-service         | 1     | 8081 | Hexagonal + DDD     |
| game-service         | 1+3   | 8082 | Hexagonal + DDD     |
| word-service         | 2     | 8083 | Hexagonal + DDD     |
| voting-service       | 3     | 8084 | Hexagonal + DDD     |
| player-service       | 3     | 8085 | Hexagonal + DDD     |
| notification-service | 4     | 8086 | Hexagonal + DDD     |
| api-gateway          | 5     | 8080 | Infrastructure only |
| config-server        | 5     | 8888 | Infrastructure only |

---

## Authentication — JWT RS256 + JWKS

- **auth-service** is the only service that holds the RSA **private key**. It signs tokens.
- All other services are **OAuth2 resource servers**: they validate tokens by fetching the
  public key from auth-service's JWKS endpoint (`/.well-known/jwks.json`, `kid: auth-key-1`).
- A resource server **never** holds a signing key. Private-key config in a resource server is
  a security error, not a convenience.
- **RS256 over HMAC:** an HMAC shared secret would let any validating service forge tokens.
  RS256 confines forgery capability to auth-service alone.

JWT claims consumed downstream: `sub` → playerId, `displayName`, `role` (`GUEST` | `USER`).

---

## Guest vs registered players

|                    | Guest          | Registered          |
|--------------------|----------------|---------------------|
| JWT role           | `GUEST`        | `USER`              |
| Expiry             | 4h, no refresh | 24h + refresh token |
| Persisted          | Never          | Yes                 |
| Play game          | ✅              | ✅                   |
| Stats/leaderboard  | ❌              | ✅                   |
| Reconnect          | ❌              | ✅                   |
| Convert to account | ✅              | —                   |

`PlayerContext` sealed interface used in `game-service` and `voting-service`:

```java
public sealed interface PlayerContext permits RegisteredPlayer, GuestPlayer {
    String id();

    String displayName();

    boolean isGuest();
}
```

Never extract this to a shared module — duplicate it per service.

---

## Game state machine

```
LOBBY → WORD_ASSIGNMENT → DISCUSSION → VOTING → RESULTS → LOBBY
```

Manual FSM using switch expression (not Spring State Machine library).
Lives in `game-service` domain layer. Other services never call it directly —
they publish Kafka events and `game-service` reacts.

```java
GamePhase next = switch (room.phase()) {
    case LOBBY -> event == GAME_STARTED ? WORD_ASSIGNMENT : invalid(event);
    case WORD_ASSIGNMENT -> event == WORDS_DEALT ? DISCUSSION : invalid(event);
    case DISCUSSION -> event == TIMER_EXPIRED ? VOTING : invalid(event);
    case VOTING -> event == VOTE_COMPLETE ? RESULTS : invalid(event);
    case RESULTS -> event == RESET ? LOBBY : invalid(event);
};
```

---

## Kafka topics

| Topic            | Producer       | Consumers                            | Events                                                             |
|------------------|----------------|--------------------------------------|--------------------------------------------------------------------|
| `game.events`    | game-service   | notification-service, player-service | `GAME_STARTED`, `PHASE_CHANGED`, `PLAYER_ELIMINATED`, `GAME_ENDED` |
| `voting.results` | voting-service | game-service, notification-service   | `VOTING_FINISHED`                                                  |
| `player.actions` | game-service   | notification-service, player-service | `PLAYER_JOINED`, `PLAYER_LEFT`                                     |

**Ownership principle — each service publishes only events about what it owns.**

- `voting-service` owns the tally, not the consequences. It publishes a neutral
  `VOTING_FINISHED` (who received the most votes). It does not decide who is out.
- `game-service` owns rooms, players and game rules. It consumes `VOTING_FINISHED`,
  applies the rules, and publishes `PLAYER_ELIMINATED` / `GAME_ENDED`.
- `game-service` owns room membership, so it publishes `PLAYER_JOINED` / `PLAYER_LEFT`.
- `notification-service` is a **translator, never a source of domain facts**: it consumes
  neutral events and turns them into client-facing messages. It publishes nothing to
  domain topics.

**Kafka carries domain facts, not UI messages.**

---

## Data storage

Each service owns its own isolated database. No cross-service DB access ever.

| Service              | PostgreSQL            | Redis             |
|----------------------|-----------------------|-------------------|
| auth-service         | auth_db               | —                 |
| game-service         | game_db (rounds only) | room state (TTL)  |
| word-service         | word_db               | assignments (TTL) |
| voting-service       | voting_db             | active vote (TTL) |
| player-service       | player_db             | —                 |
| notification-service | —                     | pub/sub relay     |

Game rooms live in Redis during active play. Only completed round summaries go to PostgreSQL.

**Redis: persistence vs. cache.** When Redis is the primary store (room state, assignments,
active vote), the adapter lives in `infrastructure/adapter/out/persistence/` — not `cache/`.
`cache/` is only for derived data whose truth lives elsewhere.

**Reconstitution vs. creation.** Aggregates need separate factory methods:
`Room.create` applies birth invariants; `Room.restore` rehydrates from persistence and must
**not** re-apply them.

---

## Iterative delivery — 5 slices

| Slice                     | Done when                                                       | Status                |
|---------------------------|-----------------------------------------------------------------|-----------------------|
| 1 — "A room exists"       | Player gets token, creates/joins room, appears in Eureka        | ✅ Verified end-to-end |
| 2 — "Words assigned"      | Host starts game, words assigned, one player gets impostor word | ← current             |
| 3 — "Full round playable" | Lobby → words → discussion → voting → results                   |                       |
| 4 — "Feels like a game"   | Live WebSocket updates, no polling                              |                       |
| 5 — "Production ready"    | Gateway, config server, CI/CD, tracing                          |                       |

---

## Testing conventions

- **Unit** (`unit/`): `@ExtendWith(MockitoExtension.class)`, no Spring context, `@Tag("unit")`
- **Integration** (`integration/`): `@SpringBootTest` + Testcontainers, `@Tag("integration")`
- **Contract** (Slice 5): Spring Cloud Contract on `word-service` assign endpoint
- Tests mirror the production package structure
- Write tests before or immediately after each feature — never at the end of a slice
- **TDD from `word-service` onwards**
- `assertThatThrownBy`: extract setup into local variables before the lambda (SonarQube rule)

---

## Environment and profiles

| File                      | Purpose                             | Committed |
|---------------------------|-------------------------------------|-----------|
| `application.yaml`        | Shared non-sensitive config         | ✅         |
| `application-local.yaml`  | Local dev URLs (`localhost`)        | ✅         |
| `application-docker.yaml` | Docker/homelab URLs (service names) | ✅         |
| `.env`                    | Secrets and passwords               | ❌ Never   |

Extension is `.yaml`, not `.yml`. Consistently.

**Run locally** — load the env file first, then activate the profile:

```bash
set -a; source ../../.env; set +a
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Run in Docker:** `SPRING_PROFILES_ACTIVE=docker`

> Between-session startup failures are almost always **execution context** issues —
> missing `.env` load, missing profile activation, wrong working directory — not code problems.
> Check those three before debugging the code.

---

## Hard rules — never violate these

- No service imports or queries another service's database
- No shared business logic in the monorepo — versions only in root pom
- `domain/` layer has zero Spring/JPA/Kafka imports
- Controllers call use case interfaces (`port/in`) — never application services directly
- Domain entities never returned from controllers — always map to DTOs
- `PlayerContext` duplicated per service — never extracted to shared module
- State machine only called from within `game-service` — never from outside
- Guests never persisted in any database
- Kafka consumers must be idempotent — same event delivered twice must be safe
- Only auth-service holds the RSA private key — resource servers validate via JWKS
- Each service publishes only events about what it owns
- Generic 500 handlers never expose `ex.getMessage()` — it leaks internals

### Injection convention

- Inyectable classes (application services, adapters, controllers):
  explicit constructor, `private final` fields, no `@RequiredArgsConstructor`.
  Single constructor → Spring injects without `@Autowired`.
- **Lombok is allowed only on structural types**: classes that carry data and hold
  no invariants — DTOs and persistence entities (JPA *and* Redis).
  Allowed there: `@Getter`, `@Setter`, `@Builder`, `@AllArgsConstructor`,
  `@NoArgsConstructor`. JPA *requires* a no-args constructor; a persistence entity
  is a mapping row, not the aggregate — it has no rules to protect.
- **Lombok is forbidden on behavioural types**: anything in `domain/`, and any
  injectable class (application services, adapters, controllers).
  The criterion is not the technology, it is whether the class has invariants
  or collaborators to protect.
- domain/ stays Lombok-free: constructors/factories are hand-written
  because they validate invariants.
- `@Repository` on all persistence adapters (including Redis) for semantic uniformity.

### Commit convention

Conventional Commits, atomic by logical unit:
`feat`, `test`, `refactor`, `chore`, `docs`, `build`, `style`.