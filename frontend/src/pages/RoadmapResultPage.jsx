import { useState, useEffect } from 'react'
import { Link, useParams, useNavigate } from 'react-router-dom'
import {
  ArrowRight,
  BookOpen,
  CalendarDays,
  CheckCircle2,
  ExternalLink,
  Lightbulb,
  Map,
  Sparkles,
  Target,
  TrendingUp,
  UserCircle2,
  XCircle,
  Zap,
} from 'lucide-react'
import MinimalSiteHeader from '../components/MinimalSiteHeader.jsx'
import AnalyzeAnotherBar from '../components/AnalyzeAnotherBar.jsx'
import ReportPageFooter from '../components/ReportPageFooter.jsx'

const ROADMAP_LOADING_MESSAGES = [
  'Fetching public GitHub profile…',
  'Reviewing repositories…',
  'Scoring portfolio signals…',
  'Identifying skill gaps…',
  'Building your roadmap…',
  'Almost done…',
]

function LoadingState({ username }) {
  const [msgIdx, setMsgIdx] = useState(0)
  const [dotCount, setDotCount] = useState(1)

  useEffect(() => {
    const msgTimer = setInterval(() => {
      setMsgIdx((i) => Math.min(i + 1, ROADMAP_LOADING_MESSAGES.length - 1))
    }, 2200)
    const dotTimer = setInterval(() => {
      setDotCount((d) => (d % 3) + 1)
    }, 480)
    return () => {
      clearInterval(msgTimer)
      clearInterval(dotTimer)
    }
  }, [])

  return (
    <div className="report-loading-shell">
      <div className="report-loading-card">
        <div className="report-loading-ring-wrap">
          <div className="report-loading-orb" aria-hidden />
          <div className="report-loading-ring" aria-hidden />
        </div>
        <div className="report-loading-text-col">
          <p className="report-loading-handle">@{username}</p>
          <p className="report-loading-message" key={msgIdx}>
            {ROADMAP_LOADING_MESSAGES[msgIdx]}
          </p>
          <span className="report-loading-dots" aria-hidden>
            {'.'.repeat(dotCount)}
          </span>
        </div>
      </div>
    </div>
  )
}

function RoadmapSection({ eyebrow, title, icon: Icon, children, variant }) {
  return (
    <div className={`roadmap-section roadmap-section--${variant}`}>
      <div className="roadmap-section-header">
        <span className={`roadmap-section-icon section-icon-slot section-icon-tone-${variant === 'warm' ? 'warm' : variant === 'pos' ? 'pos' : 'roadmap'}`} aria-hidden>
          <Icon size={17} strokeWidth={1.75} />
        </span>
        <div>
          {eyebrow && <span className="roadmap-section-eyebrow">{eyebrow}</span>}
          <h2 className="roadmap-section-title">{title}</h2>
        </div>
      </div>
      <div className="roadmap-section-body">
        {children}
      </div>
    </div>
  )
}

export default function RoadmapResultPage() {
  const { username } = useParams()
  const navigate = useNavigate()
  const [data, setData] = useState(null)
  const [error, setError] = useState(null)
  const [searchValue, setSearchValue] = useState('')

  useEffect(() => {
    if (!username) return
    setData(null)
    setError(null)
    window.scrollTo(0, 0)
    fetch(`http://localhost:8080/api/roadmap/${encodeURIComponent(username)}`)
      .then((res) => {
        if (!res.ok) throw new Error(`GitHub user not found or request failed (${res.status})`)
        return res.json()
      })
      .then((json) => setData(json))
      .catch((err) => setError(err.message))
  }, [username])

  const handleAnalyzeAnother = (e) => {
    e.preventDefault()
    const val = searchValue.trim().replace(/^@/, '')
    if (!val) return
    setSearchValue('')
    navigate(`/roadmap/${encodeURIComponent(val)}`)
  }

  const loading = !data && !error

  return (
    <div className="app-shell theme-roadmap">
      <div className="main-area">
        <MinimalSiteHeader />

        <main className="content">
          {loading && <LoadingState username={username} />}

          {error && (
            <div className="report-loading-shell">
              <div className="report-loading-card">
                <p style={{ color: 'var(--text-muted)', textAlign: 'center' }}>
                  {error}
                </p>
                <Link
                  to="/roadmap"
                  className="btn-primary"
                  style={{ marginTop: '1.25rem', display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}
                >
                  Try again
                </Link>
              </div>
            </div>
          )}

          {data && (() => {
            const analysis = data.analysis ?? data
            const roadmap = data.roadmap ?? null

            return (
              <div className="report-page-stack">
                <div className="roadmap-result-page">
                  <AnalyzeAnotherBar
                    value={searchValue}
                    onChange={setSearchValue}
                    onSubmit={handleAnalyzeAnother}
                  />

                  {/* Profile strip */}
                  <div className="ai-result-profile-strip roadmap-profile-strip">
                    {analysis.avatarUrl ? (
                      <img className="ai-result-avatar" src={analysis.avatarUrl} alt="" />
                    ) : (
                      <span className="ai-result-avatar-placeholder" aria-hidden>
                        <UserCircle2 size={36} strokeWidth={1.4} />
                      </span>
                    )}
                    <div className="ai-result-profile-info">
                      <span className="ai-result-profile-name">{analysis.name || `@${username}`}</span>
                      <span className="ai-result-profile-username">@{username}</span>
                    </div>
                    <div className="roadmap-profile-score-block">
                      <span className="ai-result-score-num">{analysis.score}</span>
                      <span className="ai-result-score-denom">/100</span>
                    </div>
                    <div className="roadmap-profile-links">
                      <Link to={`/report/${encodeURIComponent(username)}`} className="ai-result-scored-link">
                        Scored report
                        <ExternalLink size={11} strokeWidth={1.8} aria-hidden />
                      </Link>
                      <Link to={`/ai-report/${encodeURIComponent(username)}`} className="ai-result-scored-link">
                        AI Report
                        <ExternalLink size={11} strokeWidth={1.8} aria-hidden />
                      </Link>
                    </div>
                  </div>

                  {/* Page heading */}
                  <div className="roadmap-page-heading">
                    <div className="roadmap-page-heading-eyebrow">
                      <Map size={13} strokeWidth={1.8} aria-hidden />
                      AI Roadmap
                    </div>
                    <h1 className="roadmap-page-heading-title">
                      Portfolio improvement plan for{' '}
                      <span className="roadmap-page-heading-username">@{username}</span>
                    </h1>
                  </div>

                  {/* Unavailable state */}
                  {!roadmap || !roadmap.available ? (
                    <div className="card ai-result-unavailable">
                      <Sparkles size={20} strokeWidth={1.6} aria-hidden className="ai-result-unavailable-icon" />
                      <p className="ai-result-unavailable-msg">
                        {roadmap?.unavailableReason ?? 'AI roadmap is not available right now. Your scored report is still complete.'}
                      </p>
                      <Link
                        to={`/report/${encodeURIComponent(username)}`}
                        className="btn-primary"
                        style={{ marginTop: '1rem', display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}
                      >
                        View scored report
                        <ExternalLink size={13} strokeWidth={1.8} aria-hidden />
                      </Link>
                    </div>
                  ) : (
                    <div className="roadmap-result-body">

                      {/* 1. Roadmap Summary */}
                      {roadmap.roadmapSummary && (
                        <div className="roadmap-summary-block">
                          <div className="roadmap-summary-eyebrow">
                            <Map size={13} strokeWidth={1.8} aria-hidden />
                            Roadmap Summary
                          </div>
                          <p className="roadmap-summary-text">{roadmap.roadmapSummary}</p>
                        </div>
                      )}

                      {/* 2. Quick Wins */}
                      {roadmap.quickWins?.length > 0 && (
                        <RoadmapSection
                          eyebrow="Week 1"
                          title="Quick Wins"
                          icon={Zap}
                          variant="warm"
                        >
                          <div className="roadmap-quick-wins">
                            {roadmap.quickWins.map((win, i) => (
                              <div key={i} className="roadmap-quick-win-item">
                                <span className="roadmap-quick-win-num" aria-hidden>{i + 1}</span>
                                <div className="roadmap-quick-win-content">
                                  <p className="roadmap-quick-win-action">{win.action}</p>
                                  <p className="roadmap-quick-win-why">{win.why}</p>
                                </div>
                              </div>
                            ))}
                          </div>
                        </RoadmapSection>
                      )}

                      {/* 3. Skills to Learn Next */}
                      {roadmap.skillsToLearnNext?.length > 0 && (
                        <RoadmapSection
                          eyebrow="Skill Gaps"
                          title="Skills to Learn Next"
                          icon={BookOpen}
                          variant="violet"
                        >
                          <div className="roadmap-skills-grid">
                            {roadmap.skillsToLearnNext.map((skill, i) => (
                              <div key={i} className="roadmap-skill-card">
                                <div className="roadmap-skill-name">{skill.skill}</div>
                                <p className="roadmap-skill-why">{skill.why}</p>
                                <div className="roadmap-skill-meta">
                                  <div className="roadmap-skill-meta-row">
                                    <span className="roadmap-skill-meta-label">Missing evidence</span>
                                    <p className="roadmap-skill-meta-text">{skill.evidenceMissing}</p>
                                  </div>
                                  <div className="roadmap-skill-meta-row">
                                    <span className="roadmap-skill-meta-label">How to show it</span>
                                    <p className="roadmap-skill-meta-text roadmap-skill-meta-text--action">{skill.howToShow}</p>
                                  </div>
                                </div>
                              </div>
                            ))}
                          </div>
                        </RoadmapSection>
                      )}

                      {/* 4. Next Project Direction */}
                      {roadmap.nextProjectDirection?.length > 0 && (
                        <RoadmapSection
                          eyebrow="Project Direction"
                          title="Next Project Ideas"
                          icon={Lightbulb}
                          variant="mag"
                        >
                          <div className="roadmap-projects-list">
                            {roadmap.nextProjectDirection.map((project, i) => (
                              <div key={i} className="roadmap-project-card">
                                <div className="roadmap-project-header">
                                  <span className="roadmap-project-num" aria-hidden>{String.fromCharCode(65 + i)}</span>
                                  <h3 className="roadmap-project-name">{project.projectName}</h3>
                                </div>
                                <p className="roadmap-project-why">{project.whyItFits}</p>
                                <div className="roadmap-project-details">
                                  <div className="roadmap-project-detail">
                                    <span className="roadmap-project-detail-label">Skills it proves</span>
                                    <p className="roadmap-project-detail-text">{project.skillsItProves}</p>
                                  </div>
                                  <div className="roadmap-project-detail">
                                    <span className="roadmap-project-detail-label">What to build</span>
                                    <p className="roadmap-project-detail-text">{project.coreFeatures}</p>
                                  </div>
                                  <div className="roadmap-project-detail roadmap-project-detail--standout">
                                    <span className="roadmap-project-detail-label">What makes it impressive</span>
                                    <p className="roadmap-project-detail-text">{project.whatMakesItImpressive}</p>
                                  </div>
                                </div>
                              </div>
                            ))}
                          </div>
                        </RoadmapSection>
                      )}

                      {/* 5. Highest-Impact Change */}
                      {roadmap.highestImpactChange && (
                        <div className="roadmap-impact-block">
                          <div className="roadmap-impact-icon-wrap" aria-hidden>
                            <Target size={18} strokeWidth={1.75} />
                          </div>
                          <div>
                            <div className="roadmap-impact-label">Highest-Impact Change</div>
                            <p className="roadmap-impact-text">{roadmap.highestImpactChange}</p>
                          </div>
                        </div>
                      )}

                      {/* 6. 3-Month Plan */}
                      {(roadmap.monthOnePlan || roadmap.monthTwoPlan || roadmap.monthThreePlan) && (
                        <RoadmapSection
                          eyebrow="Implementation Plan"
                          title="3-Month Roadmap"
                          icon={CalendarDays}
                          variant="violet"
                        >
                          <div className="roadmap-months">
                            {roadmap.monthOnePlan && (
                              <div className="roadmap-month">
                                <div className="roadmap-month-header roadmap-month-header--one">
                                  <span className="roadmap-month-num">Month 1</span>
                                  <span className="roadmap-month-theme">Clean &amp; Clarify</span>
                                </div>
                                <p className="roadmap-month-text">{roadmap.monthOnePlan}</p>
                              </div>
                            )}
                            {roadmap.monthTwoPlan && (
                              <div className="roadmap-month">
                                <div className="roadmap-month-header roadmap-month-header--two">
                                  <span className="roadmap-month-num">Month 2</span>
                                  <span className="roadmap-month-theme">Build or Upgrade</span>
                                </div>
                                <p className="roadmap-month-text">{roadmap.monthTwoPlan}</p>
                              </div>
                            )}
                            {roadmap.monthThreePlan && (
                              <div className="roadmap-month">
                                <div className="roadmap-month-header roadmap-month-header--three">
                                  <span className="roadmap-month-num">Month 3</span>
                                  <span className="roadmap-month-theme">Polish &amp; Present</span>
                                </div>
                                <p className="roadmap-month-text">{roadmap.monthThreePlan}</p>
                              </div>
                            )}
                          </div>
                        </RoadmapSection>
                      )}

                      {/* 7. Expected Outcome */}
                      {roadmap.expectedOutcome && (
                        <RoadmapSection
                          eyebrow="After the Roadmap"
                          title="Expected Outcome"
                          icon={TrendingUp}
                          variant="pos"
                        >
                          <p className="roadmap-outcome-text">{roadmap.expectedOutcome}</p>
                        </RoadmapSection>
                      )}

                      {/* 8. Do / Avoid */}
                      {(roadmap.doList?.length > 0 || roadmap.avoidList?.length > 0) && (
                        <div className="roadmap-do-avoid-row">
                          {roadmap.doList?.length > 0 && (
                            <div className="roadmap-do-avoid-card roadmap-do-avoid-card--do">
                              <div className="roadmap-do-avoid-header">
                                <CheckCircle2 size={15} strokeWidth={2} aria-hidden />
                                Do
                              </div>
                              <ul className="roadmap-do-avoid-list">
                                {roadmap.doList.map((item, i) => (
                                  <li key={i}>
                                    <ArrowRight size={11} strokeWidth={2} aria-hidden />
                                    {item}
                                  </li>
                                ))}
                              </ul>
                            </div>
                          )}
                          {roadmap.avoidList?.length > 0 && (
                            <div className="roadmap-do-avoid-card roadmap-do-avoid-card--avoid">
                              <div className="roadmap-do-avoid-header">
                                <XCircle size={15} strokeWidth={2} aria-hidden />
                                Avoid
                              </div>
                              <ul className="roadmap-do-avoid-list">
                                {roadmap.avoidList.map((item, i) => (
                                  <li key={i}>
                                    <ArrowRight size={11} strokeWidth={2} aria-hidden />
                                    {item}
                                  </li>
                                ))}
                              </ul>
                            </div>
                          )}
                        </div>
                      )}

                      <p className="ai-result-footnote">
                        <TrendingUp size={12} strokeWidth={1.75} aria-hidden />
                        Generated from your GitHub analysis only — scores and repo metrics are unchanged.
                      </p>

                    </div>
                  )}
                </div>

                <ReportPageFooter />
              </div>
            )
          })()}
        </main>
      </div>
    </div>
  )
}
