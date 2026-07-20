# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Development
Backend (port 8080):
```
mvn spring-boot:run
```
Frontend dev server (port 5173, proxies API to backend):
```
cd frontend && npm install && npm run dev
```
Open: http://localhost:5173/waveai/

### Production build (single JAR)
```
mvn clean package -DskipTests
java -jar target/waveai-worker-0.0.1-SNAPSHOT.jar
```

### Environment variable (required)
```
# Windows
set WAVEAI_API_TOKEN=your-token-here
# macOS/Linux
export WAVEAI_API_TOKEN=your-token-here
```

### Database profiles
- `local` (default, `application-local.yml`) — Docker Postgres on `localhost:5432` (`docker compose up -d`)
- `neon` (`application-neon.yml`) — Neon Postgres; set `SPRING_PROFILES_ACTIVE=neon` plus `WAVEAI_WORKER_NEON_DATABASE_URL` (JDBC URL, direct non-pooler endpoint), `WAVEAI_WORKER_NEON_DATABASE_USERNAME`, `WAVEAI_WORKER_NEON_DATABASE_PASSWORD`. Vars are app-prefixed so multiple local apps can each point at their own Neon project without collision. SSL is applied by the profile.

## Architecture

**Stack:** Vue 3 + Vite (frontend) | Java 21 + Spring Boot 4 (backend) | PostgreSQL cache | Maven build

The app is a caching viewer for the WaveAI note-taking API. Reads always serve from the Postgres cache; the backend fetches from `https://api.wave.co/v1` (Spring `RestClient`, Bearer token) only when the cache is stale or a user requests a sync, then serves the bundled Vue SPA as static assets.

**Sync/rate-limit design** (WaveAI rate-limits aggressively — preserve these invariants when touching sync code):
- `CacheSyncService.trigger*` methods are TTL-gated on `cachedAt` (TTLs under `sync.ttl` in `application.yml`), deduplicated via an in-flight key set, and run on the bounded `syncExecutor` bean. Read endpoints in `SessionController` call these triggers; a fresh cache means zero upstream calls.
- `sync*Now` methods back the manual Sync buttons (`SyncController`, `POST /api/sync/...`): they bypass the TTL but still respect dedupe (returns 202 in-progress) and the rate limiter, and propagate errors instead of swallowing them.
- A 429 from WaveAI becomes `RateLimitedException` (in `WaveAiService`) and pauses ALL syncing via the shared `UpstreamRateLimiter` (honors `Retry-After`, falls back to `sync.rate-limit-cooldown`). `SyncController` maps it to HTTP 429 + `Retry-After`.
- The hourly `@Scheduled` sync routes through the gated triggers, so it's a no-op while fresh.
- Postgres jsonb columns written through an `AttributeConverter` need `@ColumnTransformer(write = "?::jsonb")` (see `SessionDetailEntity.speakers`) — without it inserts fail at runtime only, not in tests.

**Context path:** Everything runs under `/waveai` — the Spring Boot servlet context path and Vite's `base` are both set to `/waveai/`.

**SPA fallback:** Vue Router uses HTML5 history mode, so `SpaForwardController` forwards the client routes (`/session/{id}`, `/folders`) to `forward:/index.html` for direct loads/refreshes. Routes are listed explicitly — when adding a route in `frontend/src/router`, add it to `SpaForwardController` too. Unknown paths still 404; `/api/**` is untouched.

**Dev proxy:** Vite proxies `/waveai/api` → `http://localhost:8080` so the frontend hits the Spring Boot backend during development without CORS issues (CORS is also configured in `RestClientConfig` for `localhost:5173`).

**Build integration:** `frontend-maven-plugin` runs `npm run build` during Maven's `prepare-package` phase. Vite outputs to `../target/classes/static`, which Spring Boot serves as classpath static resources. The result is a single executable JAR.

### Backend layout
```
src/main/java/com/waveai/worker/
├── config/
│   ├── RestClientConfig.java         # RestClient bean (auth headers) + CORS
│   ├── SyncProperties.java           # Binds sync.* (schedule, rate-limit-cooldown, ttl.*)
│   └── SyncExecutorConfig.java       # Bounded ThreadPoolTaskExecutor for background syncs
├── controller/
│   ├── SessionController.java        # Cached GETs: /api/sessions, /api/sessions/{id}, /api/folders, /api/events (SSE)
│   ├── SyncController.java           # POST /api/sync/{sessions,sessions/{id},folders}, GET /api/sync/status
│   └── SpaForwardController.java     # SPA deep-link forwards → index.html (keep in sync with frontend/src/router)
├── dto/ + mapper/SessionMapper.java  # Entity → DTO for the frontend
├── entity/ + repository/             # JPA entities (cachedAt timestamps) + Spring Data repos
├── model/                            # Java records: Session, SessionDetail, Folder + response wrappers
├── service/
│   ├── WaveAiService.java            # WaveAI REST client: 429 → RateLimitedException, others → 502
│   ├── CacheSyncService.java         # TTL gate, dedupe, manual syncNow, hourly schedule, SSE broadcasts
│   └── UpstreamRateLimiter.java      # Global 429 cooldown shared by all sync paths
└── sse/SseEmitterRegistry.java       # SSE connection registry
```

### Frontend layout
```
frontend/src/
├── api/          # Axios clients (sessions.js, folders.js, sync.js) — baseURL /waveai/api
├── components/   # SessionList (toolbar w/ Sync button), SessionCard
├── views/        # SessionDetail (Sync button + last-synced), FolderList
└── router/       # Vue Router — /, /session/:id, /folders
```

### Adding a new WaveAI endpoint
1. Add a Java `record` in `model/` with `@JsonIgnoreProperties(ignoreUnknown = true)`
2. Add a method to `WaveAiService` using `restClient.get().uri(...).retrieve().body(...)` — keep the `HttpClientErrorException.TooManyRequests` → `RateLimitedException` catch before the generic one
3. If the data should be cached: add an entity (with a `cachedAt` field) + Flyway migration + repository, and a TTL-gated sync path in `CacheSyncService` following the existing trigger/syncNow pattern
4. Add a `@GetMapping` in `SessionController` (serve from cache, fire the gated trigger)
5. Add a fetch function in the appropriate `frontend/src/api/*.js` file

### Testing
- `mvn test` — unit tests plus a Testcontainers context test (needs Docker) that validates Flyway migrations and entity mappings against real Postgres
- `cd frontend && npx vitest run` — frontend tests
