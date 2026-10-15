import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, setToken } from '../api';

/* ---------- TF logo (inline SVG) ---------- */

const TFIcon = () => (
  <svg width="26" height="26" viewBox="0 0 32 32" fill="none">
    <rect width="32" height="32" rx="7" fill="url(#tfGrad)" />
    <text x="16" y="21" textAnchor="middle" fill="#fff" fontSize="12" fontWeight="900" fontFamily="monospace">TF</text>
    <defs>
      <linearGradient id="tfGrad" x1="0" x2="32" y1="0" y2="32" gradientUnits="userSpaceOnUse">
        <stop offset="0" stopColor="#818cf8" />
        <stop offset="1" stopColor="#6366f1" />
      </linearGradient>
    </defs>
  </svg>
);

/* ---------- terminal-style boot log lines (decorative) ---------- */

const bootLines = [
  { time: '16:44:08', text: 'INCOMING HTTP REQUEST DETECTED ...', color: '#94a3b8' },
  { time: '16:44:11', text: 'SERVICE WAKING UP ...', color: '#cbd5e1' },
  { time: '16:44:15', text: 'ALLOCATING COMPUTE RESOURCES ...', color: '#94a3b8' },
  { time: '16:44:18', text: 'PREPARING INSTANCE FOR INITIALIZATION ...', color: '#94a3b8' },
  { time: '16:44:22', text: 'STARTING THE INSTANCE ...', color: '#cbd5e1' },
  { time: '16:44:28', text: 'ENVIRONMENT VARIABLES INJECTED ...', color: '#94a3b8' },
  { time: '16:44:30', text: 'FINALIZING STARTUP ...', color: '#94a3b8' },
  { time: '16:44:32', text: 'OPTIMIZING DEPLOYMENT ...', color: '#94a3b8' },
  { time: '16:44:34', text: 'STEADY HANDS. CLEAN LOGS. YOUR APP IS ALMOST LIVE ...', color: '#818cf8' },
];

function BootLog() {
  return (
    <div className="boot-log">
      <div className="boot-log-header">
        <TFIcon />
        <span className="boot-log-title">TaskFlow</span>
      </div>
      <div className="boot-log-lines">
        {bootLines.map((l, i) => (
          <div key={i} className="boot-log-line" style={{ color: l.color }}>
            <span className="boot-log-time">{l.time}</span>
            <span className="boot-log-text">{l.text}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

export default function Login() {
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
      if (mode === 'register') {
        const res = await api.register(email, password);
        setToken(res.token);
        navigate('/dashboard', { replace: true });
      } else {
        const res = await api.login(email, password);
        setToken(res.token);
        navigate('/dashboard', { replace: true });
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Something went wrong');
    } finally {
      setBusy(false);
    }
  };

  const fillDemo = () => {
    setMode('login');
    setEmail('demo@taskflow.dev');
    setPassword('demo-pass-1234');
    setError(null);
    // keep fields visible — user can edit or submit directly
  };

  return (
    <div className="page">
      <BootLog />

      <main className="main">
        <div className="card">
          <div className="card-head">
            <h1 className="card-title">Welcome back</h1>
            <p className="card-sub">Sign in to your TaskFlow workspace</p>
          </div>

          {/* pill tabs */}
          <div className="tab-pill">
            <button
              type="button"
              className={`tab-opt ${mode === 'login' ? 'active' : ''}`}
              onClick={() => { setMode('login'); setError(null); }}
            >
              Sign in
            </button>
            <button
              type="button"
              className={`tab-opt ${mode === 'register' ? 'active' : ''}`}
              onClick={() => { setMode('register'); setError(null); }}
            >
              Register
            </button>
          </div>

          <form className="auth-form" onSubmit={submit}>
            <div className="field">
              <label className="field-label">Email</label>
              <input
                type="email"
                className="input"
                value={email}
                onChange={e => setEmail(e.target.value)}
                placeholder="you@example.com"
                autoComplete="email"
              />
            </div>

            <div className="field">
              <label className="field-label">Password</label>
              <div className="pw-wrap">
                <input
                  type={showPw ? 'text' : 'password'}
                  className="input"
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
            </div>

            {error && <div className="auth-error">{error}</div>}

            <button
              type="submit"
              className="btn primary"
              disabled={busy}
              style={{ width: '100%' }}
            >
              {busy ? (
                <span className="spin" style={{ width: 16, height: 16 }} />
              ) : (
                mode === 'login' ? 'Sign in →' : 'Create account →'
              )}
            </button>
          </form>

          <div className="form-footer">
            <button type="button" className="link-btn" onClick={() => { setMode('register'); setEmail(''); setPassword(''); setError(null); }}>
              Create an account
            </button>
            <span className="dot">·</span>
            <button type="button" className="link-btn" onClick={() => { setMode('login'); setEmail(''); setPassword(''); setError(null); }}>
              Sign in
            </button>
          </div>

          <div className="demo-box">
            <span className="demo-label">Demo account:</span>
            <button type="button" className="btn ghost small" onClick={fillDemo}>
              Use demo account
            </button>
          </div>
        </div>
      </main>
    </div>
  );
}
