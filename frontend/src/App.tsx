import { Navigate, NavLink, Route, Routes, useNavigate } from 'react-router-dom';
import { useAuth } from './auth';
import Login from './pages/Login';
import Landing from './pages/Landing';
import Dashboard from './pages/Dashboard';
import Jobs from './pages/Jobs';
import JobDetail from './pages/JobDetail';
import Workers from './pages/Workers';
import { ToastProvider } from './toast';

export const STATUS_COLORS: Record<string, string> = {
  SCHEDULED: '#3b82f6',
  QUEUED: '#8b5cf6',
  RUNNING: '#f59e0b',
  SUCCEEDED: '#22c55e',
  FAILED: '#ef4444',
  RETRY_WAIT: '#f97316',
  CANCELLED: '#64748b',
  DEAD_LETTER: '#dc2626',
};

export function StatusBadge({ status }: { status: string }) {
  return (
    <span className="badge" style={{ background: STATUS_COLORS[status] ?? '#64748b' }}>
      {status.replaceAll('_', ' ')}
    </span>
  );
}

const initials = (email?: string) => (email ? email.slice(0, 2).toUpperCase() : '··');

function Layout({ children }: { children: React.ReactNode }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  return (
    <div className="layout">
      <header className="topbar">
        <NavLink to="/dashboard" className="brand">
          <span className="brand-mark">TF</span> TaskFlow
        </NavLink>
        <nav>
          <NavLink to="/dashboard">Dashboard</NavLink>
          <NavLink to="/jobs">Jobs</NavLink>
          <NavLink to="/workers">Workers</NavLink>
        </nav>
        <div className="spacer" />
        <div className="userchip">
          <span className="avatar">{initials(user?.email)}</span>
          <span style={{ fontSize: '0.84rem' }}>
            {user?.email}
            {user?.role === 'ADMIN' && <span className="badge" style={{ background: '#6366f1', marginLeft: 8 }}>ADMIN</span>}
          </span>
        </div>
        <button className="btn ghost small" onClick={() => { logout(); navigate('/login'); }}>Log out</button>
      </header>
      <main className="content">{children}</main>
      <footer className="footer">
        TaskFlow — distributed job scheduler · Spring Boot + PostgreSQL + React · UTC backend, local-time display
      </footer>
    </div>
  );
}

export default function App() {
  const { user, loading } = useAuth();

  if (loading) {
    return <div className="center-page"><span className="spin" /></div>;
  }
  if (!user) {
    return (
      <Routes>
        <Route path="/" element={<Landing />} />
        <Route path="/login" element={<Login />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    );
  }
  return (
    <ToastProvider>
      <Layout>
        <Routes>
          <Route path="/login" element={<Navigate to="/dashboard" replace />} />
          <Route path="/" element={<Landing />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/jobs" element={<Jobs />} />
          <Route path="/jobs/:id" element={<JobDetail />} />
          <Route path="/workers" element={<Workers />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </Layout>
    </ToastProvider>
  );
}
