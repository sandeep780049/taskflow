import { useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, JOB_STATUSES, JOB_TYPES, type CreateJobBody, type Job } from '../api';
import { StatusBadge } from '../App';
import { useToast } from '../toast';

const dt = (iso?: string) => (iso ? new Date(iso).toLocaleString() : '—');

function rel(iso?: string): string {
  if (!iso) return '—';
  const d = new Date(iso).getTime() - Date.now();
  const abs = Math.abs(d);
  const units: [number, string][] = [[1000, 's'], [60, 'm'], [60, 'h'], [24, 'd']];
  let value = abs / 1000;
  let unit = 's';
  for (const [div, u] of units) { if (value >= div && u !== 's') { value /= div; unit = u; } else break; }
  const rounded = value < 10 ? Math.round(value * 10) / 10 : Math.round(value);
  const suffix = `${rounded}${unit}`;
  return d >= 0 ? `in ${suffix}` : `${suffix} ago`;
}

function CreateJobForm({ onDone }: { onDone: () => void }) {
  const qc = useQueryClient();
  const toast = useToast();
  const [form, setForm] = useState({
    name: '', description: '', jobType: 'ECHO', payload: '',
    priority: 5, scheduledAt: '', recurringCron: '', timezone: '',
    maxAttempts: 3, timeoutSeconds: 60, idempotencyKey: '',
  });
  const [error, setError] = useState<string | null>(null);

  const create = useMutation({
    mutationFn: (body: CreateJobBody) => api.createJob(body),
    onSuccess: (job) => {
      qc.invalidateQueries({ queryKey: ['jobs'] });
      toast(`Job "${job.name}" created`, 'ok');
      onDone();
    },
    onError: (e: Error) => { setError(e.message); toast(e.message, 'err'); },
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

  const presets: Record<string, string> = {
    ECHO: '{"message": "hello"}',
    WAIT: '{"duration_ms": 8000}',
    DEMO_FAILURE: '{"fail_times": 2}',
    HTTP_REQUEST: '{"url": "https://httpbin.org/get"}',
  };

  return (
    <form className="card" onSubmit={submit}>
      <div className="row spread">
        <h2 style={{ margin: 0 }}>New job</h2>
        <button type="button" className="btn ghost small" onClick={onDone}>✕ Close</button>
      </div>
      <div className="form-grid">
        <label>Name <input required value={form.name} onChange={e => set('name', e.target.value)} placeholder="nightly-report" /></label>
        <label>Type
          <select value={form.jobType} onChange={e => { set('jobType', e.target.value); set('payload', presets[e.target.value]); }}>
            {JOB_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
          </select>
        </label>
        <label>Priority (9 runs first)
          <input type="number" min={1} max={9} value={form.priority} onChange={e => set('priority', Number(e.target.value))} />
        </label>
        <label>Max attempts
          <input type="number" min={1} max={10} value={form.maxAttempts} onChange={e => set('maxAttempts', Number(e.target.value))} />
        </label>
        <label>Timeout (seconds)
          <input type="number" min={1} max={600} value={form.timeoutSeconds} onChange={e => set('timeoutSeconds', Number(e.target.value))} />
        </label>
        <label>Scheduled at (empty = run now)
          <input type="datetime-local" value={form.scheduledAt} onChange={e => set('scheduledAt', e.target.value)} />
        </label>
        <label>Recurring cron (6 fields)
          <input value={form.recurringCron} onChange={e => set('recurringCron', e.target.value)} placeholder="0 0 2 * * *" />
        </label>
        <label>Cron timezone
          <input value={form.timezone} onChange={e => set('timezone', e.target.value)} placeholder="optional, e.g. Asia/Kolkata" />
        </label>
        <label>Idempotency key
          <input value={form.idempotencyKey} onChange={e => set('idempotencyKey', e.target.value)} placeholder="optional" />
        </label>
        <label>Description
          <input value={form.description} onChange={e => set('description', e.target.value)} placeholder="optional" />
        </label>
      </div>
      <label>Payload (JSON)
        <textarea rows={3} value={form.payload} onChange={e => set('payload', e.target.value)} placeholder='{"duration_ms": 5000}' />
      </label>
      <p className="muted" style={{ margin: '4px 0 10px' }}>
        Preset filled on type switch · ECHO returns payload · WAIT sleeps duration_ms · HTTP_REQUEST GETs a public url · DEMO_FAILURE fails first n attempts
      </p>
      {error && <p className="error">{error}</p>}
      <button className="btn primary" disabled={create.isPending}>
        {create.isPending ? 'Creating…' : 'Create job'}
      </button>
    </form>
  );
}

export default function Jobs() {
  const [params, setParams] = useSearchParams();
  const status = params.get('status') ?? '';
  const page = Number(params.get('page') ?? 0);
  const [showForm, setShowForm] = useState(false);
  const qc = useQueryClient();
  const toast = useToast();

  const { data, isLoading, error } = useQuery({
    queryKey: ['jobs', status, page],
    queryFn: () => api.listJobs(status || undefined, page),
    refetchInterval: 5000,
  });

  const cancel = useMutation({
    mutationFn: (id: string) => api.cancelJob(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['jobs'] }); toast('Cancel requested', 'ok'); },
    onError: (e: Error) => toast(e.message, 'err'),
  });

  const quick = (type: string, payload: string, name: string) => {
    api.createJob({ name: `${name}-${Date.now() % 10000}`, jobType: type, payload: JSON.parse(payload) })
      .then(() => { qc.invalidateQueries({ queryKey: ['jobs'] }); toast(`${name} queued`, 'ok'); })
      .catch((e: Error) => toast(e.message, 'err'));
  };

  return (
    <>
      <div className="row spread">
        <h1>Jobs</h1>
        <button className="btn primary" onClick={() => setShowForm(s => !s)}>
          {showForm ? 'Close form' : '+ New job'}
        </button>
      </div>

      {showForm && <CreateJobForm onDone={() => setShowForm(false)} />}

      <div className="rows" style={{ display: 'flex', gap: 8, flexWrap: 'wrap', margin: '10px 0 2px' }}>
        <span className="mono-label" style={{ alignSelf: 'center' }}>Quick demo:</span>
        <button className="btn small ghost" onClick={() => quick('ECHO', '{"message":"hi"}', 'echo')}>Echo job</button>
        <button className="btn small ghost" onClick={() => quick('WAIT', '{"duration_ms":8000}', 'wait')}>Wait job</button>
        <button className="btn small ghost" onClick={() => quick('DEMO_FAILURE', '{"fail_times":2}', 'retry-demo')}>Retry demo</button>
      </div>

      <div className="chips">
        <button className={'chip' + (status === '' ? ' active' : '')} onClick={() => setParams({})}>All</button>
        {JOB_STATUSES.map(s => (
          <button key={s} className={'chip' + (status === s ? ' active' : '')} onClick={() => setParams({ status: s })}>
            {s.replaceAll('_', ' ')}
          </button>
        ))}
      </div>

      {isLoading && <div style={{ padding: 30 }}><span className="spin" /></div>}
      {error && <p className="error">Failed to load jobs: {(error as Error).message}</p>}
      {data && (data.items.length === 0
        ? <p className="muted">No jobs here yet — create one above or use a quick demo.</p>
        : <div className="table-wrap">
            <table className="table">
              <thead>
                <tr><th>Name</th><th>Type</th><th>Status</th><th>Priority</th><th>Attempts</th><th>Next run</th><th>Created</th><th></th></tr>
              </thead>
              <tbody>
                {data.items.map((job: Job) => (
                  <tr key={job.id}>
                    <td>
                      <Link to={`/jobs/${job.id}`} style={{ fontWeight: 600 }}>{job.name}</Link>
                      {job.recurringCron && <div className="muted" style={{ fontSize: '0.75rem' }}>↻ {job.recurringCron}</div>}
                    </td>
                    <td><code>{job.jobType}</code></td>
                    <td><StatusBadge status={job.status} /></td>
                    <td>{job.priority}</td>
                    <td>{job.attempts}/{job.maxAttempts}</td>
                    <td title={dt(job.nextAttemptAt)}>{rel(job.nextAttemptAt)}</td>
                    <td title={dt(job.createdAt)}>{rel(job.createdAt)}</td>
                    <td>
                      {['SCHEDULED', 'QUEUED', 'RUNNING', 'RETRY_WAIT'].includes(job.status) && (
                        <button className="btn small ghost" disabled={cancel.isPending}
                                onClick={() => cancel.mutate(job.id)}>Cancel</button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>)}
      {data && data.totalPages > 1 && (
        <div className="row" style={{ marginTop: 14 }}>
          <button className="btn ghost small" disabled={page === 0}
                  onClick={() => setParams({ status, page: String(page - 1) })}>← Prev</button>
          <span className="muted">Page {page + 1} of {data.totalPages}</span>
          <button className="btn ghost small" disabled={page + 1 >= data.totalPages}
                  onClick={() => setParams({ status, page: String(page + 1) })}>Next →</button>
        </div>
      )}
    </>
  );
}
