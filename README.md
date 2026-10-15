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

2. **Active deployments:**

| Service | URL | Status |
|---|---|---|
| `taskflow-api` (web service) | `https://taskflow-api-nsg7.onrender.com` | ✓ Live (commit `6f86923`) |
| `taskflow-db` (Postgres DB) | `taskflow-db` on Render | Available |

3. **Demo login** (shared): `demo@taskflow.dev` / `demo-pass-1234`

## 📚 Other projects by sandeep780049

| Project | Description | Live URL |
|---|---|---|
| [TaskFlow](./README.md) | Distributed job scheduler — schedule, execute, retry and monitor background jobs | [🌐 Live](https://taskflow-api-nsg7.onrender.com) |
| [AlgoArena](algoarena/README.md) | Competitive coding contests, quizzes, and practice — all in one platform | [🌐 Live](https://algoarena-9nwy.onrender.com) |
| [PulseWatch](pulsewatch/README.md) | Open-source uptime monitoring — heartbeat checks, latency tracking, p95 response times | [🌐 Live](https://pulsewatch-web.onrender.com) |
| [Booking Platform](Desktop/booking-platform/README.md) | Fullstack booking management — adventure bookings, hotels, item rentals with real-time chat | [🌐 Live](https://booking-platform-9tlk.onrender.com) |
| [SKTIME Work](sktime-work/README.md) | Time series forecasting and analysis utilities | — |
| [Kestra Work](kestra-work/README.md) | Workflow automation and orchestrator experiments | — |
| [Vitest PR Fork](vitest-pr-fork/README.md) | Pr-rated test runner fork for GitHub PR workflows | — |
| [GH Pr Radar](gh-pr-radar/README.md) | One-command open PR viewer across all GitHub repos | — |
| [Route Opt](route-opt/README.md) | Route optimization utilities | — |
| [Vite Work](vite-work/README.md) | Vite project experiments | — |
| [StoatDB](stoatdb/README.md) | Database utilities | — |
| [Stock Market Analysis](stock-market-analysis-powerbi/README.md) | Stock market analysis and PowerBI dashboards | — |
| [Stroke Prediction](stroke-prediction/README.md) | Healthcare machine learning — stroke prediction model | — |
| [PyAlgos](pyalgos/README.md) | Python algorithm implementations | — |

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
`/api/metrics` · `/api/workers` (admin) · `/actuator/health` ·
`/swagger-ui.html` (full OpenAPI).

## Architecture & design notes

See [docs/architecture.md](docs/architecture.md) for the data-flow diagram,
Mermaid ER model, state machine, claiming/lease/recovery semantics and the
planned outbox→RabbitMQ evolution.

## Deployment

- **Render**: `render.yaml` blueprint (free web service + free Postgres),
  deploy button: [![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy?repo=https://github.com/sandeep780049/taskflow)
- **CI**: `.github/workflows/ci.yml` runs backend tests (including
  Testcontainers against real Postgres on the GitHub runner) and builds the dashboard.
- **Env vars**: `DATABASE_URL|DATABASE_USERNAME|DATABASE_PASSWORD|TASKFLOW_JWT_SECRET|WORKER_NAME`.

## Can I run tests without Docker?

Yes — unit tests always run; the Testcontainers integration tests skip
automatically when no Docker daemon is available and run in CI.
