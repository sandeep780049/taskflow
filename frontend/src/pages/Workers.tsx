import { useQuery } from '@tanstack/react-query';
import { api } from '../api';
import { StatusBadge } from '../App';

const dt = (iso?: string) => (iso ? new Date(iso).toLocaleString() : '—');
const rel = (iso?: string) => {
  if (!iso) return '—';
  const s = (Date.now() - new Date(iso).getTime()) / 1000;
  if (s < 60) return `${Math.max(1, Math.round(s))}s ago`;
  if (s < 3600) return `${Math.round(s / 60)}m ago`;
  return `${Math.round(s / 3600)}h ago`;
};

export default function Workers() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['workers'],
    queryFn: api.workers,
    refetchInterval: 10000,
  });

  const workers = data ?? [];
  const active = workers.filter(w => w.status === 'ACTIVE').length;

  return (
    <>
      <div className="row spread">
        <h1>Workers</h1>
        <span className="muted">auto-refreshes every 10s</span>
      </div>
      <p className="muted" style={{ marginTop: 0 }}>
        Each instance registers itself, heartbeats every 15s and claims jobs with
        <code style={{ margin: '0 4px' }}>FOR UPDATE SKIP LOCKED</code>.
        Leases expire after 60s; the reaper requeues orphaned jobs automatically.
      </p>

      {isLoading && <div style={{ padding: 30 }}><span className="spin" /></div>}
      {error && <p className="error">Failed to load workers: {(error as Error).message} (admin role required)</p>}

      {workers.length > 0 && (
        <div className="cards" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(210px, 1fr))' }}>
          <div className="card metric">
            <span className="mono-label">Fleet</span>
            <div className="metric-number">{workers.length}</div>
          </div>
          <div className="card metric">
            <span className="mono-label">Active</span>
            <div className="metric-number" style={{ color: 'var(--green)' }}>{active}</div>
          </div>
          <div className="card metric">
            <span className="mono-label">Stale / stopped</span>
            <div className="metric-number" style={{ color: workers.length - active > 0 ? 'var(--amber)' : 'var(--muted)' }}>
              {workers.length - active}
            </div>
          </div>
        </div>
      )}

      {workers.length > 0 && (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr><th>Name</th><th>Status</th><th>Last heartbeat</th><th>Registered</th></tr>
            </thead>
            <tbody>
              {workers.map(w => (
                <tr key={w.name}>
                  <td style={{ fontWeight: 600 }}>{w.name}</td>
                  <td><StatusBadge status={w.status === 'ACTIVE' ? 'SUCCEEDED' : w.status === 'STALE' ? 'RETRY_WAIT' : 'CANCELLED'} /></td>
                  <td title={dt(w.lastHeartbeatAt)}>{rel(w.lastHeartbeatAt)}</td>
                  <td title={dt(w.registeredAt)}>{rel(w.registeredAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
