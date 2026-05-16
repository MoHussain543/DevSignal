import RepoCard from './RepoCard.jsx'
import RevealSection from './RevealSection.jsx'
import SectionHeading from './SectionHeading.jsx'
import {
  Activity,
  BarChart3,
  FolderGit2,
  Gauge,
  Radar,
  TrendingUp,
} from 'lucide-react'

function scoreColor(score) {
  if (score >= 85) return 'var(--accent-primary-hover)'
  if (score >= 70) return 'var(--accent-primary)'
  if (score >= 50) return 'rgba(139, 92, 246, 0.72)'
  return 'rgba(236, 72, 153, 0.65)'
}

function ScoreRing({ score }) {
  const color = scoreColor(score)
  return (
    <div className="score-ring-wrap" style={{ '--score': score, '--ring-color': color }}>
      <div className="score-inner">
        <span className="score-num">{score}</span>
        <span className="score-denom">/100</span>
      </div>
    </div>
  )
}

function CandidateBadge({ level }) {
  const slug = level.includes('Strong')
    ? 'strong'
    : level.includes('Promising')
      ? 'promising'
      : level.includes('Developing')
        ? 'developing'
        : 'early'
  const cls = {
    'Strong candidate': 'badge-green',
    'Promising candidate': 'badge-violet',
    'Developing candidate': 'badge-yellow',
    'Early portfolio': 'badge-orange',
  }[level] || 'badge-gray'

  return (
    <div className={`candidate-badge-shell halo-${slug}`}>
      <span className={`badge badge-candidate ${cls}`}>{level}</span>
    </div>
  )
}

function ProgressBar({ label, value, max = 100, color, weighted }) {
  const pct = Math.min(100, Math.round((value / max) * 100))
  return (
    <div className="progress-row">
      <span className="progress-label">{label}</span>
      <div className="progress-track">
        <div
          className={`progress-fill ${weighted ? 'progress-fill--weighted' : ''}`}
          style={{ width: `${pct}%`, background: color || 'var(--accent-primary)' }}
        />
      </div>
      <span className="progress-value">
        {value}<span className="progress-max">/{max}</span>
      </span>
    </div>
  )
}

export default function AnalysisReport({ data }) {
  const {
    username, name, avatarUrl, bio,
    publicRepos, followers,
    analyzedRepoCount, originalRepoCount, portfolioRepoCount,
    portfolioTotalStars, portfolioTotalForks, portfolioAverageRepoScore,
    portfolioTopLanguages,
    candidateLevel, hiringRecommendation, score,
    scoreExplanation,
    projectQualityExplanation,
    technicalBreadthExplanation,
    documentationExplanation,
    originalityExplanation,
    activityExplanation,
    scoreBreakdown, weightedScoreBreakdown,
    technicalHighlights, growthAreas,
    featuredRepo,
    featuredRepoReason,
    repos,
  } = data

  const spotlightRepo = featuredRepo ?? null
  const otherRepos = spotlightRepo
    ? repos.filter((r) => r.name !== spotlightRepo.name)
    : repos

  return (
    <div className="report">

      {/* Analysis stack: profile → score → evaluation → weighted breakdown */}
      <section id="section-analysis" data-nav-section="analysis" className="report-section-anchor">
        <div className="report-analysis-suite">
          <RevealSection>
            <div className="card profile-card profile-card-shell card-gradient-edge-sm">
              <SectionHeading title="Profile" icon={Gauge} tone="violet" />
              <div className="profile-card-body">
                {avatarUrl ? <img className="avatar" src={avatarUrl} alt="" /> : null}
                <div className="profile-info">
                  <h2 className="profile-name">{name || username}</h2>
                  <p className="profile-username">@{username}</p>
                  {bio ? <p className="profile-bio">{bio}</p> : null}
                  <div className="profile-stats">
                    <span className="stat"><strong>{followers.toLocaleString()}</strong> followers</span>
                    <span className="stat-sep">·</span>
                    <span className="stat"><strong>{publicRepos}</strong> public repos</span>
                    <span className="stat-sep">·</span>
                    <span className="stat"><strong>{portfolioRepoCount}</strong> portfolio repos</span>
                  </div>
                  {portfolioTopLanguages?.length > 0 ? (
                    <div className="lang-badges">
                      {portfolioTopLanguages.map(lang => (
                        <span key={lang} className="badge badge-lang">{lang}</span>
                      ))}
                    </div>
                  ) : null}
                </div>
              </div>
          </div>
          </RevealSection>

          <RevealSection delay={40}>
            <div className="card-row report-analysis-feature-row">
              <div className="card score-card card-gradient-edge score-card--featured">
                <SectionHeading title="Hiring signal" icon={Radar} tone="violet" titleClassName="card-title--lead" />
                <ScoreRing score={score} />
                <div className="score-meta">
                  <CandidateBadge level={candidateLevel} />
                  <p className="recommendation recommendation--featured">{hiringRecommendation}</p>
                </div>
              </div>

              <div className="card card--quiet">
                <SectionHeading title="Portfolio metrics" icon={Activity} tone="violet" />
                <div className="portfolio-stats">
                  <Stat label="Repos analyzed" value={analyzedRepoCount} />
                  <Stat label="Original repos" value={originalRepoCount} />
                  <Stat label="Portfolio repos" value={portfolioRepoCount} />
                  <Stat label="Total stars" value={portfolioTotalStars} />
                  <Stat label="Total forks" value={portfolioTotalForks} />
                  <Stat label="Avg composite repo score" value={portfolioAverageRepoScore} suffix="/100" />
                </div>
              </div>
            </div>
          </RevealSection>

          <RevealSection delay={30}>
            <div className="card evaluation-summary-card evaluation-summary-card--editorial">
              <SectionHeading icon={BarChart3} tone="violet" title="Reading this profile" titleClassName="card-title--lead" />
              <p className="evaluation-summary-lead">{scoreExplanation}</p>
              <div className="evaluation-mini-grid">
              <div className="evaluation-mini-card">
                <div className="evaluation-mini-label">Project quality</div>
                <p className="evaluation-mini-body">{projectQualityExplanation}</p>
              </div>
              <div className="evaluation-mini-card">
                <div className="evaluation-mini-label">Technical breadth</div>
                <p className="evaluation-mini-body">{technicalBreadthExplanation}</p>
              </div>
              <div className="evaluation-mini-card">
                <div className="evaluation-mini-label">Documentation</div>
                <p className="evaluation-mini-body">{documentationExplanation}</p>
              </div>
              <div className="evaluation-mini-card">
                <div className="evaluation-mini-label">Originality</div>
                <p className="evaluation-mini-body">{originalityExplanation}</p>
              </div>
              <div className="evaluation-mini-card">
                <div className="evaluation-mini-label">Activity</div>
                <p className="evaluation-mini-body">{activityExplanation}</p>
              </div>
            </div>
          </div>
          </RevealSection>

          <RevealSection delay={50}>
            <div className="card-row report-breakdown-row">
              <div className="card card--stat">
                <SectionHeading icon={BarChart3} tone="violet" title="Signal mix (normalized)" />
                <div className="breakdown">
                  <ProgressBar label="Project quality" value={scoreBreakdown.projectQualityScore} />
                  <ProgressBar label="Technical breadth" value={scoreBreakdown.technicalBreadthScore} />
                  <ProgressBar label="Activity" value={scoreBreakdown.activityScore} />
                  <ProgressBar label="Documentation" value={scoreBreakdown.documentationScore} />
                  <ProgressBar label="Originality" value={scoreBreakdown.originalityScore} />
                </div>
              </div>

              <div className="card card--stat">
                <SectionHeading icon={TrendingUp} tone="violet" title="Weighted points" />
                <div className="breakdown">
                  <ProgressBar
                    label="Project quality"
                    value={weightedScoreBreakdown.projectQualityPoints}
                    max={30}
                    weighted
                    color="var(--accent-primary)"
                  />
                  <ProgressBar
                    label="Technical breadth"
                    value={weightedScoreBreakdown.technicalBreadthPoints}
                    max={20}
                    weighted
                    color="var(--accent-primary)"
                  />
                  <ProgressBar
                    label="Documentation"
                    value={weightedScoreBreakdown.documentationPoints}
                    max={20}
                    weighted
                    color="var(--accent-primary)"
                  />
                  <ProgressBar
                    label="Originality"
                    value={weightedScoreBreakdown.originalityPoints}
                    max={20}
                    weighted
                    color="var(--accent-primary)"
                  />
                  <ProgressBar
                    label="Activity"
                    value={weightedScoreBreakdown.activityPoints}
                    max={10}
                    weighted
                    color="var(--accent-primary)"
                  />
                </div>
                <div className="weighted-total">
                  Total score: <strong>{weightedScoreBreakdown.totalPoints}</strong>/100
                </div>
              </div>
            </div>
          </RevealSection>
        </div>
      </section>

      {/* Repositories */}
      <section id="section-repositories" className="report-section-anchor">
        <RevealSection delay={36}>
          <div className="card card-repositories-suite">
            <SectionHeading
              icon={FolderGit2}
              tone="violet"
              title="Repositories"
              titleClassName="card-title--lead"
            />
            {spotlightRepo ? (
              <div className="repo-feature-frame">
                <p className="repo-feature-kicker">Strongest signal</p>
                {featuredRepoReason ? (
                  <p className="repo-feature-reason">{featuredRepoReason}</p>
                ) : null}
                <RevealSection delay={20}>
                  <RepoCard repo={spotlightRepo} />
                </RevealSection>
              </div>
            ) : null}
            {otherRepos.length > 0 ? (
              <div className="repo-grid repo-grid--remainder">
                {otherRepos.map((repo, i) => (
                  <RevealSection key={repo.name} delay={Math.min(i * 38, 220)}>
                    <RepoCard repo={repo} />
                  </RevealSection>
                ))}
              </div>
            ) : null}
            {repos.length === 0 ? (
              <p className="text-muted">No repositories found.</p>
            ) : null}
          </div>
        </RevealSection>
      </section>

      {/* Insights */}
      <section id="section-insights" className="report-section-anchor">
        <RevealSection delay={40}>
          <div className="card insights-duo-card">
            <SectionHeading icon={Radar} tone="violet" title="Signals & gaps" titleClassName="card-title--lead" />
            <div className="insights-duo-grid">
              <div className="insights-duo-col">
                <p className="insights-col-label">Technical highlights</p>
                <ul className="signal-list">
                  {technicalHighlights.length > 0 ? (
                    technicalHighlights.map((h, i) => (
                      <li key={i} className="signal-item signal-item--positive">{h}</li>
                    ))
                  ) : (
                    <li className="signal-item signal-muted">No highlights detected</li>
                  )}
                </ul>
              </div>
              <div className="insights-duo-col insights-duo-col--rule">
                <p className="insights-col-label">Growth areas</p>
                <ul className="signal-list">
                  {growthAreas.length > 0 ? (
                    growthAreas.map((a, i) => (
                      <li key={i} className="signal-item signal-item--watch">{a}</li>
                    ))
                  ) : (
                    <li className="signal-item signal-muted">None identified</li>
                  )}
                </ul>
              </div>
            </div>
          </div>
        </RevealSection>
      </section>

    </div>
  )
}

function Stat({ label, value, suffix = '' }) {
  return (
    <div className="pstat">
      <span className="pstat-value">
        {typeof value === 'number' ? value.toLocaleString() : value}{suffix}
      </span>
      <span className="pstat-label">{label}</span>
    </div>
  )
}
