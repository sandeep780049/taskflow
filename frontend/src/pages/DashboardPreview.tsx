/* Static, presentational preview of the TaskFlow dashboard for the landing page.
   Fully isolated from the real application: no API calls, no routing, no live data.
   All content inside is illustrative sample data. */

const statusColors: Record<string, string> = {
  RUNNING: '#f59e0b',
  SUCCEEDED: '#22c55e',
  RETRY_WAIT: '#f97316',
  SCHEDULED: '#3b82f6',
};

const previewJobs = [
  { name: 'nightly-report', status: 'RUNNING', priority: 5, due: '02:00 UTC' },
  { name: 'sync-inventory', status: 'SUCCEEDED', priority: 3, due: '03:15 UTC' },
  { name: 'webhook-dispatch', status: 'RETRY_WAIT', priority: 8, due: '04:40 UTC' },
  { name: 'db-cleanup', status: 'SCHEDULED', priority: 1, due: '05:00 UTC' },
  { name: 'email-digest', status: 'SUCCEEDED', priority: 2, due: '06:30 UTC' },
];

const activityBars = [34, 52, 40, 68, 47, 72, 58, 81, 63, 90, 77, 85];

const projects = [
  { name: 'Batch pipeline', pct: 82 },
  { name: 'API migrations', pct: 47 },
  { name: 'Nightly syncs', pct: 64 },
];

interface Props {
  includeAdminBadge?: boolean;
}

export default function DashboardPreview({ includeAdminBadge = true }: Props) {
  return (
    <div className="land-preview" role="img" aria-label="Illustrated preview of the TaskFlow dashboard interface">
      <div className="land-preview-shell">
        {/* sidebar */}
        <aside className="land-preview-side">
          <div className="land-preview-brand">
            <span className="land-preview-mark">TF</span>
            <span>TaskFlow</span>
          </div>
          <nav>
            <span className="land-preview-navitem active">▦ Dashboard</span>
            <span className="land-preview-navitem">☰ Jobs</span>
            <span className="land-preview-navitem">◉ Workers</span>
          </nav>
          <div className="land-preview-side-foot">UTC backend · v1.2</div>
        </aside>

        {/* main */}
        <div className="land-preview-main">
          <div className="land-preview-top">
            <strong>Distributed job scheduler</strong>
            <div className="land-preview-top-actions">
              <span className="land-preview-bell">🔔<i>3</i></span>
              <span className="land-preview-cta">+ New job</span>
            </div>
          </div>

          {/* stat tiles */}
          <div className="land-preview-stats">
            <div className="land-preview-stat">
              <span className="k">Active jobs</span>
              <strong style={{ color: '#f59e0b' }}>12</strong>
            </div>
            <div className="land-preview-stat">
              <span className="k">Success rate 24h</span>
              <strong style={{ color: '#22c55e' }}>98.4%</strong>
            </div>
            <div className="land-preview-stat">
              <span className="k">Avg duration</span>
              <strong style={{ color: '#818cf8' }}>412ms</strong>
            </div>
          </div>

          {/* jobs table */}
          <div className="land-preview-jobs">
            <div className="land-preview-jobs-head">
              <span>Recent jobs</span>
              <span className="land-preview-admin">
                {includeAdminBadge ? 'ADMIN' : ''}
              </span>
            </div>
            <ul>
              {previewJobs.map(j => (
                <li key={j.name}>
                  <code>{j.name}</code>
                  <span className="land-preview-status" style={{ background: statusColors[j.status] }}>
                    {j.status.replaceAll('_', ' ')}
                  </span>
                  <span className="land-preview-prio">P{j.priority}</span>
                  <span className="land-preview-due">{j.due}</span>
                </li>
              ))}
            </ul>
          </div>

          {/* bottom row: chart + projects */}
          <div className="land-preview-row">
            <div className="land-preview-chart">
              <span>Hourly throughput</span>
              <div className="land-preview-bars">
                {activityBars.map((h, i) => (
                  <span key={i} style={{ height: `${h}%` }} />
                ))}
              </div>
            </div>
            <div className="land-preview-progress">
              <span>Project progress</span>
              {projects.map(p => (
                <div key={p.name} className="land-preview-bar-row">
                  <span>{p.name}</span>
                  <div className="land-preview-track">
                    <div className="land-preview-fill" style={{ width: `${p.pct}%` }} />
                  </div>
                  <em>{p.pct}%</em>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
