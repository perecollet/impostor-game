# CLAUDE.md — Impostor Game

This file is the source of truth for this project. Read it fully before making any changes.
It contains all architectural decisions, conventions, and context agreed during design.

---

## What is this project?

A multiplayer word-guessing game. All players receive a secret word. One player (the impostor)
receives a similar but different word (e.g. everyone gets "apple", impostor gets "pear").
Players discuss and vote to find the impostor.

**Players do not need to register.** They can join as guests or as registered users.

---

## Tech stack

- **Language:** Java 21
- **Framework:** Spring Boot 3.2.x
- **Build:** Maven (monorepo with parent pom)
- **Service discovery:** Eureka (Spring Cloud)
- **Config:** Spring Cloud Config (added in Slice 5)
- **Sync communication:** REST via OpenFeign
- **Async communication:** Apache Kafka
- **Cache:** Redis
- **Database:** PostgreSQL (one isolated DB per service)
- **Migrations:** Flyway
- **Real-time:** WebSocket with STOMP
- **Testing:** JUnit 5, Mockito, Testcontainers, Spring Cloud Contract
- **CI/CD:** GitHub Actions
- **Observability:** Micrometer + Zipkin, Prometheus + Grafana (Slice 5)

---

## Monorepo structure

```
impostor-game/
├── pom.xml                          ← root parent, created manually
├── .github/workflows/
│   ├── ci.yml
│   ├── cd-staging.yml
│   └── cd-production.yml
├── services/                        ← each generated via start.spring.io
│   ├── api-gateway/
│   ├── auth-service/
│   ├── player-service/
│   ├── word-service/
│   ├── game-service/
│   ├── voting-service/
│   └── notification-service/
├── infrastructure/                  ← each generated via start.spring.io
│   ├── discovery-server/
│   ├── config-server/
│   └── config-repo/
└── docker/
    ├── docker-compose.yml
    ├── docker-compose.infra.yml
    └── docker-compose.test.yml
```

The root `pom.xml` manages dependency versions only. It does NOT contain shared business
logic modules. Services are not allowed to import each other's code — only communicate
over REST or Kafka.

---

## Per-service internal structure

Every service follows this package layout under `src/main/java/com/impostorgame/{service}/`:

```
{Service}Application.java
config/          ← Spring beans, Redis config, Feign config, etc.
controller/      ← REST controllers only, no business logic
service/         ← all business logic lives here
repository/      ← Spring Data JPA interfaces
domain/          ← JPA entities
dto/             ← request and response objects, no entities exposed directly
exception/       ← custom exceptions + one GlobalExceptionHandler per service
kafka/           ← producers and consumers (only in services that use Kafka)
client/          ← Feign clients to other services (only in game-service)
statemachine/    ← game FSM (only in game-service)
player/          ← PlayerContext sealed interface (game-service and voting-service only)
```

Tests live under `src/test/java/com/impostorgame/{service}/`:
```
unit/            ← pure Mockito, no Spring context, fast
integration/     ← @SpringBootTest + Testcontainers, real DB and Kafka
```

---

## Services

### discovery-server *(infrastructure — Slice 1)*
- Eureka server. Minimal — just `@EnableEurekaServer` + `application.yml`.
- Port: 8761
- Does not register with itself (`register-with-eureka: false`)

### config-server *(infrastructure — Slice 5)*
- Spring Cloud Config server backed by `infrastructure/config-repo/`
- Added last — services use local `application.yml` until Slice 5

### api-gateway *(Slice 5)*
- Single entry point for all client traffic
- Validates JWT on every request via `JwtAuthFilter`
- Blocks GUEST tokens on protected routes via `GuestRouteFilter`
- Routes: `/auth/**` `/players/**` `/games/**` `/words/**` `/votes/**` `/ws/**`
- Port: 8080

### auth-service *(Slice 1)*
- Issues JWT tokens for both registered users and guests
- Guests: `POST /auth/guest` → random display name, JWT with `role: GUEST`, 4h expiry, no DB record
- Registered: `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh`
- Conversion: `POST /auth/guest/convert` → promotes guest to user, preserves guestId
- DB: `auth_db` (users, refresh_tokens)
- Guests are never persisted

### player-service *(Slice 3)*
- Registered players only — all endpoints reject GUEST tokens
- `GET /players/{id}`, `GET /players/{id}/stats`, `PUT /players/{id}`
- Consumes `game.events` Kafka topic → updates stats on `GAME_ENDED`, skips if `isGuest: true`
- DB: `player_db` (players, stats, game_history)

### word-service *(Slice 2)*
- Stores word pairs and assigns them to players per room
- `GET /words/pair/random`, `POST /words/assign`, `GET /words/categories`, `POST /words` (admin)
- Word assignments cached in Redis with TTL = round duration
- DB: `word_db` (word_pairs, categories)

### game-service *(Slice 1 + extended in Slice 3)*
- Core service. Owns the game state machine and room lifecycle.
- `POST /games/rooms`, `POST /games/rooms/{code}/join`, `POST /games/rooms/{code}/start`
- `GET /games/rooms/{code}/state`, `POST /games/rooms/{code}/next-phase`
- Calls word-service via Feign (sync) to assign words when game starts
- Produces to `game.events`: `GAME_STARTED`, `PHASE_CHANGED`, `GAME_ENDED`
- Consumes `voting.results` to advance state after voting completes
- Room state cached in Redis
- DB: `game_db` (rooms, rounds, player_room)

### voting-service *(Slice 3)*
- `POST /votes/{roomId}` (cast vote), `GET /votes/{roomId}/results`
- Accepts votes only during VOTING phase
- When all players voted or timer expires → computes majority → produces to `voting.results`
- Active vote sessions stored in Redis with TTL = voting window
- DB: `voting_db` (votes, rounds)

### notification-service *(Slice 4)*
- No REST endpoints, no database
- Consumes all Kafka topics → pushes to clients via WebSocket (STOMP)
- WebSocket topics:
  - `/topic/room/{code}` → game state updates
  - `/topic/room/{code}/vote` → voting updates
  - `/user/{id}/queue/word` → private per-player word delivery
- Stateless — scales horizontally, uses Redis pub/sub as broker relay

---

## Guest vs registered players

Both receive a JWT. The difference:

| Claim      | Registered  | Guest           |
|------------|-------------|-----------------|
| `sub`      | user-uuid   | guest-uuid      |
| `role`     | `USER`      | `GUEST`         |
| `username` | "maria_dev" | "Wolf#4821"     |
| `exp`      | 24h+refresh | 4h, no refresh  |

| Feature               | Guest | Registered |
|-----------------------|-------|------------|
| Create / join rooms   | ✅    | ✅         |
| Play full game        | ✅    | ✅         |
| Stats & history       | ❌    | ✅         |
| Leaderboards          | ❌    | ✅         |
| Reconnect after refresh | ❌  | ✅         |
| Convert to account    | ✅    | —          |

The `PlayerContext` sealed interface is used in `game-service` and `voting-service` to handle
both player types without scattering if/else checks everywhere:

```java
public sealed interface PlayerContext permits RegisteredPlayer, GuestPlayer {
    String id();
    String displayName();
    boolean isGuest();
}
```

---

## Game state machine

The game room moves through these phases in order. No skipping, no going back (except RESULTS → LOBBY):

```
LOBBY → WORD_ASSIGNMENT → DISCUSSION → VOTING → RESULTS → LOBBY
```

| From             | Event           | To               |
|------------------|-----------------|------------------|
| LOBBY            | GAME_STARTED    | WORD_ASSIGNMENT  |
| WORD_ASSIGNMENT  | WORDS_DEALT     | DISCUSSION       |
| DISCUSSION       | TIMER_EXPIRED   | VOTING           |
| VOTING           | VOTE_COMPLETE   | RESULTS          |
| RESULTS          | RESET           | LOBBY            |

**Key rule:** Other services never touch the state machine directly. They report what happened
via Kafka. The `game-service` Kafka consumer receives the event and calls
`stateMachine.transition()`. Invalid transitions throw `InvalidTransitionException` and the
phase stays unchanged.

**Implementation:** Manual FSM using a switch expression (not Spring State Machine library).
Simple, easy to test, easy to understand.

```java
public void transition(Room room, GameEvent event) {
    GamePhase next = switch (room.getPhase()) {
        case LOBBY           -> event == GAME_STARTED  ? WORD_ASSIGNMENT : invalid(room, event);
        case WORD_ASSIGNMENT -> event == WORDS_DEALT   ? DISCUSSION      : invalid(room, event);
        case DISCUSSION      -> event == TIMER_EXPIRED ? VOTING          : invalid(room, event);
        case VOTING          -> event == VOTE_COMPLETE ? RESULTS         : invalid(room, event);
        case RESULTS         -> event == RESET         ? LOBBY           : invalid(room, event);
    };
    room.setPhase(next);
}
```

---

## Kafka topics

| Topic            | Producer         | Consumers                          | Events                                              |
|------------------|------------------|------------------------------------|-----------------------------------------------------|
| `game.events`    | game-service     | notification-service, player-service | `GAME_STARTED`, `PHASE_CHANGED`, `GAME_ENDED`     |
| `voting.results` | voting-service   | game-service, notification-service | `VOTE_CAST`, `VOTING_COMPLETE`, `PLAYER_ELIMINATED` |
| `player.actions` | notification-service | game-service                   | `PLAYER_JOINED`, `PLAYER_LEFT`                      |

All events share this envelope:

```json
{
  "eventId": "uuid",
  "eventType": "GAME_STARTED",
  "timestamp": "2024-01-15T10:30:00Z",
  "roomId": "ABC123",
  "isGuest": false,
  "payload": {}
}
```

---

## Data storage

Each service owns its own isolated database. No service may query another service's database.

| Service              | PostgreSQL DB  | Redis                  |
|----------------------|----------------|------------------------|
| auth-service         | auth_db        | —                      |
| player-service       | player_db      | —                      |
| word-service         | word_db        | assignments (TTL)      |
| game-service         | game_db        | room state cache       |
| voting-service       | voting_db      | active vote (TTL)      |
| notification-service | —              | pub/sub relay          |

---

## How services are created

1. Root `pom.xml` and folder structure are created **manually**
2. Each service is generated on **start.spring.io** with the dependencies below
3. After unzipping, update `<parent>` in each service `pom.xml` to point to root
4. Add `jjwt` manually to `auth-service` (not on start.spring.io)

| Service               | start.spring.io dependencies                                                         |
|-----------------------|--------------------------------------------------------------------------------------|
| discovery-server      | Eureka Server, Actuator                                                              |
| config-server         | Config Server, Actuator                                                              |
| api-gateway           | Gateway, Eureka Discovery Client, Actuator                                           |
| auth-service          | Web, Security, JPA, PostgreSQL Driver, Flyway, Validation, Eureka Client, Actuator  |
| player-service        | Web, JPA, PostgreSQL Driver, Flyway, Kafka, Validation, Eureka Client, Actuator      |
| word-service          | Web, JPA, PostgreSQL Driver, Flyway, Data Redis, Validation, Eureka Client, Actuator |
| game-service          | Web, JPA, PostgreSQL Driver, Flyway, Data Redis, OpenFeign, Kafka, Eureka Client, Actuator |
| voting-service        | Web, JPA, PostgreSQL Driver, Flyway, Data Redis, Kafka, Eureka Client, Actuator      |
| notification-service  | WebSocket, Kafka, Eureka Client, Actuator                                            |

---

## Iterative delivery — 5 slices

The project is built slice by slice. Each slice produces a working, runnable app.
Never build half a feature across multiple services and leave them disconnected.

### Slice 1 — "A room exists and players can join"
Services: `discovery-server`, `auth-service`, `game-service` (lobby only)
Done when: guest or registered player gets a token, creates/joins a room, both services appear in Eureka.

### Slice 2 — "Words get assigned"
Services: `word-service` (new), `game-service` (extended), Redis added
Done when: host starts game, word-service assigns words, one player gets impostor word, game moves to DISCUSSION.

### Slice 3 — "A full round is playable"
Services: `voting-service` (new), `player-service` (new), `game-service` (extended)
Done when: full loop works — lobby → words → discussion → voting → results. Stats update for registered players.

### Slice 4 — "It feels like a real game"
Services: `notification-service` (new), Kafka wired across all services
Done when: players see state changes live via WebSocket. No polling. Stats update via Kafka event.

### Slice 5 — "Production ready"
Services: `api-gateway` (new), `config-server` (new)
Done when: single entry point with JWT enforcement, centralized config, CI/CD pipeline running,
distributed tracing visible in Zipkin.

---

## Testing conventions

### Unit tests (`unit/` package)
- No Spring context — use `@ExtendWith(MockitoExtension.class)` only
- Mock all dependencies with `@Mock` / `@InjectMocks`
- Fast — run on every build
- Tag with `@Tag("unit")`

### Integration tests (`integration/` package)
- Use `@SpringBootTest` + Testcontainers (real PostgreSQL and/or Kafka containers)
- Use `@DynamicPropertySource` to wire container ports into Spring config
- Tag with `@Tag("integration")`
- Run separately from unit tests in CI

### Contract tests (Slice 5)
- Spring Cloud Contract on `word-service` assign endpoint
- `game-service` uses the generated stub in its tests instead of calling real service

### Guest-specific tests to always maintain
- Guest token has shorter expiry than user token
- Guest token accepted on game and room endpoints
- Guest token rejected on `GET /players/{id}/stats` and `PUT /players/{id}`
- Guest token rejected on `POST /auth/refresh`
- `POST /auth/guest/convert` preserves guestId in new user record

---

## CI/CD conventions

- CI runs on every PR and push to `main` / `develop`
- Uses `dorny/paths-filter` — only the service whose files changed gets built and tested
- Job order: unit tests → integration tests → Docker build → contract tests
- Docker images pushed to `ghcr.io` on merge to `develop`
- CD to staging on merge to `develop` branch
- CD to production on merge to `main` branch

---

## Hard rules — never violate these

- No service may import or query another service's database
- No shared business logic modules in the monorepo (versions only in root pom)
- `PlayerContext` sealed interface must be duplicated in `game-service` and `voting-service` —
  do not extract it to a shared module
- External services never call `stateMachine.transition()` directly —
  they publish Kafka events and game-service reacts
- Controllers contain no business logic — delegate everything to the service layer
- Entities are never returned from controllers — always map to DTOs
- Guests are never persisted in any database
- Write tests before or immediately after each feature — not at the end of a slice
