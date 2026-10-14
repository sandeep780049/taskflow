import { useQuery } from '@tanstack/react-query';
import { api } from '../api';
import { StatusBadge } from '../App';

const dt = (iso?: string) => (iso ? new Date(iso).toLocaleString() : '—');

export default function Workers() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['workers'],
    queryFn: api.workers,
    refetchInterval: 10000,
  });

  return (
    <>
      <h1>Workers</h1>
      <p className="muted">Worker nodes register themselves, heartbeat every few seconds and claim jobs from PostgreSQL with <code>FOR UPDATE SKIP LOCKED</code>. Leases expire after 60s; the reaper recovers orphaned jobs.</p>
      {isLoading && <p className="muted">Loading…</p>}
      {error && <p className="error">Failed to load workers: {(error as Error).message} (admin role required)</p>}
      {data && (data.length === 0 ? <p className="muted">No workers registered.</p> : (
        <table className="table">
          <thead>
            <tr><th>Name</th><th>Status</th><th>Last heartbeat</th><th>Registered</th></tr>
          </thead>
          <tbody>
            {data.map(worker => (
              <tr key={worker.name}>
                <td>{worker.name}</td>
                <td><StatusBadge status={worker.status === 'ACTIVE' ? 'SUCCEEDED' : 'CANCELLED'} /></td>
                <td>{dt(worker.lastHeartbeatAt)}</td>
                <td>{dt(worker.registeredAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      ))}
    </>
  );
}
