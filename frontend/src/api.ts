const API_BASE = import.meta.env.VITE_API_URL ?? '';

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

const TOKEN_KEY = 'taskflow.token';
export const getToken = () => localStorage.getItem(TOKEN_KEY);
export const setToken = (t: string) => localStorage.setItem(TOKEN_KEY, t);
export const clearToken = () => localStorage.removeItem(TOKEN_KEY);

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken();
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });
  if (res.status === 204) return undefined as T;
  const body = await res.json().catch(() => null);
  if (!res.ok) {
    if (res.status === 401) clearToken();
    throw new ApiError(res.status, body?.message ?? `Request failed (${res.status})`);
  }
  return body as T;
}

export interface User { id: string; email: string; role: string }
export interface LoginResponse { token: string; user: User }

export interface Job {
  id: string; name: string; description?: string; jobType: string; payload?: unknown;
  status: string; priority: number; scheduledAt?: string; recurringCron?: string; timezone?: string;
  maxAttempts: number; attempts: number; timeoutSeconds: number; nextAttemptAt?: string;
  idempotencyKey?: string; lockedBy?: string; lockedUntil?: string; cancelRequested: boolean;
  createdAt?: string; updatedAt?: string; version: number;
}
export interface Execution {
  id: string; attemptNumber: number; workerId?: string; status: string;
  startedAt?: string; finishedAt?: string; durationMs?: number;
  result?: unknown; errorCode?: string; errorMessage?: string;
}
export interface JobLog {
  id: string; executionId?: string; level: string; message: string; timestamp?: string;
}
export interface JobDetail { job: Job; executions: Execution[]; logs: JobLog[] }
export interface JobPage { page: number; totalPages: number; totalElements: number; items: Job[] }
export interface CreateJobBody {
  name: string; description?: string; jobType: string; payload?: unknown;
  priority?: number; scheduledAt?: string; recurringCron?: string; timezone?: string;
  maxAttempts?: number; timeoutSeconds?: number; idempotencyKey?: string;
}
export interface HourlyStats { hour: string; total: number; succeeded: number; avgDurationMs: number }
export interface MetricsSummary {
  jobsByStatus: Record<string, number>;
  successRate24h: number; avgDurationMs24h: number;
  hourly: HourlyStats[]; activeWorkers: number; staleWorkers: number;
}
export interface WorkerInfo {
  name: string; status: string; lastHeartbeatAt?: string; registeredAt?: string;
}

export const api = {
  register: (email: string, password: string) =>
    request<LoginResponse>('/api/auth/register', { method: 'POST', body: JSON.stringify({ email, password }) }),
  login: (email: string, password: string) =>
    request<LoginResponse>('/api/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),
  me: () => request<User>('/api/auth/me'),

  listJobs: (status?: string, page = 0, size = 20) =>
    request<JobPage>(`/api/jobs?${status ? `status=${status}&` : ''}page=${page}&size=${size}`),
  getJob: (id: string) => request<JobDetail>(`/api/jobs/${id}`),
  createJob: (body: CreateJobBody) =>
    request<Job>('/api/jobs', { method: 'POST', body: JSON.stringify(body) }),
  cancelJob: (id: string) =>
    request<{ cancelled: boolean }>(`/api/jobs/${id}/cancel`, { method: 'POST' }),
  deleteJob: (id: string) => request<void>(`/api/jobs/${id}`, { method: 'DELETE' }),

  metrics: () => request<MetricsSummary>('/api/metrics'),
  workers: () => request<WorkerInfo[]>('/api/workers'),
};

export const JOB_TYPES = ['ECHO', 'WAIT', 'HTTP_REQUEST', 'DEMO_FAILURE'] as const;
export const JOB_STATUSES = ['SCHEDULED', 'QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'RETRY_WAIT', 'CANCELLED', 'DEAD_LETTER'] as const;
