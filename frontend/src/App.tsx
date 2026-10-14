import { Navigate, NavLink, Route, Routes, useNavigate } from 'react-router-dom';
import { useAuth } from './auth';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Jobs from './pages/Jobs';
import JobDetail from './pages/JobDetail';
import Workers from './pages/Workers';
import { JOB_STATUSES, type Job } from './api';

export const STATUS_COLORS: Record<string, string> = {
  SCHEDULED: 'var(--blue)',
  QUEUED: 'var(--violet)',
  RUNNING: 'var(--amber)',
  SUCCEEDED: 'var(--green)',
  FAILED: 'var(--red)',
  RETRY_WAIT: 'var(--amber)',
  CANCELLED: 'var(--muted)',
  DEAD_LETTER: 'var(--red)',
};

export function StatusBadge({ status }: { status: string }) {
  return (
    <span className="badge" style={{ background: STATUS_COLORS[status] ?? 'var(--muted)' }}>
      {status.replaceAll('_', ' ')}
    </span>
  );
}

const emptyStatuses: Record<string, number> = Object.fromEntries(JOB_STATUSES.map(s => [s, 0]));
export const statusCounts = (by: Record<string, number> | undefined): Record<string, number> =>
  Object.assign({}, emptyStatuses, by ?? {});

function Layout({ children }: { children: React.ReactNode }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  return (
    <div className="layout">
      <header className="topbar">
        <NavLink to="/dashboard" className="brand">TaskFlow</NavLink>
        <nav>
          <NavLink to="/dashboard">Dashboard</NavLink>
          <NavLink to="/jobs">Jobs</NavLink>
          <NavLink to="/workers">Workers</NavLink>
        </nav>
        <div className="spacer" />
        <span className="muted">{user?.email}{user?.role === 'ADMIN' ? ' · admin' : ''}</span>
        <button className="btn ghost" onClick={() => { logout(); navigate('/login'); }}>Log out</button>
      </header>
      <main className="content">{children}</main>
      <footer className="footer">TaskFlow — distributed job scheduler · UTC backend · local-time order</footer>
    </div>
  );
}

export function statusBadgeList(job: Job) {
  return <StatusBadge status={job.status} />;
}

export default function App() {
  const { user, loading } = useAuth();

  if (loading) return <div className="center-page"><p>Loading…</p></div>;
  if (!user) {
    return (
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    );
  }
  return (
    <Layout>
      <Routes>
        <Route path="/login" element={<Navigate to="/dashboard" replace />} />
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/jobs" element={<Jobs />} />
        <Route path="/jobs/:id" element={<JobDetail />} />
        <Route path="/workers" element={<Workers />} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </Layout>
  );
}
