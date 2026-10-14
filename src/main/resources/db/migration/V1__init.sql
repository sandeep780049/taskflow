-- TaskFlow initial schema (PostgreSQL)

CREATE TABLE users (
    id             UUID PRIMARY KEY,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    role           VARCHAR(20)  NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE workers (
    id                 UUID PRIMARY KEY,
    name               VARCHAR(120) NOT NULL UNIQUE,
    status             VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'STALE', 'STOPPED')),
    last_heartbeat_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    registered_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    metadata           JSONB
);
CREATE INDEX idx_workers_heartbeat ON workers (last_heartbeat_at);

CREATE TABLE jobs (
    id               UUID PRIMARY KEY,
    owner_id         UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name             VARCHAR(160) NOT NULL,
    description      VARCHAR(2000),
    job_type         VARCHAR(40)  NOT NULL,
    payload          JSONB,
    status           VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED'
        CHECK (status IN ('SCHEDULED','QUEUED','RUNNING','SUCCEEDED','FAILED','RETRY_WAIT','CANCELLED','DEAD_LETTER')),
    priority         SMALLINT NOT NULL DEFAULT 5 CHECK (priority BETWEEN 1 AND 9),
    scheduled_at     TIMESTAMPTZ,
    recurring_cron   VARCHAR(80),
    timezone         VARCHAR(60),
    max_attempts     SMALLINT NOT NULL DEFAULT 3 CHECK (max_attempts BETWEEN 1 AND 10),
    attempts         SMALLINT NOT NULL DEFAULT 0,
    timeout_seconds  INT NOT NULL DEFAULT 60 CHECK (timeout_seconds BETWEEN 1 AND 600),
    next_attempt_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    idempotency_key  VARCHAR(120),
    locked_by        VARCHAR(120),
    locked_until     TIMESTAMPTZ,
    cancel_requested BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    version          BIGINT NOT NULL DEFAULT 0
);

-- job-claiming query: due queued jobs by priority
CREATE INDEX idx_jobs_claim ON jobs (status, priority DESC, next_attempt_at);
-- schedule due promotion
CREATE INDEX idx_jobs_scheddue ON jobs (scheduled_at) WHERE status = 'SCHEDULED';
CREATE INDEX idx_jobs_retrydue ON jobs (next_attempt_at) WHERE status = 'RETRY_WAIT';
-- owner listing / status filtering
CREATE INDEX idx_jobs_owner ON jobs (owner_id, created_at DESC);
CREATE INDEX idx_jobs_status ON jobs (status);
-- lease recovery
CREATE INDEX idx_jobs_lease ON jobs (locked_until) WHERE status = 'RUNNING';
-- idempotency scope: unique per owner, only when the key is supplied
CREATE UNIQUE INDEX uq_jobs_owner_idem ON jobs (owner_id, idempotency_key) WHERE idempotency_key IS NOT NULL;

CREATE TABLE job_executions (
    id             UUID PRIMARY KEY,
    job_id         UUID NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
    attempt_number SMALLINT NOT NULL,
    worker_id      VARCHAR(120),
    status         VARCHAR(20) NOT NULL
        CHECK (status IN ('RUNNING','SUCCEEDED','FAILED')),
    started_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at    TIMESTAMPTZ,
    duration_ms    BIGINT,
    result         JSONB,
    error_code     VARCHAR(40),
    error_message  VARCHAR(2000),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (job_id, attempt_number)
);
CREATE INDEX idx_executions_job ON job_executions (job_id, attempt_number DESC);
CREATE INDEX idx_executions_started ON job_executions (started_at DESC);

CREATE TABLE job_logs (
    id            UUID PRIMARY KEY,
    job_id        UUID NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
    execution_id  UUID REFERENCES job_executions (id) ON DELETE CASCADE,
    level         VARCHAR(10) NOT NULL DEFAULT 'INFO' CHECK (level IN ('INFO','WARN','ERROR','DEBUG')),
    message       VARCHAR(4000) NOT NULL,
    timestamp     TIMESTAMPTZ NOT NULL DEFAULT now(),
    metadata      JSONB
);
CREATE INDEX idx_job_logs_job_time ON job_logs (job_id, timestamp DESC);

CREATE TABLE outbox_events (
    id            UUID PRIMARY KEY,
    aggregate_id  UUID NOT NULL,
    event_type    VARCHAR(60) NOT NULL,
    payload       JSONB,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at  TIMESTAMPTZ,
    attempts      SMALLINT NOT NULL DEFAULT 0,
    last_error    VARCHAR(2000)
);
CREATE INDEX idx_outbox_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;
