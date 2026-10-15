import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
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
    <div className="center-page">
      <div className="login-shell">
        <aside className="login-hero">
          <div className="brand" style={{ fontSize: '1.25rem' }}>
            <span className="brand-mark">TF</span> TaskFlow
          </div>
          <p className="lead">
            A production-minded <b style={{ color: 'var(--text)' }}>distributed job scheduler</b> —
            schedule, execute, retry and monitor background jobs at scale.
          </p>
          <div className="hero-list">
            {FEATURES.map(f => (
              <div className="hero-item" key={f.title}>
                <span className="hero-dot">{f.icon}</span>
                <span><b>{f.title}</b>{f.text}</span>
              </div>
            ))}
          </div>
          <p className="muted" style={{ marginTop: 'auto', fontSize: '0.75rem' }}>
            Spring Boot · PostgreSQL · React — SKIP LOCKED, leases &amp; backoff
          </p>
        </aside>

        <main className="login-panel">
          <h1>{mode === 'login' ? 'Welcome back' : 'Create your account'}</h1>
          <p className="sub">
            {mode === 'login' ? 'Sign in to your TaskFlow workspace' : 'Free — just an email and a password'}
          </p>
          <form className="auth-card" onSubmit={submit}>
            <div className="tabs">
              <button type="button" className={mode === 'login' ? 'tab active' : 'tab'} onClick={() => setMode('login')}>Sign in</button>
              <button type="button" className={mode === 'register' ? 'tab active' : 'tab'} onClick={() => setMode('register')}>Register</button>
            </div>
            <label>Email
              <input type="email" required value={email} onChange={e => setEmail(e.target.value)} placeholder="you@example.com" autoComplete="email" />
            </label>
            <label>Password
              <span className="pw-wrap">
                <input type={showPw ? 'text' : 'password'} required minLength={8} value={password}
                       onChange={e => setPassword(e.target.value)} placeholder="min. 8 characters"
                       autoComplete={mode === 'login' ? 'current-password' : 'new-password'} />
                <button type="button" className="pw-toggle" onClick={() => setShowPw(s => !s)}>
                  {showPw ? 'Hide' : 'Show'}
                </button>
              </span>
            </label>
            {error && <p className="error" style={{ marginTop: 6 }}>{error}</p>}
            <button className="btn primary" disabled={busy} style={{ width: '100%', marginTop: 12 }}>
              {busy ? <span className="spin" style={{ width: 16, height: 16, borderWidth: 2 }} /> :
                mode === 'login' ? 'Sign in →' : 'Create account →'}
            </button>
          </form>
        </main>
      </div>
    </div>
  );
}
