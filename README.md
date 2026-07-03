# Impostor Game

Juego multijugador de deducción social por palabras. Todos los jugadores reciben una
palabra secreta; un jugador (el impostor) recibe una palabra parecida pero distinta.
Se discute y se vota para descubrir al impostor. No hace falta registrarse: juegan
invitados y usuarios registrados.

> Este proyecto es, sobre todo, una **plataforma de aprendizaje** de arquitectura de
> microservicios: hexagonal (Ports & Adapters), DDD, TDD, mensajería asíncrona con
> Kafka, autenticación JWT distribuida (RS256 + JWKS) y CI/CD.

---

## Stack

| Área | Tecnología |
|---|---|
| Lenguaje / Framework | Java 25 · Spring Boot 4.0.7 |
| Build | Maven monorepo (parent pom) |
| Spring Cloud | 2025.1.2 |
| Discovery | Eureka |
| Sync / Async | REST (OpenFeign) · Apache Kafka |
| Datos | PostgreSQL (una por servicio) · Redis · Flyway |
| Auth | JWT RS256 + JWKS |
| Tiempo real | WebSocket STOMP |
| Testing | JUnit 5 · Mockito · Testcontainers · AssertJ |
| CI/CD | GitHub Actions |
| Despliegue | Homelab vía Docker Compose |

---

## Arquitectura

Todos los servicios de negocio siguen **arquitectura hexagonal + DDD**:

```
domain/          ← Java puro, sin dependencias de framework
  model/         ← agregados, entidades, value objects
  event/         ← eventos de dominio
  exception/     ← excepciones de dominio
  port/
    in/          ← casos de uso (driving ports)
    out/         ← repositorios y servicios externos (driven ports)

application/     ← implementa port/in, orquesta el dominio
  service/       ← una clase por caso de uso

infrastructure/  ← todo el código Spring/JPA/Kafka/Redis
  adapter/
    in/web/          ← controllers REST
    out/persistence/ ← store primario (JPA o Redis cuando es fuente de verdad)
    out/messaging/   ← productores/consumidores Kafka
    out/cache/       ← Redis solo como caché derivada
  config/            ← beans de Spring
```

**Regla de dependencias:** `domain ← application ← infrastructure`. Nunca al revés.
`domain/` tiene cero imports de Spring/JPA/Kafka y cero Lombok.

---

## Servicios

| Servicio | Slice | Puerto | Estado |
|---|---|---|---|
| discovery-server | 1 | 8761 | ✅ |
| auth-service | 1 | 8081 | ✅ funcional |
| game-service | 1+3 | 8082 | 🚧 en curso |
| word-service | 2 | 8083 | ⏳ |
| voting-service | 3 | 8084 | ⏳ |
| player-service | 3 | 8085 | ⏳ |
| notification-service | 4 | 8086 | ⏳ |
| api-gateway | 5 | 8080 | ⏳ |
| config-server | 5 | 8888 | ⏳ |

Módulos activos en el parent pom: `discovery-server`, `auth-service`, `game-service`.

---

## Invitado vs registrado

| | Invitado | Registrado |
|---|---|---|
| Rol JWT | `GUEST` | `USER` |
| Expiración | 4h, sin refresh | 24h + refresh token |
| Persistido | Nunca | Sí |
| Jugar | ✅ | ✅ |
| Stats / ranking | ❌ | ✅ |
| Reconectar | ❌ | ✅ |

---

## Entrega iterativa — 5 slices

| Slice | Terminado cuando |
|---|---|
| 1 — "Existe una sala" | El jugador obtiene token, crea/entra a una sala y aparece en Eureka |
| 2 — "Palabras asignadas" | El host inicia la partida y un jugador recibe la palabra de impostor |
| 3 — "Ronda jugable" | Lobby → palabras → discusión → votación → resultados |
| 4 — "Se siente como juego" | Actualizaciones live por WebSocket, sin polling |
| 5 — "Listo para producción" | Gateway, config server, CI/CD, tracing |

**Slice 1 en curso.** `auth-service` completo; `game-service` con dominio y aplicación
listos, persistencia Redis lista, `RoomController` en progreso.

---

## Puesta en marcha

Requisitos: Java 25, Maven 3.9+, Docker + Docker Compose.

### 1. Secretos

Crea un `.env` en la raíz (nunca se commitea) con las credenciales de BD y las claves.
La keypair RSA de `auth-service` (PKCS#8) está gitignoreada.

### 2. Infraestructura (Postgres, Redis, Kafka)

```bash
docker compose up -d
```

### 3. Levantar un servicio en local

```bash
set -a; source .env; set +a
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

En Docker/homelab: `SPRING_PROFILES_ACTIVE=docker`.

### Perfiles

| Fichero | Propósito | Commiteado |
|---|---|---|
| `application.yml` | Config común no sensible | ✅ |
| `application-local.yml` | URLs de dev local | ✅ |
| `application-docker.yml` | URLs de Docker/homelab | ✅ |
| `.env` | Secretos y contraseñas | ❌ |

---

## Testing

```bash
mvn test                      # todos
mvn test -Dgroups=unit        # solo unitarios
mvn test -Dgroups=integration # solo integración (Testcontainers)
```

- **Unit:** `@ExtendWith(MockitoExtension.class)`, sin contexto Spring, `@Tag("unit")`.
- **Integration:** `@SpringBootTest` + Testcontainers, `@Tag("integration")`.
- Layout de tests espeja la estructura de paquetes; AssertJ para asserts.
- TDD a partir de `word-service`.

---

## Reglas duras

- Ningún servicio accede a la BD de otro.
- Sin lógica de negocio compartida en el monorepo (solo versiones en el pom raíz).
- `domain/` sin imports de Spring/JPA/Kafka y sin Lombok.
- Los controllers llaman a `port/in`, nunca a los application services directamente.
- Las entidades de dominio no se devuelven desde controllers: siempre a DTO.
- `PlayerContext` se duplica por servicio, nunca se extrae a un módulo compartido.
- La máquina de estados solo se invoca dentro de `game-service`.
- Los invitados nunca se persisten.
- Los consumidores Kafka deben ser idempotentes.
- Clases inyectables: constructor explícito, `private final`, sin `@RequiredArgsConstructor`.
  Lombok solo `@Getter`/`@Builder` en DTOs y entidades Redis.

---

## Estructura del monorepo

```
impostor-game/
├── pom.xml                  ← parent pom
├── docker-compose.yml
├── docker/init-dbs.sql
├── CLAUDE.md                ← fuente de verdad
├── infrastructure/
│   └── discovery-server/
└── services/
    ├── auth-service/
    └── game-service/
```