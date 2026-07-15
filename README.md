# Impostor Game

A multiplayer social-deduction word game built as a **microservices learning project**.

Every player receives a secret word — except one, the **impostor**, who gets a similar but
different word. Players discuss, then vote to unmask the impostor. No registration required:
both guests and registered users can play.

> This repository is primarily a hands-on study of microservices architecture: hexagonal
> (Ports & Adapters) design, DDD, TDD, async messaging with Kafka, distributed authentication
> with JWT (RS256 + JWKS), and CI/CD with GitHub Actions.

---

## Tech stack

| Area | Choice |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.0.x |
| Build | Maven monorepo (parent pom) |
| Service discovery | Eureka |
| Config server | Spring Cloud Config *(Slice 5)* |
| Sync communication | REST via OpenFeign |
| Async communication | Apache Kafka |
| Cache / ephemeral state | Redis |
| Database | PostgreSQL (one per service) |
| Migrations | Flyway |
| Real-time | WebSocket (STOMP) |
| Testing | JUnit 5, Mockito, Testcontainers |
| CI/CD | GitHub Actions |
| Observability | Micrometer + Zipkin, Prometheus + Grafana *(Slice 5)* |

---

## Architecture

Each business service follows **Hexagonal Architecture + DDD**, with a strict dependency rule:

```
domain  ←  application  ←  infrastructure
```

The `domain` layer is pure Java — zero Spring, JPA, or Kafka imports — and never depends on the
layers around it.

```
{service}/src/main/java/com/impostorgame/{service}/
├── domain/            pure Java: aggregates, value objects, domain events, ports (in/out)
├── application/       use-case implementations orchestrating the domain
└── infrastructure/    Spring/JPA/Kafka/Redis: web adapters, persistence, messaging, config
```

For architectural conventions, hard rules, and the reasoning behind each decision, see
[`CLAUDE.md`](./CLAUDE.md) — the source of truth for this project.

---

## Services

| Service | Slice | Port | Role |
|---|---|---|---|
| discovery-server | 1 | 8761 | Eureka registry (infrastructure only) |
| auth-service | 1 | 8081 | Issues JWTs, holds the RSA private key |
| game-service | 1 & 3 | 8082 | Rooms, players, game state machine |
| word-service | 2 | 8083 | Word assignment (incl. impostor word) |
| voting-service | 3 | 8084 | Vote tally |
| player-service | 3 | 8085 | Player stats / leaderboard |
| notification-service | 4 | 8086 | Translates domain events into client messages |
| api-gateway | 5 | 8080 | Single entry point (infrastructure only) |
| config-server | 5 | 8888 | Centralized config (infrastructure only) |

---

## Authentication

Authentication uses **JWT signed with RS256** and validated via **JWKS**:

- `auth-service` is the *only* service holding the RSA private key. It signs tokens.
- Every other service is an **OAuth2 resource server**: it validates tokens by fetching the
  public key from `auth-service`'s JWKS endpoint (`/.well-known/jwks.json`, `kid: auth-key-1`).
- RS256 (over HMAC) confines token-forging capability to `auth-service` alone.

### Guest vs registered players

| | Guest | Registered |
|---|---|---|
| JWT role | `GUEST` | `USER` |
| Token expiry | 4h, no refresh | 24h + refresh token |
| Persisted in DB | Never | Yes |
| Play a game | ✅ | ✅ |
| Stats / leaderboard | ❌ | ✅ |
| Reconnect | ❌ | ✅ |

---

## Game flow

A round moves through a manual finite-state machine (lives in `game-service`):

```
LOBBY → WORD_ASSIGNMENT → DISCUSSION → VOTING → RESULTS → LOBBY
```

Services never call the state machine directly. They publish Kafka events and `game-service`
reacts — each service publishing only events about what it owns.

---

## Getting started

### Prerequisites

- JDK 25 (managed with `jenv` recommended)
- Maven 3.9.x
- Docker Desktop (for Postgres, Redis, Kafka via Docker Compose)

### Configuration

Secrets live in a git-ignored `.env` file at the repo root (never committed). Profiles:

| Profile | Purpose |
|---|---|
| `local` | Local dev — `localhost` URLs |
| `docker` | Homelab / Docker — service-name URLs |

### Run a service locally

Load the env file first, then activate the profile:

```bash
set -a; source ../../.env; set +a
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Start `discovery-server` first, then the individual services.

### Run in Docker

```bash
SPRING_PROFILES_ACTIVE=docker docker compose up
```

---

## Delivery roadmap

The project is delivered in five vertical slices, each shippable end-to-end.

| Slice | Goal | Status |
|---|---|---|
| 1 — A room exists | Get a token, create/join a room, appear in Eureka | ✅ Done |
| 2 — Words assigned | Host starts game, words dealt, one player gets impostor word | 🔨 In progress |
| 3 — Full round playable | Lobby → words → discussion → voting → results | ⏳ |
| 4 — Feels like a game | Live WebSocket updates, no polling | ⏳ |
| 5 — Production ready | Gateway, config server, CI/CD, tracing | ⏳ |

---

## Testing

- **Unit tests** — no Spring context, `@Tag("unit")`, Mockito.
- **Integration tests** — `@SpringBootTest` + Testcontainers, `@Tag("integration")`.
- Tests mirror the production package structure.
- TDD from `word-service` onwards.

---

## License
 
MIT — see [`LICENSE`](./LICENSE).
