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

## Architecture

**Stack:** Vue 3 + Vite (frontend) | Java 21 + Spring Boot 3.5 (backend) | Maven build

The app is a proxy/viewer for the WaveAI note-taking API. The backend forwards requests to `https://api.wave.co/v1` using Spring's `RestClient` with a Bearer token, then serves the bundled Vue SPA as static assets.

**Context path:** Everything runs under `/waveai` — the Spring Boot servlet context path and Vite's `base` are both set to `/waveai/`.

**Dev proxy:** Vite proxies `/waveai/api` → `http://localhost:8080` so the frontend hits the Spring Boot backend during development without CORS issues (CORS is also configured in `RestClientConfig` for `localhost:5173`).

**Build integration:** `frontend-maven-plugin` runs `npm run build` during Maven's `prepare-package` phase. Vite outputs to `../target/classes/static`, which Spring Boot serves as classpath static resources. The result is a single executable JAR.

### Backend layout
```
src/main/java/com/waveai/worker/
├── config/RestClientConfig.java      # RestClient bean (auth headers) + CORS
├── controller/SessionController.java # GET /api/sessions, /api/sessions/{id}, /api/folders
├── model/                            # Java records: Session, SessionDetail, Folder + response wrappers
└── service/WaveAiService.java        # Calls WaveAI REST API, wraps errors as 502
```

### Frontend layout
```
frontend/src/
├── api/          # Axios clients (sessions.js, folders.js) — baseURL /waveai/api
├── components/   # SessionList, SessionCard
├── views/        # SessionDetail, FolderList
└── router/       # Vue Router — /, /session/:id, /folders
```

### Adding a new WaveAI endpoint
1. Add a Java `record` in `model/` with `@JsonIgnoreProperties(ignoreUnknown = true)`
2. Add a method to `WaveAiService` using `restClient.get().uri(...).retrieve().body(...)`
3. Add a `@GetMapping` in `SessionController`
4. Add a fetch function in the appropriate `frontend/src/api/*.js` file
