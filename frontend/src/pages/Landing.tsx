import { Link } from 'react-router-dom';
import DashboardPreview from './DashboardPreview';
import '../landing.css';

/* ---------- section-level building blocks ---------- */

function Nav() {
  return (
    <header className="land-nav">
      <div className="land-nav-inner">
        <Link to="/" className="land-brand">
          <span className="land-brand-mark">TF</span>
          TaskFlow
        </Link>
        <nav className="land-nav-links" aria-label="Main">
          <a href="#features">Features</a>
          <a href="#how-it-works">How It Works</a>
          <a href="#faq">FAQ</a>
        </nav>
        <div className="land-nav-actions">
          <Link to="/login" className="land-btn land-btn-ghost">Sign In</Link>
          <Link to="/login?mode=register" className="land-btn land-btn-primary">Get Started</Link>
        </div>
      </div>
    </header>
  );
}

function Hero() {
  return (
    <section className="land-hero">
      <div className="land-hero-copy">
        <span className="land-eyebrow">Organize &bull; Plan &bull; Achieve</span>
        <h1>
          Get More Done with <span className="land-gradient-text">TaskFlow</span>
        </h1>
        <p className="land-hero-sub">
          A modern and powerful job scheduling platform to organize your tasks,
          coordinate a fleet of workers, and turn your ideas into reality.
        </p>
        <div className="land-hero-cta">
          <Link to="/login?mode=register" className="land-btn land-btn-primary land-btn-lg">
            Get Started &rarr;
          </Link>
          <Link to="/login" className="land-btn land-btn-ghost land-btn-lg">Sign In</Link>
        </div>
        <dl className="land-hero-stats">
          <div>
            <dt>8</dt>
            <dd>job lifecycle states, tracked end to end</dd>
          </div>
          <div>
            <dt>24/7</dt>
            <dd>recurring cron &amp; one-shot scheduling, timezone-aware</dd>
          </div>
          <div>
            <dt>100%</dt>
            <dd>inspected executions, logs &amp; retries</dd>
          </div>
        </dl>
      </div>
      <div className="land-hero-preview">
        <DashboardPreview />
      </div>
    </section>
  );
}

const features = [
  {
    icon: '▦',
    title: 'Flexible Job Scheduling',
    desc: 'Schedule one-shot jobs at an exact time, or recurring jobs with cron expressions and per-job timezones.',
  },
  {
    icon: '⟳',
    title: 'Resilient Execution',
    desc: 'Configurable retries and attempt tracking, hard timeouts, and dead-letter handling for failing jobs.',
  },
  {
    icon: '◫',
    title: 'Real-time Analytics',
    desc: 'Live metrics on job status distribution, 24h success rate, hourly throughput and average duration.',
  },
  {
    icon: '🛡',
    title: 'Secure by Design',
    desc: 'JWT-protected APIs with role-aware access, validated registration, and token-based session management.',
  },
];

function Features() {
  return (
    <section className="land-section" id="features">
      <h2 className="land-h2">Everything you need to <span className="land-gradient-text">ship on time</span></h2>
      <p className="land-section-sub">Built as a real, working product — every feature below is live in the app today.</p>
      <div className="land-cards">
        {features.map(f => (
          <article className="land-card" key={f.title}>
            <span className="land-card-icon" aria-hidden="true">{f.icon}</span>
            <h3>{f.title}</h3>
            <p>{f.desc}</p>
          </article>
        ))}
      </div>
    </section>
  );
}

const steps = [
  {
    n: '01',
    title: 'Organize Your Work',
    desc: 'Create jobs from the dashboard or the API — set a priority, a schedule and an idempotency key.',
  },
  {
    n: '02',
    title: 'Track Your Progress',
    desc: 'Workers lease the work and report heartbeats. Watch attempts, durations and retries live.',
  },
  {
    n: '03',
    title: 'Achieve Your Goals',
    desc: 'Inspect every execution and log, and let analytics tell you how the fleet is performing.',
  },
];

function HowItWorks() {
  return (
    <section className="land-section" id="how-it-works">
      <h2 className="land-h2">How it <span className="land-gradient-text">works</span></h2>
      <p className="land-section-sub">Three steps from idea to insight.</p>
      <ol className="land-steps">
        {steps.map(s => (
          <li className="land-step" key={s.n}>
            <span className="land-step-num">{s.n}</span>
            <h3>{s.title}</h3>
            <p>{s.desc}</p>
          </li>
        ))}
      </ol>
    </section>
  );
}

const faqs = [
  {
    q: 'What is TaskFlow?',
    a: 'TaskFlow is a distributed job scheduler: you define jobs, and a pool of registered workers executes them on schedule with retries, timeouts and full execution history.',
  },
  {
    q: 'How can I try it?',
    a: 'The login page includes a demo account button, or you can create your own account in seconds — no credit card, no setup.',
  },
  {
    q: 'What powers it under the hood?',
    a: 'A Spring Boot backend backed by PostgreSQL, with job leasing, heartbeat tracking and UTC as the canonical time base. The frontend is React.',
  },
  {
    q: 'Can jobs run on a recurring schedule?',
    a: 'Yes — recurring jobs use standard cron expressions and honor a per-job timezone, while scheduled jobs fire once at an exact UTC time.',
  },
];

function Faq() {
  return (
    <section className="land-section" id="faq">
      <h2 className="land-h2">Frequently asked <span className="land-gradient-text">questions</span></h2>
      <div className="land-faq">
        {faqs.map(f => (
          <details key={f.q} className="land-faq-item">
            <summary>{f.q}</summary>
            <p>{f.a}</p>
          </details>
        ))}
      </div>
    </section>
  );
}

function FinalCta() {
  return (
    <section className="land-section">
      <div className="land-cta-panel">
        <h2>Ready to keep your jobs on schedule?</h2>
        <p>Create a free account and take the demo fleet for a spin.</p>
        <div className="land-hero-cta" style={{ justifyContent: 'center' }}>
          <Link to="/login?mode=register" className="land-btn land-btn-primary land-btn-lg">Get Started &rarr;</Link>
          <Link to="/login" className="land-btn land-btn-ghost land-btn-lg">Try the demo</Link>
        </div>
      </div>
    </section>
  );
}

const year = new Date().getFullYear();

function Footer() {
  return (
    <footer className="land-footer">
      <div className="land-footer-inner">
        <div className="land-footer-brand">
          <Link to="/" className="land-brand">
            <span className="land-brand-mark">TF</span>
            TaskFlow
          </Link>
          <p>Distributed job scheduler &middot; Spring Boot + PostgreSQL + React &middot; UTC backend, local-time display</p>
        </div>
        <div className="land-footer-col">
          <h4>Product</h4>
          <a href="#features">Features</a>
          <a href="#how-it-works">How It Works</a>
          <a href="#faq">FAQ</a>
        </div>
        <div className="land-footer-col">
          <h4>Account</h4>
          <Link to="/login">Sign In</Link>
          <Link to="/login?mode=register">Create Account</Link>
        </div>
      </div>
      <div className="land-footer-bottom">© {year} TaskFlow. All rights reserved.</div>
    </footer>
  );
}

export default function Landing() {
  return (
    <div className="land-page">
      <Nav />
      <main>
        <Hero />
        <Features />
        <HowItWorks />
        <Faq />
        <FinalCta />
      </main>
      <Footer />
    </div>
  );
}
