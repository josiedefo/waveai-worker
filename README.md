# WaveAI Worker

A web app that fetches and displays session data from the WaveAI Note Taking tool. Browse your sessions, view full summaries, transcripts, and speaker details, and organize by folder.

**Tech stack:** Vue 3 + Vite (frontend) | Java 21 + Spring Boot 3.5 (backend) | PostgreSQL (cache)

## Prerequisites

- Java 21+
- Maven 3.9+
- Node.js 22+ (dev only)
- Docker (for local PostgreSQL — skip if you're using Neon instead, see below)
- AWS CLI v2 (for deployment)

## Setup

**1. Set your WaveAI API token:**

```bash
# Windows
set WAVEAI_API_TOKEN=your-token-here

# macOS/Linux
export WAVEAI_API_TOKEN=your-token-here
```

**2. Choose a database.** The backend picks its datasource via a Spring profile — `local` (Docker Postgres) is the default, `neon` (Neon Postgres) is opt-in.

**Option A — Local Docker Postgres (default):**

```bash
docker compose up -d
```

Starts a PostgreSQL 16 container on `localhost:5432` with database/user/password all set to `waveai`. `mvn spring-boot:run` connects to this automatically — no further config needed (`local` profile, `application-local.yml`).

**Option B — Neon Postgres:** skip Docker; instead set these before starting the backend:

```bash
# Windows
set SPRING_PROFILES_ACTIVE=neon
set WAVEAI_WORKER_NEON_DATABASE_URL=jdbc:postgresql://<your-endpoint-host>/<database>
set WAVEAI_WORKER_NEON_DATABASE_USERNAME=<role-name>
set WAVEAI_WORKER_NEON_DATABASE_PASSWORD=<role-password>

# macOS/Linux
export SPRING_PROFILES_ACTIVE=neon
export WAVEAI_WORKER_NEON_DATABASE_URL=jdbc:postgresql://<your-endpoint-host>/<database>
export WAVEAI_WORKER_NEON_DATABASE_USERNAME=<role-name>
export WAVEAI_WORKER_NEON_DATABASE_PASSWORD=<role-password>
```

Get these values from the Neon console (Connect → select your database, pooling **off**). Use the **direct (non-pooler) endpoint** — Flyway needs session-level features that PgBouncer transaction pooling doesn't support. Don't add `sslmode` to the URL yourself; the `neon` profile (`application-neon.yml`) applies it automatically. The env vars are prefixed `WAVEAI_WORKER_` so they won't collide with another app's Neon credentials in the same shell/machine.

## Development

**Backend** (Terminal 1):

```bash
mvn spring-boot:run
```

Connects to whichever database you set up in step 2 above — Docker by default, or Neon if `SPRING_PROFILES_ACTIVE=neon` and the `WAVEAI_WORKER_NEON_DATABASE_*` vars are set in the same shell. Flyway runs the schema migration automatically on startup either way.

**Frontend** (Terminal 2):

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173/waveai/

The Vite dev server proxies `/waveai/api` requests to Spring Boot on port 8080.

## Production Build

Single command builds everything (frontend + backend) into one executable JAR:

```bash
mvn clean package -DskipTests
java -jar target/waveai-worker-0.0.1-SNAPSHOT.jar
```

Open http://localhost:8080/waveai/

Same profile system as local development (see [Setup](#setup)): defaults to the `local` Docker profile unless `SPRING_PROFILES_ACTIVE=neon` plus the `WAVEAI_WORKER_NEON_DATABASE_*` variables are set.

## How it works

On page load the app immediately serves cached data from PostgreSQL, then silently fetches fresh data from the WaveAI API in the background. When the sync completes, the UI updates automatically via Server-Sent Events — no page refresh needed.

The background sync also runs on a schedule every 15 minutes.

## Features

- **Sessions** — browse all your WaveAI sessions in a card grid
- **Session detail** — click any session to see date, time, duration, speakers, full summary, and a link to open it in Wave
- **Transcript** — speaker-attributed transcript displayed on the session detail page
- **Folders** — view all your Wave folders with session counts and color labels

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/sessions` | List all sessions (from cache) |
| GET | `/api/sessions/{id}` | Full session detail (summary, speakers, language, notes) |
| GET | `/api/sessions/{id}/transcript` | Speaker-attributed transcript segments |
| GET | `/api/folders` | List all folders |
| GET | `/api/events` | SSE stream for cache-update notifications |

## Deploy to AWS Elastic Beanstalk

### Prerequisites

- AWS CLI configured (`aws configure`)
- IAM user with permissions for: Elastic Beanstalk, S3, IAM instance profiles
- A PostgreSQL instance accessible from EB (e.g. Neon or Amazon RDS)

### Deploy

**Windows (Command Prompt):**
```bat
set WAVEAI_API_TOKEN=your-token-here
scripts\deploy.bat
```

**Windows (Git Bash) / macOS / Linux:**
```bash
export WAVEAI_API_TOKEN=your-token-here
bash scripts/deploy.sh
```

Optionally pass a region (default: `us-east-1`):
```bat
scripts\deploy.bat eu-west-1
```

Set `SPRING_PROFILES_ACTIVE=neon` plus `WAVEAI_WORKER_NEON_DATABASE_URL`, `WAVEAI_WORKER_NEON_DATABASE_USERNAME`, and `WAVEAI_WORKER_NEON_DATABASE_PASSWORD` as environment variables in the EB environment configuration — they are never stored in source code or the deploy ZIP.

### Subsequent deploys

Same command — the script detects the environment already exists and just updates it.

## Project Structure

```
├── docker-compose.yml                # Local PostgreSQL container
├── Procfile                          # Tells Elastic Beanstalk how to run the app
├── .ebextensions/app.config          # EB health check path config
├── scripts/deploy.sh                 # AWS deploy script (uses AWS CLI)
├── src/assembly/eb.xml               # Maven assembly: packages JAR + Procfile into deploy ZIP
├── pom.xml                           # Maven build
├── src/main/resources/
│   ├── application.yml               # Port, context path, API URL, Flyway, pool config
│   ├── application-local.yml         # local profile (default): Docker Postgres
│   ├── application-neon.yml          # neon profile: Neon Postgres via WAVEAI_WORKER_NEON_DATABASE_* env vars
│   └── db/migration/                 # Flyway SQL migrations
└── src/main/java/com/waveai/worker/
    ├── config/RestClientConfig.java  # RestClient + CORS
    ├── controller/SessionController.java
    ├── dto/                          # Response DTOs served to the frontend
    ├── entity/                       # JPA entities + JSONB converter
    ├── mapper/SessionMapper.java     # Entity → DTO mapping
    ├── model/                        # WaveAI API response records
    ├── repository/                   # Spring Data JPA repositories
    ├── service/
    │   ├── WaveAiService.java        # WaveAI API client
    │   └── CacheSyncService.java     # Background sync + scheduled refresh
    └── sse/SseEmitterRegistry.java   # SSE connection registry
```

```
frontend/src/
├── api/
│   ├── sessions.js   # fetchSessions, fetchSession, fetchTranscript
│   ├── folders.js    # fetchFolders
│   └── sse.js        # Singleton SSE client
├── components/       # SessionList, SessionCard
├── views/            # SessionDetail, FolderList
└── router/           # Vue Router
```
