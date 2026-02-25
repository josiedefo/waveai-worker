# WaveAI Worker

A simple web app that fetches and displays session data from the WaveAI Note Taking tool.

**Tech stack:** Vue 3 + Vite (frontend) | Java 21 + Spring Boot 3.5 (backend)

## Prerequisites

- Java 21+
- Maven 3.9+
- Node.js 22+ (dev only)
- AWS CLI v2 (for deployment)

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

Open http://localhost:5173/waveai/

The Vite dev server proxies `/waveai/api` requests to Spring Boot on port 8080.

## Production Build

Single command builds everything (frontend + backend) into one executable JAR:

```bash
mvn clean package -DskipTests
java -jar target/waveai-worker-0.0.1-SNAPSHOT.jar
```

Open http://localhost:8080/waveai/

## Deploy to AWS Elastic Beanstalk

### Prerequisites

- AWS CLI configured (`aws configure`)
- IAM user with permissions for: Elastic Beanstalk, S3, IAM instance profiles

### Deploy

```bash
export WAVEAI_API_TOKEN=your-token-here
bash scripts/deploy.sh
```

Optionally pass a region (default: `us-east-1`):

```bash
bash scripts/deploy.sh eu-west-1
```

The script will:
1. Build the project with `mvn clean package`
2. Create an S3 bucket for deploy artifacts (if it doesn't exist)
3. Create an Elastic Beanstalk application and environment (first run only)
4. Deploy the app and wait for it to go healthy
5. Print the live URL

The `WAVEAI_API_TOKEN` is set as an environment variable on the EB environment — it never touches the source code or the deploy ZIP.

### Subsequent deploys

Same command — the script detects the environment already exists and just updates it:

```bash
export WAVEAI_API_TOKEN=your-token-here
bash scripts/deploy.sh
```

## Project Structure

```
├── Procfile                         # Tells Elastic Beanstalk how to run the app
├── .ebextensions/app.config         # EB health check path config
├── scripts/deploy.sh                # AWS deploy script (uses AWS CLI)
├── src/assembly/eb.xml              # Maven assembly: packages JAR + Procfile into deploy ZIP
├── pom.xml                          # Maven build
├── src/main/java/com/waveai/worker/
│   ├── WaveaiWorkerApplication.java # Entry point
│   ├── config/RestClientConfig.java # RestClient + CORS
│   ├── controller/SessionController.java # GET /api/sessions
│   ├── model/Session.java           # Session DTO
│   └── service/WaveAiService.java   # WaveAI API client
├── src/main/resources/
│   └── application.yml              # Configuration (port, context path, API URL)
└── frontend/
    ├── src/
    │   ├── App.vue                  # Root component
    │   ├── api/sessions.js          # Axios client
    │   └── components/
    │       ├── SessionList.vue      # Session grid
    │       └── SessionCard.vue      # Session card
    └── vite.config.js               # Vite config (base path, proxy, build output)
```
