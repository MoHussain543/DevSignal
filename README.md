# DevSignal

DevSignal is a full-stack GitHub portfolio analysis app that turns a public GitHub username into a scored report, an AI-written portfolio summary, and an AI-generated improvement roadmap.

The product is built around a simple split:

- the **core scored GitHub report** is public
- the **AI Report** and **AI Roadmap** require sign-in through Supabase Auth

DevSignal is deployed on AWS with:

- **frontend hosting** on AWS Amplify
- **backend containers** on Amazon ECS (Fargate) behind an Application Load Balancer
- **backend HTTPS** through ACM + Route 53
- **user auth and profile persistence** through Supabase

## What DevSignal Does

Given a public GitHub username, DevSignal analyzes signals such as:

- repository quality
- README and documentation depth
- language and stack variety
- originality of work
- recent activity
- overall portfolio signal

From that, the app can produce:

1. a **public scored report**
2. an **AI Report Summary** with a hiring-style interpretation
3. an **AI Roadmap** with concrete next steps and project guidance

## Core Features

### Public GitHub report

- username-based analysis with no GitHub OAuth required
- repository-by-repository scoring and notes
- overall portfolio score and summary
- frontend report experience built around a landing page + report flow

### Account-based AI features

- Supabase Auth sign-in / sign-up flow
- protected frontend routes for AI features
- backend token validation before AI endpoints can be used
- AI-generated summary and roadmap tied to the signed-in user's profile

### Saved profile state

- latest linked GitHub username
- latest AI report
- latest roadmap
- personal DevSignal profile page

## Tech Stack

### Frontend

- React
- Vite
- React Router
- custom CSS design system

### Backend

- Spring Boot
- Spring Data JPA
- Flyway
- PostgreSQL driver

### Auth / Data

- Supabase Auth
- Supabase Postgres

### AI

- OpenAI Java SDK

### Infrastructure

- AWS Amplify
- Amazon ECS (Fargate)
- Amazon ECR
- Application Load Balancer
- AWS Certificate Manager (ACM)
- Route 53
- GitHub Actions

## Architecture

### Frontend delivery

- frontend lives in `frontend/`
- hosted by Amplify
- pushes to the connected branch trigger automatic frontend rebuilds and redeploys

### Backend delivery

- backend lives in `backend/`
- packaged as a Docker image
- pushed to Amazon ECR
- ECS service pulls the updated image and redeploys
- ALB exposes the service publicly
- backend health check is available at `/actuator/health`

### Auth model

- unauthenticated users can access the public scored report
- authenticated users can access:
  - `/ai-report`
  - `/ai-report/:username`
  - `/roadmap`
  - `/roadmap/:username`
- backend AI endpoints also require a valid Supabase bearer token

## Environment Variables

The app uses environment-driven configuration for local development and AWS deployment.

### Frontend

Copy `frontend/.env.example` to `frontend/.env.local`.

Required values:

- `VITE_API_BASE_URL`
- `VITE_SUPABASE_URL`
- `VITE_SUPABASE_PUBLISHABLE_KEY`

Example local frontend config:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_SUPABASE_URL=...
VITE_SUPABASE_PUBLISHABLE_KEY=...
```

### Backend

Use `backend/.env.example` as the reference.

Important backend variables:

- `PORT`
- `GITHUB_TOKEN`
- `OPENAI_API_KEY`
- `OPENAI_MODEL`
- `OPENAI_ENABLED`
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

Runs on:

- `http://localhost:5173`

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Runs on:

- `http://localhost:8080`

## Testing And Quality Checks

### Frontend checks

From `frontend/`:

```bash
npm run lint
npm run build
npm run test:e2e
```

These cover:

- ESLint checks for the React/Vite frontend
- production build validation
- Playwright end-to-end coverage for key landing/auth flows

### Backend checks

From `backend/`:

```bash
./mvnw test
```

This runs the Spring Boot test suite used by backend CI.

## Docker

The backend is containerized and can be built directly through Docker.

From the repo root:

```bash
docker buildx build --platform linux/amd64 -t devsignal-backend ./backend --load
```

There is also a `docker-compose.yml` flow for local multi-service development:

```bash
docker compose up --build
```

## AWS Deployment

### Frontend

- deployed through Amplify
- rebuilds and redeploys automatically on push to the connected branch

### Backend

- Docker image stored in ECR
- ECS service runs the Spring Boot container
- ALB exposes the service
- HTTPS is configured with:
  - Route 53
  - ACM
  - ALB HTTPS listener

### Production backend health check

```text
/actuator/health
```

## CI/CD

This repo uses GitHub Actions for CI/CD and AWS managed services for hosting.

### Frontend delivery model

- GitHub Actions handles **frontend CI**
- AWS Amplify handles **frontend deployment**
- pushes to the connected branch trigger Amplify rebuilds and redeploys automatically

### Backend delivery model

- GitHub Actions handles **backend CI**
- GitHub Actions also handles **backend deployment**
- the deploy workflow builds a Docker image, pushes it to ECR, and forces ECS to roll out the new container version

### Workflow summary

This repo includes three GitHub Actions workflows:

- **Frontend CI**
  - installs frontend dependencies
  - runs ESLint
  - runs the production frontend build
  - runs Playwright end-to-end tests after the build job succeeds
  - uses concurrency groups to cancel superseded runs on the same branch

- **Backend CI**
  - sets up Java 21
  - runs the backend Maven test suite
  - uses concurrency groups to cancel superseded runs on the same branch

- **Backend CD**
  - runs after backend CI passes on `main`
  - authenticates to AWS
  - builds the backend Docker image for `linux/amd64`
  - pushes both a commit-based image tag and `latest` to ECR
  - forces a new ECS deployment

### Current GitHub Actions behavior

- pull requests run CI checks before merge
- pushes to `main` run CI again
- successful backend CI on `main` triggers automated ECS deployment
- frontend deploys stay managed by Amplify instead of a custom GitHub Actions deploy job

Current workflow files:

- `.github/workflows/frontend-ci.yml`
- `.github/workflows/backend-ci.yml`
- `.github/workflows/backend-deploy.yml`

## Project Structure

```text
DevSignal/
├── backend/
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/
│   ├── package.json
│   └── package-lock.json
├── .github/
│   └── workflows/
└── README.md
```

## Why This Project Is Interesting

DevSignal is more than a simple GitHub API toy project. It combines:

- public GitHub analysis
- account-gated AI features
- saved user state
- containerized backend infrastructure
- AWS deployment across multiple services
- GitHub Actions automation for CI/CD

It is meant to show both product thinking and real deployment/operations experience.
