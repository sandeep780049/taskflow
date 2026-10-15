import { useState, type FormEvent } from 'react';
import { useNavigate, NavLink } from 'react-router-dom';
import { useAuth } from '../auth';

const FEATURES = [
  { icon: '⚡', title: 'Instant & scheduled jobs', text: 'Run now, at a future time, or on recurring cron schedules.' },
  { icon: '🔒', title: 'Distributed & safe', text: 'Postgres SKIP LOCKED claiming with leases — no double runs.' },
  { icon: '🔁', title: 'Retries & dead-letter', text: 'Exponential backoff, permanent-failure quarantine, crash recovery.' },
  { icon: '📊', title: 'Full observability', text: 'Per-attempt records, live logs, worker health, charts.' },
];

export default function Login() {
  const { login, register } = useAuth();
  const navigate = useNavigate();
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPw, setShowPw] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      if (mode === 'login') await login(email, password);
      else await register(email, password);
      navigate('/dashboard', { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Something went wrong');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="login-page">
      {/* Top Navigation */}
      <header className="login-nav">
        <div className="nav-container">
          <NavLink to="/dashboard" className="nav-brand">
            <span className="brand-mark">TF</span> TaskFlow
          </NavLink>
          <div className="nav-links">
            <a href="/dashboard">Dashboard</a>
            <a href="/jobs">Jobs</a>
            <a href="/workers">Workers</a>
          </div>
          <div className="nav-actions">
            <button className="btn ghost" onClick={() => navigate('/login')}>Sign In</button>
            <button className="btn primary" onClick={() => navigate('/register')}>
              Get Started
            </button>
          </div>
        </div>
      </header>

      {/* Main Hero + Form Section */}
      <div className="login-hero">
        <div className="login-container">
          <div className="hero-content">
            <span className="hero-badge">⚡ Production-minded job scheduler</span>
            <h1>
              TaskFlow — <span className="gradient-text">Distributed Job Scheduler</span>
            </h1>
            <p className="hero-subtitle">
              A full-stack platform to <b>create, schedule, execute, monitor, retry, cancel and audit</b> background jobs.
            </p>

            <div className="feature-grid">
              {FEATURES.map(f => (
                <div key={f.title} className="feature-item">
                  <span className="feature-icon">{f.icon}</span>
                  <div>
                    <b>{f.title}</b>
                    <p>{f.text}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Login Form */}
          <div className="login-form-card">
            <h1>{mode === 'login' ? 'Welcome back' : 'Create your account'}</h1>
            <p className="form-subtitle">
              {mode === 'login' ? 'Sign in to your TaskFlow workspace' : 'Free — just an email and a password'}
            </p>
            <form className="auth-form" onSubmit={submit}>
              <div className="form-group">
                <label htmlFor="email">Email</label>
                <input
                  type="email"
                  id="email"
                  required
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                  placeholder="you@example.com"
                  autoComplete="email"
                />
              </div>
              <label className="password-field">
                Password
                <div className="pw-wrap">
                  <input
                    type={showPw ? 'text' : 'password'}
                    required
                    minLength={8}
                    value={password}
                    onChange={e => setPassword(e.target.value)}
                    placeholder="min. 8 characters"
                    autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                  />
                  <button
                    type="button"
                    className="pw-toggle"
                    onClick={() => setShowPw(s => !s)}
                  >
                    {showPw ? 'Hide' : 'Show'}
                  </button>
                </div>
              </label>
              {error && <p className="error" style={{ marginTop: 6 }}>{error}</p>}
              <button
                className="btn primary"
                disabled={busy}
                style={{ width: '100%', marginTop: 12 }}
                type="submit"
              >
                {busy ? <span className="spin" style={{ width: 16, height: 16, borderWidth: 2 }} /> :
                  mode === 'login' ? 'Sign in →' : 'Create account →'}
              </button>
            </form>
            <div className="form-footer">
              <button type="button" className="tab-link" onClick={() => setMode('register')}>
                Create an account
              </button>
              <span className="divider">·</span>
              <button type="button" className="tab-link" onClick={() => setMode('login')}>
                Sign in
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Bottom Navigation */}
      <div className="login-nav bottom-nav">
        <div className="nav-container">
          <div className="nav-links" style={{ justifyContent: 'center', gap: '12px' }}>
            <a href="#" className="nav-link">
              <span className="nav-icon">🏠</span> Home
            </a>
            <a href="#" className="nav-link">
              <span className="nav-icon">🏆</span> Contests
            </a>
            <a href="#" className="nav-link">
              <span className="nav-icon">🔥</span> Daily
            </a>
            <a href="#" className="nav-link">
              <span className="nav-icon">🎯</span> Practice
            </a>
            <a href="#" className="nav-link">
              <span className="nav-icon">&lt;/&gt;</span> Leaderboard
            </a>
          </div>
          <div className="nav-actions-bottom">
            <button className="btn ghost" style={{ borderColor: 'var(--border)', color: 'var(--muted)' }}>
              <span className="nav-icon">☀️</span>
            </button>
            <button className="btn primary" onClick={() => navigate('/login')}>
              Get Started
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
