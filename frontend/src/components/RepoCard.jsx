import {
  Activity,
  BookOpen,
  CheckCircle2,
  Code2,
  GitFork,
  Star,
  XCircle,
} from 'lucide-react'

const QUALITY_BADGE = {
  'Strong portfolio repo': 'badge-green',
  'Solid project': 'badge-violet',
  'Basic project': 'badge-yellow',
  Fork: 'badge-gray',
  'Looks like a demo or sample': 'badge-orange',
  'Active, but README is thin': 'badge-yellow',
  'Room to strengthen this repo': 'badge-red',
}

function repoScoreColor(s) {
  if (s >= 70) return 'var(--green)'
  if (s >= 50) return 'var(--accent-primary)'
  if (s >= 30) return 'var(--yellow)'
  return 'var(--red)'
}

function ReadmeCheck({ label, ok }) {
  const Ico = ok ? CheckCircle2 : XCircle
  const cls = ok ? 'check-ok' : 'check-no'
  const tone = ok ? 'ico-pos' : 'ico-rose'
  return (
    <span className={`readme-check ${cls} readme-check--icon`}>
      <Ico className={`readme-check-icon ${tone}`} aria-hidden strokeWidth={1.8} size={13} /> {label}
    </span>
  )
}

export default function RepoCard({ repo }) {
  const {
    name, description, language, stars, forks,
    fork, likelyDemoRepo, recentlyUpdated,
    repoQualitySignal,
    repoScore,
    repoSignals,
    readmeAnalysis: r,
  } = repo

  const scoreCol = repoScoreColor(repoScore)
  const qualityBadgeCls = QUALITY_BADGE[repoQualitySignal] || 'badge-gray'

  return (
    <div className={`repo-card${fork ? ' repo-fork' : ''}`}>
      <div className="repo-header">
        <div className="repo-title-row">
          <span className="repo-name">{name}</span>
          {language ? (
            <span className="badge badge-lang repo-lang-chip">
              <Code2 aria-hidden strokeWidth={1.7} size={12} /> {language}
            </span>
          ) : null}
        </div>
        <span className={`badge ${qualityBadgeCls} repo-quality-badge`}>
          <Activity aria-hidden strokeWidth={1.7} size={12} /> {repoQualitySignal}
        </span>
      </div>

      {description ? <p className="repo-desc">{description}</p> : null}

      <div className="repo-meta">
        <span className="repo-stat repo-stat-ico">
          <Star aria-hidden strokeWidth={1.7} size={14} fill="rgba(251,191,36,0.12)" stroke="var(--accent-warm-soft)" /> {stars}
        </span>
        <span className="repo-stat repo-stat-ico">
          <GitFork aria-hidden strokeWidth={1.7} size={14} stroke="var(--accent-magenta-soft)" /> {forks}
        </span>
        {recentlyUpdated ? (
          <span className="badge badge-green repo-flag">
            <Activity aria-hidden strokeWidth={1.7} size={12} /> Active
          </span>
        ) : null}
        {fork ? <span className="badge badge-gray repo-flag">Fork</span> : null}
        {likelyDemoRepo ? (
          <span className="badge badge-orange repo-flag">Demo</span>
        ) : null}
      </div>

      <div className="repo-score-row">
        <span className="repo-score-label">Score</span>
        <div className="repo-score-track repo-score-track-strong">
          <div
            className="repo-score-fill"
            style={{ width: `${repoScore}%`, background: scoreCol }}
          />
        </div>
        <span className="repo-score-val" style={{ color: scoreCol }}>{repoScore}</span>
      </div>

      <div className="readme-section">
        <div className="readme-header readme-header-icons">
          <BookOpen aria-hidden strokeWidth={1.7} size={13} stroke="var(--accent-secondary)" />{' '}
          <span className="readme-label">README & docs</span>
          {r.hasReadme ? (
            <span className="badge badge-green"><CheckCircle2 aria-hidden strokeWidth={1.7} size={12} /> Present</span>
          ) : (
            <span className="badge badge-red"><XCircle aria-hidden strokeWidth={1.7} size={12} /> Missing</span>
          )}
          {r.hasReadme ? (
            <span className="readme-doc-score">Doc {r.documentationScore}/100</span>
          ) : null}
        </div>
        {r.hasReadme ? (
          <div className="readme-checks">
            <ReadmeCheck label="Install" ok={r.hasInstallationInstructions} />
            <ReadmeCheck label="Usage" ok={r.hasUsageInstructions} />
            <ReadmeCheck label="Tech Stack" ok={r.hasTechStackMention} />
            <ReadmeCheck label="Features" ok={r.hasFeatureSection} />
            <ReadmeCheck label="Screenshots" ok={r.hasScreenshots} />
          </div>
        ) : null}
      </div>

      {repoSignals.length > 0 ? (
        <div className="repo-signals">
          {repoSignals.map((s, i) => (
            <span key={i} className="signal-chip">{s}</span>
          ))}
        </div>
      ) : null}
    </div>
  )
}
