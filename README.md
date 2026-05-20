# DevSignal

DevSignal is a React + Spring Boot application that analyzes GitHub profiles, stores user state in Supabase-backed Postgres, and can generate OpenAI-powered reports and roadmaps.

## Stack

- Frontend: React + Vite
- Backend: Spring Boot + JPA + Flyway
- Database/Auth: Supabase Postgres + Supabase Auth
- AI: OpenAI

## Environment

The repo now supports environment-driven frontend and backend configuration for local development, Docker, and AWS deployment.

### Frontend

Copy `frontend/.env.example` to `frontend/.env.local` and fill in the values you need.

Important variables:

- `VITE_API_BASE_URL`: backend base URL, for example `http://localhost:8080` locally or your App Runner URL in production
- `VITE_SUPABASE_URL`
- `VITE_SUPABASE_PUBLISHABLE_KEY`

### Backend

Use `backend/.env.example` as the reference for required runtime variables.

Important variables:

- `PORT`
- `GITHUB_TOKEN`
- `OPENAI_API_KEY`
- `OPENAI_MODEL`
- `SUPABASE_DB_URL`
- `SUPABASE_DB_USERNAME`
- `SUPABASE_DB_PASSWORD`
- `SUPABASE_URL`
- `SUPABASE_PUBLISHABLE_KEY`
- `APP_CORS_ALLOWED_ORIGIN_PATTERNS`

## Local Development

### Frontend

```bash
cd frontend
npm install
npm run dev
```

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

## Docker

Use `docker-compose.yml` with a root `.env` file based on `.env.example`.

```bash
docker compose up --build
```

This starts:

- frontend on `http://localhost:5173`
- backend on `http://localhost:8080`

## Deployment Direction

Recommended first AWS setup:

- Frontend: Amplify Hosting or S3 + CloudFront
- Backend: AWS App Runner from the backend container image
- Secrets: AWS Secrets Manager
- Database/Auth: keep Supabase for v1

## Health Check

The backend exposes Spring Boot health checks at `/actuator/health`.
