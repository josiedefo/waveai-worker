# WaveAI Worker

A web app that fetches and displays session data from the WaveAI Note Taking tool. Browse your sessions, view full summaries, transcripts, and speaker details, and organize by folder.

**Tech stack:** Vue 3 + Vite (frontend) | Java 21 + Spring Boot 3.5 (backend) | PostgreSQL (cache)

## Prerequisites

- Java 21+
- Maven 3.9+
- Node.js 22+ (dev only)
- Docker (for local PostgreSQL)
- AWS CLI v2 (for deployment)

## Setup

**1. Start the database:**

```bash
docker compose up -d
```

This starts a PostgreSQL 16 container on `localhost:5432` with database/user/password all set to `waveai`. No extra config needed for local dev.

**2. Set your WaveAI API token:**

```bash
# Windows
set WAVEAI_API_TOKEN=your-token-here

# macOS/Linux
export WAVEAI_API_TOKEN=your-token-here
```

## Development

**Backend** (Terminal 1):

```bash
mvn spring-boot:run
```

Flyway runs the schema migration automatically on startup.

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

For production, set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` environment variables to point at your PostgreSQL instance.

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
- A PostgreSQL instance accessible from EB (e.g. Amazon RDS)

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

Set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` as environment variables in the EB environment configuration — they are never stored in source code or the deploy ZIP.

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
│   ├── application.yml               # Port, context path, API URL, DB, Flyway config
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
