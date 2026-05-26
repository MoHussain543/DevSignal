import RepoCard from './RepoCard.jsx'
import RevealSection from './RevealSection.jsx'
import {
  Activity,
  BarChart3,
  FolderGit2,
  Radar,
  TrendingUp,
} from 'lucide-react'

function scoreColor(score) {
  if (score >= 85) return 'var(--flow-accent-hover)'
  if (score >= 70) return 'var(--flow-accent)'
  if (score >= 50) return 'rgba(139, 92, 246, 0.72)'
  return 'rgba(139, 92, 246, 0.55)'
}

function ScoreRing({ score }) {
  const color = scoreColor(score)
  return (
    <div className="score-ring-wrap flow-score-ring" style={{ '--score': score, '--ring-color': color }}>
      <div className="score-inner">
        <span className="score-num">{score}</span>
        <span className="score-denom">/100</span>
      </div>
    </div>
  )
}

function CandidateBadge({ level }) {
  const cls = {
    'Strong portfolio signal': 'badge-green',
    'Promising portfolio signal': 'badge-violet',
    'Developing portfolio': 'badge-yellow',
    'Early-stage portfolio': 'badge-orange',
  }[level] || 'badge-gray'

  return <span className={`badge badge-candidate ${cls}`}>{level}</span>
}

function ProgressBar({ label, value, max = 100, color, weighted }) {
  const pct = Math.min(100, Math.round((value / max) * 100))
  return (
    <div className="progress-row flow-progress-row">
      <span className="progress-label">{label}</span>
      <div className="progress-track">
        <div
          className={`progress-fill ${weighted ? 'progress-fill--weighted' : ''}`}
          style={{ width: `${pct}%`, background: color || 'var(--flow-accent)' }}
        />
      </div>
      <span className="progress-value">
        {value}<span className="progress-max">/{max}</span>
      </span>
    </div>
  )
}

const SUMMARY_DIMENSIONS = [
  { key: 'projectQualityExplanation', label: 'Project quality' },
  { key: 'technicalBreadthExplanation', label: 'Technology variety' },
  { key: 'documentationExplanation', label: 'README & documentation' },
  { key: 'originalityExplanation', label: 'Original project work' },
  { key: 'activityExplanation', label: 'Recent activity' },
]

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

  const explanations = {
    projectQualityExplanation,
    technicalBreadthExplanation,
    documentationExplanation,
    originalityExplanation,
    activityExplanation,
  }

  const spotlightRepo = featuredRepo ?? null
  const otherRepos = spotlightRepo
    ? repos.filter((r) => r.name !== spotlightRepo.name)
    : repos

  return (
    <div className="report">
      <section id="section-analysis" data-nav-section="analysis" className="report-section-anchor">
        <div className="flow-doc flow-doc--analyzer">

          <RevealSection>
            <header className="flow-profile-band">
              {avatarUrl ? <img className="flow-profile-avatar" src={avatarUrl} alt="" /> : null}
              <div className="flow-profile-copy">
                <h2 className="flow-profile-name">{name || username}</h2>
                <p className="flow-profile-handle">@{username}</p>
                {bio ? <p className="flow-profile-bio">{bio}</p> : null}
                <div className="flow-profile-stats">
                  <span><strong>{followers.toLocaleString()}</strong> followers</span>
                  <span className="flow-profile-stat-sep" aria-hidden>·</span>
                  <span><strong>{publicRepos}</strong> public repos</span>
                  <span className="flow-profile-stat-sep" aria-hidden>·</span>
                  <span><strong>{portfolioRepoCount}</strong> portfolio repos</span>
                </div>
                {portfolioTopLanguages?.length > 0 ? (
                  <div className="lang-badges flow-profile-langs">
                    {portfolioTopLanguages.map((lang) => (
                      <span key={lang} className="badge badge-lang">{lang}</span>
                    ))}
                  </div>
                ) : null}
              </div>
            </header>
          </RevealSection>

          <RevealSection delay={40}>
            <div className="flow-score-band">
              <div className="flow-score-main">
                <ScoreRing score={score} />
                <div className="flow-score-meta">
                  <span className="flow-section-eyebrow">
                    <Radar size={13} strokeWidth={1.75} aria-hidden />
                    Overall score
                  </span>
                  <CandidateBadge level={candidateLevel} />
                  <p className="flow-score-rec">{hiringRecommendation}</p>
                </div>
              </div>
              <div className="flow-metrics-grid">
                <Stat label="Repos reviewed" value={analyzedRepoCount} />
                <Stat label="Original repos" value={originalRepoCount} />
                <Stat label="Portfolio repos" value={portfolioRepoCount} />
                <Stat label="Total stars" value={portfolioTotalStars} />
                <Stat label="Total forks" value={portfolioTotalForks} />
                <Stat label="Avg repo quality" value={portfolioAverageRepoScore} suffix="/100" />
              </div>
            </div>
          </RevealSection>

          <RevealSection delay={30}>
            <section className="flow-section" aria-labelledby="report-summary-heading">
              <div className="flow-section-head">
                <span className="flow-section-icon" aria-hidden>
                  <BarChart3 size={16} strokeWidth={1.75} />
                </span>
                <div>
                  <span className="flow-section-eyebrow">Summary</span>
                  <h2 id="report-summary-heading" className="flow-section-title">Profile read</h2>
                </div>
              </div>
              <p className="flow-lead">{scoreExplanation}</p>
              <div className="flow-rows">
                {SUMMARY_DIMENSIONS.map(({ key, label }) => (
                  <div key={key} className="flow-row">
                    <span className="flow-row-label">{label}</span>
                    <p className="flow-row-text">{explanations[key]}</p>
                  </div>
                ))}
              </div>
            </section>
          </RevealSection>

          <RevealSection delay={50}>
            <section className="flow-section flow-section--split" aria-label="Score breakdown">
              <div className="flow-split-col">
                <div className="flow-section-head flow-section-head--compact">
                  <span className="flow-section-icon" aria-hidden>
                    <BarChart3 size={16} strokeWidth={1.75} />
                  </span>
                  <h3 className="flow-section-title">How your score is built</h3>
                </div>
                <p className="flow-section-hint">
                  Each category is scored out of 100 before contributing to your final score.
                </p>
                <div className="breakdown">
                  <ProgressBar label="Project quality" value={scoreBreakdown.projectQualityScore} />
                  <ProgressBar label="Technology variety" value={scoreBreakdown.technicalBreadthScore} />
                  <ProgressBar label="Recent activity" value={scoreBreakdown.activityScore} />
                  <ProgressBar label="README & documentation" value={scoreBreakdown.documentationScore} />
                  <ProgressBar label="Original project work" value={scoreBreakdown.originalityScore} />
                </div>
              </div>
              <div className="flow-split-col">
                <div className="flow-section-head flow-section-head--compact">
                  <span className="flow-section-icon flow-section-icon--trim" aria-hidden>
                    <TrendingUp size={16} strokeWidth={1.75} />
                  </span>
                  <h3 className="flow-section-title">Points earned</h3>
                </div>
                <p className="flow-section-hint">
                  Weighted contribution of each category to your final score.
                </p>
                <div className="breakdown">
                  <ProgressBar label="Project quality" value={weightedScoreBreakdown.projectQualityPoints} max={30} weighted color="var(--flow-trim)" />
                  <ProgressBar label="Technology variety" value={weightedScoreBreakdown.technicalBreadthPoints} max={20} weighted color="var(--flow-trim)" />
                  <ProgressBar label="README & documentation" value={weightedScoreBreakdown.documentationPoints} max={20} weighted color="var(--flow-trim)" />
                  <ProgressBar label="Original project work" value={weightedScoreBreakdown.originalityPoints} max={20} weighted color="var(--flow-trim)" />
                  <ProgressBar label="Recent activity" value={weightedScoreBreakdown.activityPoints} max={10} weighted color="var(--flow-trim)" />
                </div>
                <div className="weighted-total flow-weighted-total">
                  Final score: <strong>{weightedScoreBreakdown.totalPoints}</strong>/100
                </div>
              </div>
            </section>
          </RevealSection>
        </div>
      </section>

      <section id="section-repositories" className="report-section-anchor">
        <RevealSection delay={36}>
          <section className="flow-doc flow-doc--analyzer flow-section" aria-labelledby="repos-heading">
            <div className="flow-section-head">
              <span className="flow-section-icon" aria-hidden>
                <FolderGit2 size={16} strokeWidth={1.75} />
              </span>
              <div>
                <span className="flow-section-eyebrow">Repositories</span>
                <h2 id="repos-heading" className="flow-section-title">Repositories reviewed</h2>
              </div>
            </div>
            {spotlightRepo ? (
              <div className="flow-repo-spotlight">
                <p className="flow-row-label">Best portfolio repo</p>
                {featuredRepoReason ? (
                  <p className="flow-section-hint flow-repo-reason">{featuredRepoReason}</p>
                ) : null}
                <RepoCard repo={spotlightRepo} />
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
          </section>
        </RevealSection>
      </section>

      <section id="section-insights" className="report-section-anchor">
        <RevealSection delay={40}>
          <section className="flow-doc flow-doc--analyzer flow-section flow-section--split" aria-labelledby="insights-heading">
            <div className="flow-section-head flow-section-head--full">
              <span className="flow-section-icon" aria-hidden>
                <Activity size={16} strokeWidth={1.75} />
              </span>
              <div>
                <span className="flow-section-eyebrow">Insights</span>
                <h2 id="insights-heading" className="flow-section-title">Strengths & areas to improve</h2>
              </div>
            </div>
            <div className="flow-split-col">
              <p className="flow-row-label">What looks strong</p>
              <ul className="signal-list flow-signal-list">
                {technicalHighlights.length > 0 ? (
                  technicalHighlights.map((h, i) => (
                    <li key={i} className="signal-item signal-item--positive">{h}</li>
                  ))
                ) : (
                  <li className="signal-item signal-muted">Nothing stood out in this pass.</li>
                )}
              </ul>
            </div>
            <div className="flow-split-col">
              <p className="flow-row-label">What to improve</p>
              <ul className="signal-list flow-signal-list">
                {growthAreas.length > 0 ? (
                  growthAreas.map((a, i) => (
                    <li key={i} className="signal-item signal-item--watch">{a}</li>
                  ))
                ) : (
                  <li className="signal-item signal-muted">No major gaps flagged.</li>
                )}
              </ul>
            </div>
          </section>
        </RevealSection>
      </section>
    </div>
  )
}

function Stat({ label, value, suffix = '' }) {
  return (
    <div className="flow-metric">
      <span className="flow-metric-value">
        {typeof value === 'number' ? value.toLocaleString() : value}{suffix}
      </span>
      <span className="flow-metric-label">{label}</span>
    </div>
  )
}
