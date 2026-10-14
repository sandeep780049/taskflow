import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { api } from '../api';
import { statusCounts } from '../App';
import { StatusBadge } from '../App';

const fmt = (ms: number) => (ms >= 1000 ? `${(ms / 1000).toFixed(1)}s` : `${Math.round(ms)}ms`);
const localTime = (iso: string) => new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

export default function Dashboard() {
  const { data, isLoading, error } = useQuery({ queryKey: ['metrics'], queryFn: api.metrics, refetchInterval: 10000 });

  if (isLoading) return <p className="muted">Loading metrics…</p>;
  if (error || !data) return <p className="error">Failed to load metrics: {(error as Error)?.message}</p>;

  const counts = statusCounts(data.jobsByStatus);
  const chartData = data.hourly.map(h => ({
    time: localTime(h.hour + (h.hour.endsWith('Z') ? '' : 'Z')),
    total: h.total,
    succeeded: h.succeeded,
  }));

  return (
    <>
      <h1>Dashboard</h1>
      <div className="cards">
        {Object.entries(counts).map(([status, n]) => (
          <Link key={status} to={`/jobs?status=${status}`} className="card metric">
            <StatusBadge status={status} />
            <div className="metric-number">{n}</div>
          </Link>
        ))}
      </div>
      <div className="cards">
        <div className="card metric">
          <span className="muted">24h success rate</span>
          <div className="metric-number">{data.successRate24h.toFixed(1)}%</div>
        </div>
        <div className="card metric">
          <span className="muted">Avg duration (attempts)</span>
          <div className="metric-number">{fmt(data.avgDurationMs24h)}</div>
        </div>
        <div className="card metric">
          <span className="muted">Workers</span>
          <div className="metric-number">{data.activeWorkers} active</div>
          {data.staleWorkers > 0 && <div className="error">+{data.staleWorkers} stale</div>}
        </div>
      </div>

      <div className="card">
        <h2>Executions · last 24h</h2>
        {chartData.length === 0 ? (
          <p className="muted">No executions in the last 24 hours. Create a job to get started.</p>
        ) : (
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={chartData}>
              <XAxis dataKey="time" tick={{ fontSize: 11 }} />
              <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
              <Tooltip />
              <Legend />
              <Bar dataKey="total" name="attempts" fill="#6366f1" />
              <Bar dataKey="succeeded" name="succeeded" fill="#22c55e" />
            </BarChart>
          </ResponsiveContainer>
        )}
      </div>
    </>
  );
}
