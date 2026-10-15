import { useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, type Job } from '../api';
import { StatusBadge } from '../App';
import { useToast } from '../toast';

const dt = (iso?: string) => (iso ? new Date(iso).toLocaleString() : '—');
const fmtMs = (ms?: number) => (ms == null ? '—' : ms >= 1000 ? `${(ms / 1000).toFixed(2)}s` : `${ms}ms`);
const rel = (iso?: string) => {
  if (!iso) return '—';
  const d = Date.now() - new Date(iso).getTime();
  const abs = Math.abs(d) / 1000;
  if (abs < 60) return `${Math.max(1, Math.round(abs))}s ago`;
  if (abs < 3600) return `${Math.round(abs / 60)}m ago`;
  if (abs < 86400) return `${Math.round(abs / 3600)}h ago`;
  return `${Math.round(abs / 86400)}d ago`;
};

const ACTIVE = ['SCHEDULED', 'QUEUED', 'RUNNING', 'RETRY_WAIT'];

export default function JobDetail() {
  const { id = '' } = useParams();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const toast = useToast();

  const { data, isLoading, error } = useQuery({
    queryKey: ['job', id],
    queryFn: () => api.getJob(id),
    refetchInterval: 3000,
  });

  const cancel = useMutation({
    mutationFn: () => api.cancelJob(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['job', id] }); toast('Cancellation requested', 'ok'); },
    onError: (e: Error) => toast(e.message, 'err'),
  });
  const del = useMutation({
    mutationFn: () => api.deleteJob(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['jobs'] }); toast('Job deleted', 'ok'); navigate('/jobs'); },
    onError: (e: Error) => toast(e.message, 'err'),
  });

  if (isLoading) return <div style={{ padding: 40 }}><span className="spin" /></div>;
  if (error) return <p className="error">Failed to load job: {(error as Error).message}</p>;
  if (!data) return null;
  const { job, executions, logs } = data;
  const j: Job = job;

  const copy = (text: string, label: string) => {
    navigator.clipboard.writeText(text).then(() => toast(`${label} copied`, 'ok'));
  };
  const rerun = () => {
    api.createJob({
      name: `${j.name}-rerun-${Date.now() % 10000}`,
      description: j.description, jobType: j.jobType, payload: j.payload ?? undefined,
      priority: j.priority, recurringCron: j.recurringCron, timezone: j.timezone,
      maxAttempts: j.maxAttempts, timeoutSeconds: j.timeoutSeconds,
    }).then(() => {
      qc.invalidateQueries({ queryKey: ['jobs'] });
      toast('Cloned as a new job and queued', 'ok');
      navigate('/jobs');
    }).catch((e: Error) => toast(e.message, 'err'));
  };

  return (
    <>
      <div className="row spread" style={{ marginBottom: 4 }}>
        <div>
          <h1 style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            {j.name} <StatusBadge status={j.status} />
            {j.recurringCron && <span className="badge" style={{ background: '#8b5cf6' }}>RECURRING ↻</span>}
          </h1>
          {j.description && <p className="muted" style={{ margin: 0 }}>{j.description}</p>}
        </div>
        <div className="row">
          <button className="btn small" onClick={rerun}>⭮ Run again</button>
          {ACTIVE.includes(j.status) ? (
            <button className="btn small danger" onClick={() => cancel.mutate()} disabled={cancel.isPending}>
              Cancel job
            </button>
          ) : (
            <button className="btn small danger" onClick={() => del.mutate()} disabled={del.isPending}>Delete</button>
          )}
        </div>
      </div>

      <div className="card">
        <div className="detail-grid">
          <div className="detail-cell"><span className="mono-label">Type</span><code>{j.jobType}</code></div>
          <div className="detail-cell"><span className="mono-label">Priority</span>{j.priority} / 9</div>
          <div className="detail-cell"><span className="mono-label">Attempts</span>{j.attempts} / {j.maxAttempts}</div>
          <div className="detail-cell"><span className="mono-label">Timeout</span>{j.timeoutSeconds}s</div>
          <div className="detail-cell"><span className="mono-label">Next attempt</span>{rel(j.nextAttemptAt)} <span className="muted">({dt(j.nextAttemptAt)})</span></div>
          <div className="detail-cell"><span className="mono-label">Created</span>{rel(j.createdAt)} <span className="muted">({dt(j.createdAt)})</span></div>
          {j.recurringCron && (
            <div className="detail-cell">
              <span className="mono-label">Cron</span>
              <code>{j.recurringCron}</code>{j.timezone ? <span className="muted"> {j.timezone}</span> : <span className="muted"> UTC</span>}
            </div>
          )}
          {j.idempotencyKey && <div className="detail-cell"><span className="mono-label">Idempotency</span><code>{j.idempotencyKey}</code></div>}
          {j.lockedBy && <div className="detail-cell"><span className="mono-label">Locked by</span>{j.lockedBy}</div>}
          {j.cancelRequested && <div className="detail-cell"><span className="mono-label">Cancellation</span><span className="error">requested — stopping…</span></div>}
        </div>
        {j.payload != null && (
          <div style={{ marginTop: 14 }}>
            <div className="row spread" style={{ marginBottom: 6 }}>
              <span className="mono-label">Payload</span>
              <button className="btn small ghost" onClick={() => copy(JSON.stringify(j.payload, null, 2), 'Payload')}>Copy</button>
            </div>
            <pre className="code">{JSON.stringify(j.payload, null, 2)}</pre>
          </div>
        )}
      </div>

      <h2>Execution attempts <span className="muted">({executions.length})</span></h2>
      {executions.length === 0 ? <p className="muted">No attempts yet — the worker will pick this up shortly.</p> : (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr><th>#</th><th>Status</th><th>Worker</th><th>Started</th><th>Duration</th><th>Result / error</th></tr>
            </thead>
            <tbody>
              {executions.map(ex => (
                <tr key={ex.id}>
                  <td><b>{ex.attemptNumber}</b></td>
                  <td><StatusBadge status={ex.status === 'SUCCEEDED' ? 'SUCCEEDED' : ex.status === 'RUNNING' ? 'RUNNING' : 'FAILED'} /></td>
                  <td className="muted">{ex.workerId ?? '—'}</td>
                  <td title={dt(ex.startedAt)}>{rel(ex.startedAt)}</td>
                  <td>{fmtMs(ex.durationMs)}</td>
                  <td className="wrap-text">
                    {ex.result != null && (
                      <div>
                        <div className="row" style={{ marginBottom: 4 }}>
                          <button className="btn small ghost" onClick={() => copy(JSON.stringify(ex.result), 'Result')}>Copy</button>
                        </div>
                        <pre className="code small">{JSON.stringify(ex.result)}</pre>
                      </div>
                    )}
                    {ex.errorCode && <span className="error">{ex.errorCode}: {ex.errorMessage}</span>}
                    {ex.result == null && !ex.errorCode && ex.status === 'RUNNING' && <span className="muted">running…</span>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <h2>Logs <span className="muted">({logs.length})</span></h2>
      {logs.length === 0 ? <p className="muted">No logs.</p> : (
        <div className="card loglist">
          {logs.map(l => (
            <div key={l.id} className={`log l-${l.level.toLowerCase()}`}>
              <span className="ts">{new Date(l.timestamp ?? '').toLocaleTimeString()}</span>
              <span className={'level-' + l.level.toLowerCase()}>{l.level}</span>
              <span>{l.message}</span>
            </div>
          ))}
        </div>
      )}
    </>
  );
}
