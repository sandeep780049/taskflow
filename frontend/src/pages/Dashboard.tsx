import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Legend, CartesianGrid } from 'recharts';
import { api, JOB_STATUSES } from '../api';
import { StatusBadge } from '../App';

const fmt = (ms: number) => (ms >= 1000 ? `${(ms / 1000).toFixed(1)}s` : `${Math.round(ms)}ms`);
const localTime = (iso: string) => new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

const emptyCounts = Object.fromEntries(JOB_STATUSES.map(s => [s, 0]));

const chartTooltip = {
  contentStyle: {
    background: '#141927', border: '1px solid #232a3d', borderRadius: 10,
    fontSize: 12.5, color: '#e8ebf4',
  } as const,
};

export default function Dashboard() {
  const { data, isLoading, error } = useQuery({ queryKey: ['metrics'], queryFn: api.metrics, refetchInterval: 10000 });

  if (isLoading) return <div style={{ padding: 40 }}><span className="spin" /></div>;
  if (error || !data) return <p className="error">Failed to load metrics: {(error as Error)?.message}</p>;

  const counts: Record<string, number> = { ...emptyCounts };
  Object.entries(data.jobsByStatus ?? {}).forEach(([k, v]) => { if (k in counts) counts[k] = v; });
  const chartData = data.hourly.map(h => ({
    time: localTime(h.hour + (h.hour.endsWith('Z') || h.hour.includes('+') ? '' : 'Z')),
    total: h.total,
    succeeded: h.succeeded,
  }));

  return (
    <>
      <div className="row spread">
        <h1>Dashboard</h1>
        <span className="muted">auto-refreshes every 10s</span>
      </div>
      <div className="cards">
        {JOB_STATUSES.map(status => (
          <Link key={status} to={`/jobs?status=${status}`} className="card metric">
            <StatusBadge status={status} />
            <div className="metric-number">{counts[status]}</div>
          </Link>
        ))}
      </div>
      <div className="cards" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(210px, 1fr))' }}>
        <div className="card metric">
          <span className="mono-label">24h success rate</span>
          <div className="metric-number" style={{ color: data.successRate24h >= 90 ? 'var(--green)' : data.successRate24h >= 50 ? 'var(--amber)' : 'var(--red)' }}>
            {data.successRate24h.toFixed(1)}%
          </div>
        </div>
        <div className="card metric">
          <span className="mono-label">Avg attempt duration</span>
          <div className="metric-number">{fmt(data.avgDurationMs24h)}</div>
        </div>
        <div className="card metric">
          <span className="mono-label">Worker fleet</span>
          <div className="metric-number" style={{ color: 'var(--green)' }}>{data.activeWorkers} <span style={{ fontSize: '0.85rem', color: 'var(--muted)' }}>active</span></div>
          {data.staleWorkers > 0 && <div className="error" style={{ fontSize: '0.8rem' }}>+{data.staleWorkers} stale</div>}
        </div>
      </div>

      <div className="card">
        <h2 style={{ marginTop: 0 }}>Executions · last 24h</h2>
        {chartData.length === 0 ? (
          <p className="muted">No executions in the last 24 hours. Create a job to get started →</p>
        ) : (
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={chartData} margin={{ top: 6, right: 6, left: -18, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#1e2537" vertical={false} />
              <XAxis dataKey="time" tick={{ fontSize: 11, fill: '#8a93ab' }} stroke="#232a3d" />
              <YAxis allowDecimals={false} tick={{ fontSize: 11, fill: '#8a93ab' }} stroke="#232a3d" />
              <Tooltip {...chartTooltip} cursor={{ fill: 'rgba(99,102,241,0.08)' }} />
              <Legend wrapperStyle={{ fontSize: 12 }} />
              <Bar dataKey="total" name="attempts" fill="#6366f1" radius={[4, 4, 0, 0]} maxBarSize={26} />
              <Bar dataKey="succeeded" name="succeeded" fill="#22c55e" radius={[4, 4, 0, 0]} maxBarSize={26} />
            </BarChart>
          </ResponsiveContainer>
        )}
      </div>
    </>
  );
}
