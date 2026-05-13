# CLAUDE.md — Impostor Game

This file is the source of truth for this project. Read it fully before making any changes.

---

## What is this project?

A multiplayer word-guessing game. All players receive a secret word. One player (the impostor)
receives a similar but different word. Players discuss and vote to find the impostor.
Players do not need to register — guests and registered users both play.

---

## Tech stack

- **Language:** Java 21 — **Framework:** Spring Boot 3.5.x
- **Build:** Maven monorepo with parent pom
- **Discovery:** Eureka — **Config:** Spring Cloud Config (Slice 5)
- **Sync:** REST via OpenFeign — **Async:** Apache Kafka
- **Cache:** Redis — **DB:** PostgreSQL (one per service) — **Migrations:** Flyway
- **Real-time:** WebSocket STOMP — **Testing:** JUnit 5, Mockito, Testcontainers
- **CI/CD:** GitHub Actions — **Observability:** Micrometer + Zipkin, Prometheus + Grafana (Slice 5)

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
        │       ├── persistence/   ← JPA entities + repositories implementing port/out
        │       ├── messaging/     ← Kafka producers + consumers
        │       └── cache/         ← Redis
        └── config/                ← Spring beans
```

**Dependency rule:** `domain` ← `application` ← `infrastructure`. Never reversed.
`domain` has zero Spring/JPA/Kafka imports. It is pure Java.

---

## Services

| Service | Slice | Port | Architecture |
|---|---|---|---|
| discovery-server | 1 | 8761 | Infrastructure only |
| auth-service | 1 | 8081 | Hexagonal + DDD |
| game-service | 1+3 | 8082 | Hexagonal + DDD |
| word-service | 2 | 8083 | Hexagonal + DDD |
| voting-service | 3 | 8084 | Hexagonal + DDD |
| player-service | 3 | 8085 | Hexagonal + DDD |
| notification-service | 4 | 8086 | Hexagonal + DDD |
| api-gateway | 5 | 8080 | Infrastructure only |
| config-server | 5 | 8888 | Infrastructure only |

---

## Guest vs registered players

| | Guest | Registered |
|---|---|---|
| JWT role | `GUEST` | `USER` |
| Expiry | 4h, no refresh | 24h + refresh token |
| Persisted | Never | Yes |
| Play game | ✅ | ✅ |
| Stats/leaderboard | ❌ | ✅ |
| Reconnect | ❌ | ✅ |
| Convert to account | ✅ | — |

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
    case LOBBY           -> event == GAME_STARTED  ? WORD_ASSIGNMENT : invalid(event);
    case WORD_ASSIGNMENT -> event == WORDS_DEALT   ? DISCUSSION      : invalid(event);
    case DISCUSSION      -> event == TIMER_EXPIRED ? VOTING          : invalid(event);
    case VOTING          -> event == VOTE_COMPLETE ? RESULTS         : invalid(event);
    case RESULTS         -> event == RESET         ? LOBBY           : invalid(event);
};
```

---

## Kafka topics

| Topic | Producer | Consumers | Events |
|---|---|---|---|
| `game.events` | game-service | notification-service, player-service | `GAME_STARTED`, `PHASE_CHANGED`, `GAME_ENDED` |
| `voting.results` | voting-service | game-service, notification-service | `VOTING_COMPLETE`, `PLAYER_ELIMINATED` |
| `player.actions` | notification-service | game-service | `PLAYER_JOINED`, `PLAYER_LEFT` |

---

## Data storage

Each service owns its own isolated database. No cross-service DB access ever.

| Service | PostgreSQL | Redis |
|---|---|---|
| auth-service | auth_db | — |
| game-service | game_db (rounds only) | room state (TTL) |
| word-service | word_db | assignments (TTL) |
| voting-service | voting_db | active vote (TTL) |
| player-service | player_db | — |
| notification-service | — | pub/sub relay |

Game rooms live in Redis during active play. Only completed round summaries go to PostgreSQL.

---

## Iterative delivery — 5 slices

| Slice | Done when |
|---|---|
| 1 — "A room exists" | Player gets token, creates/joins room, appears in Eureka |
| 2 — "Words assigned" | Host starts game, words assigned, one player gets impostor word |
| 3 — "Full round playable" | Lobby → words → discussion → voting → results |
| 4 — "Feels like a game" | Live WebSocket updates, no polling |
| 5 — "Production ready" | Gateway, config server, CI/CD, tracing |

---

## Testing conventions

- **Unit** (`unit/`): `@ExtendWith(MockitoExtension.class)`, no Spring context, `@Tag("unit")`
- **Integration** (`integration/`): `@SpringBootTest` + Testcontainers, `@Tag("integration")`
- **Contract** (Slice 5): Spring Cloud Contract on `word-service` assign endpoint
- Write tests before or immediately after each feature — never at the end of a slice
- TDD from `word-service` onwards

---

## Environment and profiles

| File | Purpose | Committed |
|---|---|---|
| `application.yml` | Shared non-sensitive config | ✅ |
| `application-local.yml` | Local dev URLs | ✅ |
| `application-docker.yml` | Docker/homelab URLs | ✅ |
| `.env` | Secrets and passwords | ❌ Never |

Run locally: `mvn spring-boot:run -Dspring-boot.run.profiles=local`
Run in Docker: `SPRING_PROFILES_ACTIVE=docker`

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