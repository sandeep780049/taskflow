import { useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, JOB_STATUSES, JOB_TYPES, type CreateJobBody, type Job } from '../api';
import { StatusBadge } from '../App';

const dt = (iso?: string) => (iso ? new Date(iso).toLocaleString() : '—');

function CreateJobForm({ onDone }: { onDone: () => void }) {
  const qc = useQueryClient();
  const [form, setForm] = useState({
    name: '', description: '', jobType: 'ECHO', payload: '',
    priority: 5, scheduledAt: '', recurringCron: '', timezone: '',
    maxAttempts: 3, timeoutSeconds: 60, idempotencyKey: '',
  });
  const [error, setError] = useState<string | null>(null);

  const create = useMutation({
    mutationFn: (body: CreateJobBody) => api.createJob(body),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['jobs'] }); onDone(); },
    onError: (e: Error) => setError(e.message),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    let payload: unknown = undefined;
    if (form.payload.trim()) {
      try { payload = JSON.parse(form.payload); }
      catch { setError('Payload is not valid JSON'); return; }
    }
    create.mutate({
      name: form.name,
      description: form.description || undefined,
      jobType: form.jobType,
      payload,
      priority: form.priority,
      scheduledAt: form.scheduledAt ? new Date(form.scheduledAt).toISOString() : undefined,
      recurringCron: form.recurringCron || undefined,
      timezone: form.timezone || undefined,
      maxAttempts: form.maxAttempts,
      timeoutSeconds: form.timeoutSeconds,
      idempotencyKey: form.idempotencyKey || undefined,
    });
  };

  const set = (k: string, v: string | number) => setForm(f => ({ ...f, [k]: v }));

  return (
    <form className="card" onSubmit={submit}>
      <h2>New job</h2>
      <div className="form-grid">
        <label>Name <input required value={form.name} onChange={e => set('name', e.target.value)} /></label>
        <label>Type
          <select value={form.jobType} onChange={e => set('jobType', e.target.value)}>
            {JOB_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
          </select>
        </label>
        <label>Priority (1–9, 9 first)
          <input type="number" min={1} max={9} value={form.priority} onChange={e => set('priority', Number(e.target.value))} />
        </label>
        <label>Max attempts
          <input type="number" min={1} max={10} value={form.maxAttempts} onChange={e => set('maxAttempts', Number(e.target.value))} />
        </label>
        <label>Timeout (s)
          <input type="number" min={1} max={600} value={form.timeoutSeconds} onChange={e => set('timeoutSeconds', Number(e.target.value))} />
        </label>
        <label>Scheduled at (UTC future = scheduled, empty = now)
          <input type="datetime-local" value={form.scheduledAt} onChange={e => set('scheduledAt', e.target.value)} />
        </label>
        <label>Recurring cron (6 fields, e.g. 0 0 2 * * *)
          <input value={form.recurringCron} onChange={e => set('recurringCron', e.target.value)} placeholder="0 0 2 * * *" />
        </label>
        <label>Timezone (for cron)
          <input value={form.timezone} onChange={e => set('timezone', e.target.value)} placeholder="optional, e.g. Asia/Kolkata" />
        </label>
        <label>Idempotency key (optional)
          <input value={form.idempotencyKey} onChange={e => set('idempotencyKey', e.target.value)} />
        </label>
      </div>
      <label>Payload (JSON)
        <textarea rows={3} value={form.payload} onChange={e => set('payload', e.target.value)}
                  placeholder='{"duration_ms": 5000}' />
      </label>
      <p className="muted">
        ECHO: returns the payload · WAIT: sleeps duration_ms · HTTP_REQUEST: {"{"}url{"}"} GET to a public host ·
        DEMO_FAILURE: {"{"}fail_times: n{"}"} fails first n attempts
      </p>
      {error && <p className="error">{error}</p>}
      <div className="row">
        <button className="btn primary" disabled={create.isPending}>{create.isPending ? 'Creating…' : 'Create job'}</button>
        <button type="button" className="btn ghost" onClick={onDone}>Cancel</button>
      </div>
    </form>
  );
}

export default function Jobs() {
  const [params, setParams] = useSearchParams();
  const status = params.get('status') ?? '';
  const page = Number(params.get('page') ?? 0);
  const [showForm, setShowForm] = useState(false);

  const { data, isLoading, error } = useQuery({
    queryKey: ['jobs', status, page],
    queryFn: () => api.listJobs(status || undefined, page),
    refetchInterval: 5000,
  });

  return (
    <>
      <div className="row spread">
        <h1>Jobs</h1>
        <button className="btn primary" onClick={() => setShowForm(s => !s)}>
          {showForm ? 'Close form' : '+ New job'}
        </button>
      </div>
      {showForm && <CreateJobForm onDone={() => setShowForm(false)} />}

      <div className="row wrap chips">
        <button className={'chip' + (status === '' ? ' active' : '')} onClick={() => setParams({})}>All</button>
        {JOB_STATUSES.map(s => (
          <button key={s} className={'chip' + (status === s ? ' active' : '')}
                  onClick={() => setParams({ status: s })}>{s.replaceAll('_', ' ')}</button>
        ))}
      </div>

      {isLoading && <p className="muted">Loading…</p>}
      {error && <p className="error">Failed to load jobs: {(error as Error).message}</p>}
      {data && (data.items.length === 0
        ? <p className="muted">No jobs here yet.</p>
        : <table className="table">
            <thead>
              <tr><th>Name</th><th>Type</th><th>Status</th><th>Priority</th><th>Attempt</th><th>Schedule</th><th>Created</th></tr>
            </thead>
            <tbody>
              {data.items.map((job: Job) => (
                <tr key={job.id}>
                  <td><Link to={`/jobs/${job.id}`}>{job.name}</Link>{job.recurringCron && <span className="muted"> ↻ {job.recurringCron}</span>}</td>
                  <td>{job.jobType}</td>
                  <td><StatusBadge status={job.status} /></td>
                  <td>{job.priority}</td>
                  <td>{job.attempts}/{job.maxAttempts}</td>
                  <td title={job.nextAttemptAt ?? ''}>{job.nextAttemptAt ? dt(job.nextAttemptAt) : '—'}</td>
                  <td>{dt(job.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>)}
      {data && data.totalPages > 1 && (
        <div className="row">
          <button className="btn ghost" disabled={page === 0}
                  onClick={() => setParams({ status, page: String(page - 1) })}>← Prev</button>
          <span className="muted">Page {page + 1} of {data.totalPages}</span>
          <button className="btn ghost" disabled={page + 1 >= data.totalPages}
                  onClick={() => setParams({ status, page: String(page + 1) })}>Next →</button>
        </div>
      )}
    </>
  );
}
