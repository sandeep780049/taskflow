import { useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../api';
import { StatusBadge } from '../App';

const dt = (iso?: string) => (iso ? new Date(iso).toLocaleString() : '—');
const fmtMs = (ms?: number) => (ms == null ? '—' : ms >= 1000 ? `${(ms / 1000).toFixed(2)}s` : `${ms}ms`);

export default function JobDetail() {
  const { id = '' } = useParams();
  const navigate = useNavigate();
  const qc = useQueryClient();

  const { data, isLoading, error } = useQuery({
    queryKey: ['job', id],
    queryFn: () => api.getJob(id),
    refetchInterval: 3000,
  });

  const cancel = useMutation({
    mutationFn: () => api.cancelJob(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['job', id] }),
  });
  const del = useMutation({
    mutationFn: () => api.deleteJob(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['jobs'] }); navigate('/jobs'); },
  });
  if (isLoading) return <p className="muted">Loading…</p>;
  if (error) return <p className="error">Failed to load job: {(error as Error).message}</p>;
  if (!data) return null;
  const { job, executions, logs } = data;
  const actives = ['SCHEDULED', 'QUEUED', 'RUNNING', 'RETRY_WAIT', 'FAILED'];

  return (
    <>
      <div className="row spread">
        <h1>{job.name}</h1>
        <div className="row">
          {actives.includes(job.status) && (
            <button className="btn" onClick={() => cancel.mutate()} disabled={cancel.isPending}>
              Cancel job
            </button>
          )}
          {!actives.includes(job.status) && (
            <button className="btn danger" onClick={() => del.mutate()} disabled={del.isPending}>
              Delete
            </button>
          )}
        </div>
      </div>

      <div className="card">
        <div className="row wrap info">
          <StatusBadge status={job.status} />
          <span><b>{job.jobType}</b></span>
          <span className="muted">priority {job.priority}</span>
          <span className="muted">attempts {job.attempts}/{job.maxAttempts}</span>
          <span className="muted">timeout {job.timeoutSeconds}s</span>
          {job.recurringCron && <span>↻ cron <code>{job.recurringCron}</code>{job.timezone ? ` (${job.timezone})` : ' (UTC)'}</span>}
          {job.idempotencyKey && <span className="muted">idem: {job.idempotencyKey}</span>}
          {job.lockedBy && <span className="muted">locked by {job.lockedBy}</span>}
        </div>
        {job.description && <p>{job.description}</p>}
        {job.payload != null && (
          <pre className="code">{JSON.stringify(job.payload, null, 2)}</pre>
        )}
        <p className="muted">
          created {dt(job.createdAt)} · next attempt {dt(job.nextAttemptAt)} {job.cancelRequested && '· cancellation requested'}
        </p>
      </div>

      <h2>Execution attempts</h2>
      {executions.length === 0 ? <p className="muted">No attempts yet.</p> : (
        <table className="table">
          <thead>
            <tr><th>#</th><th>Status</th><th>Worker</th><th>Started</th><th>Duration</th><th>Result / error</th></tr>
          </thead>
          <tbody>
            {executions.map(ex => (
              <tr key={ex.id}>
                <td>{ex.attemptNumber}</td>
                <td><StatusBadge status={ex.status === 'SUCCEEDED' ? 'SUCCEEDED' : ex.status === 'RUNNING' ? 'RUNNING' : 'FAILED'} /></td>
                <td>{ex.workerId ?? '—'}</td>
                <td>{dt(ex.startedAt)}</td>
                <td>{fmtMs(ex.durationMs)}</td>
                <td className="wrap-text">
                  {ex.result != null && <pre className="code small">{JSON.stringify(ex.result)}</pre>}
                  {ex.errorCode && <span className="error">{ex.errorCode}: {ex.errorMessage}</span>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <h2>Logs</h2>
      {logs.length === 0 ? <p className="muted">No logs.</p> : (
        <div className="card loglist">
          {logs.map(l => (
            <div key={l.id} className={`log l-${l.level.toLowerCase()}`}>
              <span className="muted">{dt(l.timestamp)}</span>
              <span className={'level-' + l.level.toLowerCase()}>{l.level}</span>
              <span>{l.message}</span>
            </div>
          ))}
        </div>
      )}
    </>
  );
}
