# AI Content Summarizer

[![CI](https://github.com/sharanzzgit/ai_summarizer/actions/workflows/ci.yml/badge.svg)](https://github.com/sharanzzgit/ai_summarizer/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Redis](https://img.shields.io/badge/Redis-7-red)
![Coverage](https://img.shields.io/badge/coverage-74%25-yellow)

A production-style REST API that summarizes text using Google Gemini, with a three-tier caching strategy (Redis → PostgreSQL → LLM) that reduces redundant LLM calls for duplicate content.

> Built as a portfolio project to demonstrate backend engineering practices: layered architecture, content-addressable caching, integration testing with Testcontainers, and CI/CD automation.

---

## Why this project exists

LLM calls are slow and expensive. A naive summarization API hits the model on every request — even when two users submit identical content. This service deduplicates by content hash and caches results, so the same input is summarized exactly once.

Concretely, calling `/api/summarize` with the same text three times in this implementation costs **one Gemini call** instead of three. Subsequent calls return in **~10ms instead of ~1-3 seconds**.

---

## Architecture

```
┌────────────┐    HTTP POST     ┌────────────────────────────────┐
│   Client   │ ───────────────▶ │   Spring Boot (port 8080)      │
└────────────┘                  │                                │
                                │  ┌──────────────────────────┐  │
                                │  │ SummaryController        │  │
                                │  │   ↓                      │  │
                                │  │ SummaryService           │  │
                                │  │   ├─ SHA-256 hash input  │  │
                                │  │   ├─ Check Redis     ────┼──┼──▶ Redis (in-memory, 24h TTL)
                                │  │   ├─ Check Postgres  ────┼──┼──▶ PostgreSQL (durable)
                                │  │   └─ Call Gemini     ────┼──┼──▶ Google Gemini API
                                │  │       persist + cache    │  │
                                │  └──────────────────────────┘  │
                                └────────────────────────────────┘
```

**Three-tier cache strategy.** On every request, the service computes a SHA-256 hash of the input text and walks the storage layers from cheapest to most expensive:

1. **Redis** (sub-ms). In-memory, hot summaries live here for 24 hours.
2. **PostgreSQL** (5-20ms). Durable, persists across restarts. Warms Redis on a hit.
3. **Gemini API** (1-3s). Only called on a full miss. Result is persisted to Postgres and cached in Redis.

---

## Tech stack

| Layer | Choice | Why |
|-------|--------|-----|
| Language | Java 21 | Records, sealed types, modern stdlib |
| Framework | Spring Boot 4.0.6 | Industry standard, autoconfiguration |
| HTTP client | Spring WebClient | Reactive, non-blocking calls to Gemini |
| Database | PostgreSQL 16 | Durable persistence, indexed content lookups |
| Migrations | Flyway | Versioned, reproducible schema changes |
| ORM | Spring Data JPA (Hibernate) | Repository pattern, transactional boundaries |
| Cache | Redis 7 | Sub-millisecond lookups, TTL-based eviction |
| Security | Spring Security | Filter chain in place (currently permissive — auth planned) |
| Build | Maven (wrapper) | No global Maven install required |
| Container | Docker Compose | One-command local environment |
| Tests | JUnit 5, Mockito, AssertJ, Testcontainers | Unit + real integration tests |
| Coverage | JaCoCo | HTML reports, CI-integrated |
| CI | GitHub Actions | Tests run on every push and PR |

---

## Key features

- **Content-addressable deduplication.** SHA-256 hash of the input is the cache key. Identical input always maps to the same cached summary.
- **Three-tier cache.** Redis → Postgres → Gemini. Each tier slower than the one before, so we try the cheapest first.
- **Transactional persistence.** Service-level `@Transactional` ensures DB writes and cache writes don't leave the system in a partial state.
- **Schema migrations via Flyway.** Versioned migrations applied on startup; no manual DDL.
- **Polymorphic JSON serialization in Redis.** `Instant` timestamps and typed objects round-trip cleanly using Jackson with default typing.
- **Real integration tests.** Testcontainers spins up actual Postgres and Redis containers per test run — no in-memory substitutes, no mocked DB layer.
- **CI/CD on every push.** GitHub Actions runs the full suite, attaches the JaCoCo coverage report to each run.

---

## Performance characteristics

Observed timings on local hardware for repeated requests with identical content:

| Call | Source | Time |
|------|--------|------|
| 1 | Gemini API (cold) | ~1-3s |
| 2 | Postgres + Redis warmup | ~30-100ms |
| 3 | Redis hit | **~10ms** |

That's a **~120× speedup** on cached lookups and a 100% LLM-call reduction for duplicate content within the TTL window. In a workload where some fraction of requests are repeated (realistic for a summarizer), this translates directly to lower latency for users and lower API costs.

---

## Getting started

### Prerequisites

- JDK 21
- Docker Desktop (running)
- A Google Gemini API key — get one at [aistudio.google.com](https://aistudio.google.com/app/apikey)

### Setup

```bash
# Clone
git clone https://github.com/sharanzzgit/ai_summarizer.git
cd ai_summarizer

# Set the Gemini API key (Windows PowerShell)
[System.Environment]::SetEnvironmentVariable("GEMINI_API_KEY", "your-key-here", "User")

# (Linux / macOS)
export GEMINI_API_KEY="your-key-here"

# Start Postgres + Redis
docker compose up -d

# Run the app
./mvnw spring-boot:run   # Linux/macOS
.\mvnw.cmd spring-boot:run  # Windows
```

The app boots on `http://localhost:8080`. Flyway runs migrations automatically on first start.

### Try it

```bash
curl -X POST http://localhost:8080/api/summarize \
  -H "Content-Type: application/json" \
  -d '{"text":"Spring Boot is a Java framework that simplifies building production-grade applications by providing auto-configuration, embedded servers, and starter dependencies."}'
```

Response:
```json
{
  "id": 1,
  "summary": "Spring Boot is a Java framework that streamlines production-grade application development through auto-configuration, embedded servers, and starter dependencies.",
  "model": "gemini-2.5-flash",
  "cached": false,
  "createdAt": "2026-06-15T12:00:00Z"
}
```

Run the same request again — `cached` becomes `true`, response time drops by ~100×.

---

## API reference

### `POST /api/summarize`

Summarize a piece of text. Identical inputs return the cached summary.

**Request body**
```json
{
  "text": "string, 50–50000 characters"
}
```

**Response 200**
```json
{
  "id": 1,
  "summary": "string",
  "model": "gemini-2.5-flash",
  "cached": false,
  "createdAt": "ISO-8601 timestamp"
}
```

**Validation errors** return `400 Bad Request` with details.

---

## Project structure

```
ai_summarizer/
├── src/
│   ├── main/
│   │   ├── java/com/sharan/ai_summary/
│   │   │   ├── AiSummaryApplication.java     # entry point
│   │   │   ├── config/                       # security, cache, properties, beans
│   │   │   ├── controller/                   # REST endpoints
│   │   │   ├── service/                      # SummaryService, GeminiService
│   │   │   ├── repository/                   # JPA repositories
│   │   │   ├── entity/                       # JPA entities
│   │   │   └── dto/                          # request/response shapes
│   │   └── resources/
│   │       ├── application.yml               # config (datasource, redis, gemini)
│   │       └── db/migration/                 # Flyway SQL migrations
│   └── test/
│       └── java/com/sharan/ai_summary/
│           ├── AiSummaryApplicationTests.java   # integration tests (Testcontainers)
│           └── service/SummaryServiceTest.java  # unit tests for orchestration
├── docker-compose.yml                        # local Postgres + Redis
├── pom.xml                                   # Maven config
└── .github/workflows/ci.yml                  # CI pipeline
```

---

## Testing

```bash
./mvnw test
```

The suite has two layers:

**Unit tests (`SummaryServiceTest`).** Mock the repository, cache, and Gemini service. Verify that the orchestration logic (cache → DB → Gemini, with cache warming on DB hit) calls the right dependencies the right number of times. Fast — runs in milliseconds.

**Integration tests (`AiSummaryApplicationTests`).** Boot the full Spring context against real Postgres and Redis containers managed by Testcontainers. Mock only Gemini (we don't want to burn API calls in CI). Hit the real `/api/summarize` endpoint with HTTP requests via MockMvc and assert on JSON responses. Catches wiring issues that unit tests miss.

**Coverage:** 74% of instructions, generated by JaCoCo. Report at `target/site/jacoco/index.html` after running the tests. Honest about what's covered: the orchestration and HTTP layers are well-tested; lightweight DTOs and entities aren't padded with throwaway tests for the sake of a higher number.

---

## Design decisions

A few choices worth explaining, since they came up during the build:

**Why hash the content rather than using the text directly as the cache key?**
Content can be up to 50,000 characters. A 64-character SHA-256 hash is constant size, indexable in Postgres (`VARCHAR(64)`), and trivially comparable in Redis. Collisions are astronomically unlikely for SHA-256, so they're not a practical concern.

**Why three storage tiers instead of just Redis?**
Redis is fast but volatile — restart and the cache is empty. If summaries only lived in Redis, a restart would mean re-paying for every Gemini call from scratch. Postgres provides the durability layer: even if Redis goes cold, the system still avoids redundant LLM calls.

**Why `CacheManager` directly instead of `@Cacheable` annotations?**
Spring's caching annotations work through proxies, which means internal method calls within the same bean bypass them silently. Calling `cacheManager.getCache(...).put(...)` explicitly avoids the gotcha. More code, fewer surprises.

**Why polymorphic JSON serialization for Redis values?**
Jackson by default doesn't include type information in the JSON it writes. When reading back, it returns `LinkedHashMap` instead of your actual type. Enabling default typing (`activateDefaultTyping`) makes Jackson embed `@class` so deserialization restores the original type. Without this, every cache hit would `ClassCastException`.

**Why Testcontainers instead of an in-memory DB?**
H2 doesn't behave identically to Postgres — SQL dialect differences, transaction isolation differences, lack of `TIMESTAMP WITH TIME ZONE` support, etc. Testing against a fake DB makes tests pass when production would fail. Testcontainers spins up the actual Postgres image, so what the tests exercise is what production runs.

---

## What's not built (yet)

Honest list of scope that's been left out:

- **JWT-based authentication.** Spring Security is wired in and ready for it. The endpoint is currently open. Adding `users` table, register/login endpoints, and a JWT filter is the natural next step.
- **Per-user rate limiting.** Bucket4j with Redis storage is the planned approach.
- **Streaming responses.** Gemini supports streaming; the current implementation buffers the full response. SSE (`text/event-stream`) is the planned transport.
- **Distributed tracing.** Spring Boot's Actuator + Micrometer + OpenTelemetry would be straightforward to plug in.
- **Frontend.** A Vite + React UI is planned to demonstrate end-to-end usage in a browser.
- **AWS deployment.** Planned to ship to ECS Fargate with RDS Postgres and ElastiCache Redis.

These were left out intentionally to keep the project's scope focused on the backend caching pattern. Each is a reasonable extension; none would fundamentally change the architecture.

---

## License

MIT
