# WaveAI Worker

A simple web app that fetches and displays session data from the WaveAI Note Taking tool.

**Tech stack:** Vue 3 + Vite (frontend) | Java 21 + Spring Boot 3.5 (backend)

## Prerequisites

- Java 21+
- Maven 3.9+
- Node.js 22+

## Setup

Set your WaveAI API token as an environment variable:

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

**Frontend** (Terminal 2):

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173

The Vite dev server proxies `/api` requests to Spring Boot on port 8080.

## Production Build

```bash
cd frontend && npm run build && cd ..
mvn clean package
java -jar target/waveai-worker-0.0.1-SNAPSHOT.jar
```

Open http://localhost:8080

## Project Structure

```
├── pom.xml                          # Maven build
├── src/main/java/com/waveai/worker/
│   ├── WaveaiWorkerApplication.java # Entry point
│   ├── config/RestClientConfig.java # RestClient + CORS
│   ├── controller/SessionController.java # GET /api/sessions
│   ├── model/Session.java           # Session DTO
│   └── service/WaveAiService.java   # WaveAI API client
├── src/main/resources/
│   └── application.yml              # Configuration
└── frontend/
    ├── src/
    │   ├── App.vue                  # Root component
    │   ├── api/sessions.js          # Axios client
    │   └── components/
    │       ├── SessionList.vue      # Session grid
    │       └── SessionCard.vue      # Session card
    └── vite.config.js               # Vite + proxy config
```
