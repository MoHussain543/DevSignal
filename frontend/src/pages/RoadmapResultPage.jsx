import { useState, useEffect } from 'react'
import { Link, useParams, useNavigate } from 'react-router-dom'
import {
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
import { useAuth } from '../context/useAuth.js'
import { apiBaseUrl, apiUrl, readApiError } from '../lib/api.js'

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

function RoadmapSection({ eyebrow, title, icon: Icon, children }) {
  return (
    <section className="flow-section">
      <div className="flow-section-head">
        <span className="flow-section-icon flow-section-icon--trim" aria-hidden>
          <Icon size={16} strokeWidth={1.75} />
        </span>
        <div>
          {eyebrow ? <span className="flow-section-eyebrow">{eyebrow}</span> : null}
          <h2 className="flow-section-title">{title}</h2>
        </div>
      </div>
      <div className="flow-section-body">
        {children}
      </div>
    </section>
  )
}

function QuickWinStep({ step, action, why }) {
  return (
    <article className="pattern-win-step">
      <span className="pattern-win-step-num">{step}</span>
      <div className="pattern-win-step-body">
        <p className="pattern-win-step-action">{action}</p>
        {why ? <p className="pattern-win-step-why">{why}</p> : null}
      </div>
    </article>
  )
}

function SkillCard({ skill, why, evidenceMissing, howToShow }) {
  return (
    <article className="pattern-skill-card">
      <span className="pattern-skill-badge">{skill}</span>
      <p className="pattern-skill-why">{why}</p>
      <div className="pattern-skill-meta-grid">
        {evidenceMissing ? (
          <div className="pattern-skill-meta">
            <span className="pattern-skill-meta-label">Missing evidence</span>
            <p>{evidenceMissing}</p>
          </div>
        ) : null}
        {howToShow ? (
          <div className="pattern-skill-meta">
            <span className="pattern-skill-meta-label">How to show it</span>
            <p>{howToShow}</p>
          </div>
        ) : null}
      </div>
    </article>
  )
}

function ProjectSpotlight({ name, whyItFits, skillsItProves, coreFeatures, whatMakesItImpressive }) {
  return (
    <article className="pattern-project-card">
      <h3 className="pattern-project-name">{name}</h3>
      <p className="pattern-project-fit">{whyItFits}</p>
      {skillsItProves ? (
        <div className="pattern-project-tags">
          {(typeof skillsItProves === 'string'
            ? skillsItProves.split(/[,·|/]+/)
            : [String(skillsItProves)]
          ).map((tag) => tag.trim()).filter(Boolean).map((tag) => (
            <span key={tag} className="pattern-project-tag">{tag}</span>
          ))}
        </div>
      ) : null}
      <dl className="pattern-project-details">
        {coreFeatures ? (
          <>
            <dt>What to build</dt>
            <dd>{coreFeatures}</dd>
          </>
        ) : null}
        {whatMakesItImpressive ? (
          <>
            <dt>What makes it impressive</dt>
            <dd>{whatMakesItImpressive}</dd>
          </>
        ) : null}
      </dl>
    </article>
  )
}

function PhaseStep({ marker, label, body }) {
  return (
    <div className="pattern-phase-step">
      <span className="pattern-phase-marker">{marker}</span>
      <div className="pattern-phase-body">
        <span className="pattern-phase-label">{label}</span>
        <p className="pattern-phase-text">{body}</p>
      </div>
    </div>
  )
}

function GuidanceLane({ title, items, icon: Icon, variant }) {
  if (!items?.length) return null
  return (
    <div className={`pattern-guidance-lane pattern-guidance-lane--${variant}`}>
      <h3 className="pattern-guidance-lane-head">
        <Icon size={14} strokeWidth={2} aria-hidden />
        {title}
      </h3>
      <ul className="pattern-guidance-list">
        {items.map((item, i) => (
          <li key={i}>{item}</li>
        ))}
      </ul>
    </div>
  )
}

export default function RoadmapResultPage() {
  const { username } = useParams()
  const navigate = useNavigate()
  const { session } = useAuth()
  const [data, setData] = useState(null)
  const [error, setError] = useState(null)
  const [searchValue, setSearchValue] = useState('')

  useEffect(() => {
    if (!username) return
    setData(null)
    setError(null)
    window.scrollTo(0, 0)
    const headers = session?.access_token
      ? { Authorization: `Bearer ${session.access_token}` }
      : undefined
    let cancelled = false
    let pollTimerId = null

    const handleRequestError = (e) => {
      setError(
        e.name === 'TypeError' && e.message.includes('fetch')
          ? `Cannot reach the backend at ${apiBaseUrl}. Make sure the API is running and VITE_API_BASE_URL is set correctly.`
          : e.message
      )
    }

    const pollJob = async (runKey) => {
      const res = await fetch(apiUrl(`/api/roadmap/jobs/${runKey}`), { headers })
      if (!res.ok) {
        throw new Error(await readApiError(res))
      }

      const job = await res.json()
      if (cancelled) return true

      if (job.status === 'completed' || job.status === 'partial') {
        setData(job.result)
        return true
      }

      if (job.status === 'failed') {
        throw new Error(job.errorMessage || 'AI roadmap generation failed. Please try again.')
      }

      return false
    }

    const schedulePoll = (runKey) => {
      pollTimerId = window.setTimeout(async () => {
        try {
          const done = await pollJob(runKey)
          if (!done && !cancelled) {
            schedulePoll(runKey)
          }
        } catch (e) {
          if (!cancelled) {
            handleRequestError(e)
          }
        }
      }, 1800)
    }

    async function startJob() {
      try {
        const res = await fetch(apiUrl(`/api/roadmap/${encodeURIComponent(username)}/jobs`), {
          method: 'POST',
          headers,
        })
        if (!res.ok) {
          throw new Error(await readApiError(res))
        }

        const job = await res.json()
        if (cancelled) return

        const done = await pollJob(job.runKey)
        if (!done && !cancelled) {
          schedulePoll(job.runKey)
        }
      } catch (e) {
        if (!cancelled) {
          handleRequestError(e)
        }
      }
    }

    startJob()

    return () => {
      cancelled = true
      if (pollTimerId !== null) {
        window.clearTimeout(pollTimerId)
      }
    }
  }, [session?.access_token, username])

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
                    <div className="flow-doc flow-doc--roadmap roadmap-result-body pattern-layout">

                      {roadmap.roadmapSummary && (
                        <section className="flow-section flow-section--opening pattern-hero-callout">
                          <div className="flow-section-head">
                            <span className="flow-section-icon" aria-hidden>
                              <Map size={16} strokeWidth={1.75} />
                            </span>
                            <div>
                              <span className="flow-section-eyebrow">Overview</span>
                              <h2 className="flow-section-title">Roadmap summary</h2>
                            </div>
                          </div>
                          <p className="pattern-hero-callout-text">{roadmap.roadmapSummary}</p>
                        </section>
                      )}

                      {roadmap.quickWins?.length > 0 && (
                        <RoadmapSection eyebrow="Week 1" title="Quick wins" icon={Zap}>
                          <div className="pattern-win-runway">
                            {roadmap.quickWins.map((win, i) => (
                              <QuickWinStep key={i} step={i + 1} action={win.action} why={win.why} />
                            ))}
                          </div>
                        </RoadmapSection>
                      )}

                      {roadmap.skillsToLearnNext?.length > 0 && (
                        <RoadmapSection eyebrow="Skill gaps" title="Skills to learn next" icon={BookOpen}>
                          <div className="pattern-skill-grid">
                            {roadmap.skillsToLearnNext.map((skill, i) => (
                              <SkillCard
                                key={i}
                                skill={skill.skill}
                                why={skill.why}
                                evidenceMissing={skill.evidenceMissing}
                                howToShow={skill.howToShow}
                              />
                            ))}
                          </div>
                        </RoadmapSection>
                      )}

                      {roadmap.nextProjectDirection?.length > 0 && (
                        <RoadmapSection eyebrow="Project direction" title="Next project ideas" icon={Lightbulb}>
                          <div className="pattern-project-grid">
                            {roadmap.nextProjectDirection.map((project, i) => (
                              <ProjectSpotlight
                                key={i}
                                name={project.projectName}
                                whyItFits={project.whyItFits}
                                skillsItProves={project.skillsItProves}
                                coreFeatures={project.coreFeatures}
                                whatMakesItImpressive={project.whatMakesItImpressive}
                              />
                            ))}
                          </div>
                        </RoadmapSection>
                      )}

                      {roadmap.highestImpactChange && (
                        <section className="flow-section pattern-unlock-banner">
                          <div className="pattern-unlock-banner-inner">
                            <span className="pattern-unlock-icon" aria-hidden>
                              <Target size={20} strokeWidth={1.75} />
                            </span>
                            <div>
                              <span className="flow-section-eyebrow">Highest impact</span>
                              <h2 className="flow-section-title">One change that matters most</h2>
                              <p className="pattern-unlock-text">{roadmap.highestImpactChange}</p>
                            </div>
                          </div>
                        </section>
                      )}

                      {(roadmap.monthOnePlan || roadmap.monthTwoPlan || roadmap.monthThreePlan) && (
                        <RoadmapSection eyebrow="Implementation plan" title="3-month roadmap" icon={CalendarDays}>
                          <div className="pattern-phase-pipeline">
                            <svg className="pattern-phase-pipeline-line" viewBox="0 0 1000 24" preserveAspectRatio="none" aria-hidden>
                              <path d="M 20 12 L 980 12" />
                            </svg>
                            {roadmap.monthOnePlan && (
                              <PhaseStep
                                marker="M1"
                                label="Month 1 · Clean & clarify"
                                body={roadmap.monthOnePlan}
                              />
                            )}
                            {roadmap.monthTwoPlan && (
                              <PhaseStep
                                marker="M2"
                                label="Month 2 · Build or upgrade"
                                body={roadmap.monthTwoPlan}
                              />
                            )}
                            {roadmap.monthThreePlan && (
                              <PhaseStep
                                marker="M3"
                                label="Month 3 · Polish & present"
                                body={roadmap.monthThreePlan}
                              />
                            )}
                          </div>
                        </RoadmapSection>
                      )}

                      {roadmap.expectedOutcome && (
                        <section className="flow-section pattern-outcome-strip">
                          <span className="pattern-outcome-icon" aria-hidden>
                            <TrendingUp size={18} strokeWidth={1.75} />
                          </span>
                          <div>
                            <span className="flow-section-eyebrow">After the roadmap</span>
                            <h2 className="flow-section-title">Expected outcome</h2>
                            <p className="pattern-outcome-text">{roadmap.expectedOutcome}</p>
                          </div>
                        </section>
                      )}

                      {(roadmap.doList?.length > 0 || roadmap.avoidList?.length > 0) && (
                        <section className="flow-section pattern-guidance-board">
                          <GuidanceLane title="Do" items={roadmap.doList} icon={CheckCircle2} variant="do" />
                          <GuidanceLane title="Avoid" items={roadmap.avoidList} icon={XCircle} variant="avoid" />
                        </section>
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
