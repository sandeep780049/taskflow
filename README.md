# TaskFlow — Distributed Job Scheduler

TaskFlow is a full-stack, production-minded platform to **create, schedule, execute, monitor, retry, cancel and audit background jobs**:

- immediate, one-shot future and recurring (cron) jobs with priorities
- distributed workers claiming jobs via `SELECT ... FOR UPDATE SKIP LOCKED`
- leases + heartbeat renewal, timeout enforcement, graceful cancellation
- automatic retries with exponential backoff and dead-lettering
- per-attempt execution records and log streams
- worker registry with health heartbeats
- JWT auth with owner-scoped job access (ADMIN sees all)
- React (TypeScript + Vite + TanStack Query + Recharts) dashboard
- JUnit/Mockito unit tests + Testcontainers end-to-end lifecycle tests
- Docker, Docker Compose (two worker instances), GitHub Actions CI, Render blueprint

## 🌐 Live Deployment (Permanent)

TaskFlow is permanently hosted on **Render** — no Cloudflare tunnel dependency, no rotating URLs.

1. **One-click deploy:** [![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy?repo=https://github.com/sandeep780049/taskflow)

2. **Live URL:** → [**https://taskflow-api-nsg7.onrender.com**](https://taskflow-api-nsg7.onrender.com)

3. **Demo login** (shared): `demo@taskflow.dev` / `demo-pass-1234`

## How to redeploy (any time)

Just **push to `main`** — GitHub Actions auto-builds and deploys. That's it.

## Quick start (local, without Docker)

Requirements: Java 21+, Maven 3.9+, Node 22+, PostgreSQL 16.

```bash
# 1. a Postgres (docker run -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:16-alpine)
# 2. backend
./mvnw spring-boot:run          # http://localhost:8080, Swagger at /swagger-ui.html
# 3. dashboard with HMR (dev)
cd frontend && npm install && npm run dev   # http://localhost:5173 (proxies /api)
```

## Quick start (Docker Compose — two distributed workers)

```bash
docker compose up --build      # API 1 on :8080, API 2 on :8081, shared Postgres
```

Both services register themselves as workers and compete for the same job
queue — open both dashboards and create jobs to see them claimed across
instances.

## Safe job types

| Type | Purpose | Payload |
|---|---|---|
| `ECHO` | canonical happy path | anything (returned) |
| `WAIT` | real long-running work + cancellation demo | `{"duration_ms": 0-120000}` |
| `HTTP_REQUEST` | outbound GET with SSRF protection | `{"url": "https://..."}` (public hosts only) |
| `DEMO_FAILURE` | exercises retry/backoff/dead-letter | `{"fail_times": n}` fails first n attempts |

Arbitrary user code/shell is never executed. Handlers are idempotent;
delivery is documented as at-least-once.

## API overview

`/api/auth/register|login|me` · `/api/jobs` (CRUD, filters, cancel) ·
`/api/metrics` · `/api/workers` (admin) · `/actuator/health` · `/swagger-ui.html` (full OpenAPI).

## Architecture & design notes

See [docs/architecture.md](docs/architecture.md) for the data-flow diagram,
Mermaid ER model, state machine, claiming/lease/recovery semantics and the
planned outbox→RabbitMQ evolution.
