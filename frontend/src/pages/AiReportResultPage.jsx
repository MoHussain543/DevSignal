import { useState, useEffect } from 'react'
import { Link, useParams, useNavigate } from 'react-router-dom'
import {
  ExternalLink,
  Sparkles,
  Target,
  TrendingUp,
  UserCircle2,
  Zap,
} from 'lucide-react'
import MinimalSiteHeader from '../components/MinimalSiteHeader.jsx'
import AnalyzeAnotherBar from '../components/AnalyzeAnotherBar.jsx'
import ReportPageFooter from '../components/ReportPageFooter.jsx'

const AI_LOADING_MESSAGES = [
  'Fetching public profile…',
  'Reviewing repositories…',
  'Scoring your portfolio…',
  'Running AI analysis…',
  'Writing narrative sections…',
  'Almost done…',
]

function LoadingState({ username }) {
  const [msgIdx, setMsgIdx] = useState(0)
  const [dotCount, setDotCount] = useState(1)

  useEffect(() => {
    const msgTimer = setInterval(() => {
      setMsgIdx((i) => Math.min(i + 1, AI_LOADING_MESSAGES.length - 1))
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
            {AI_LOADING_MESSAGES[msgIdx]}
          </p>
          <span className="report-loading-dots" aria-hidden>
            {'.'.repeat(dotCount)}
          </span>
        </div>
      </div>
    </div>
  )
}

function AiSection({ label, children, variant }) {
  if (!children) return null
  return (
    <div className={`ai-result-section ai-result-section--${variant}`}>
      <h2 className="ai-result-section-label">{label}</h2>
      <p className="ai-result-section-body">{children}</p>
    </div>
  )
}

function ActionCard({ icon: Icon, label, body, variant }) {
  if (!body) return null
  return (
    <div className={`ai-result-action-card ai-result-action-card--${variant}`}>
      <span className="ai-result-action-icon" aria-hidden>
        <Icon size={16} strokeWidth={1.85} />
      </span>
      <div>
        <h2 className="ai-result-action-label">{label}</h2>
        <p className="ai-result-action-body">{body}</p>
      </div>
    </div>
  )
}

export default function AiReportResultPage() {
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
    fetch(`http://localhost:8080/api/report/${encodeURIComponent(username)}`)
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
    navigate(`/ai-report/${encodeURIComponent(val)}`)
  }

  const loading = !data && !error

  return (
    <div className="app-shell">
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
                <Link to="/ai-report" className="btn-primary" style={{ marginTop: '1.25rem', display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                  Try again
                </Link>
              </div>
            </div>
          )}

          {data && (() => {
            const analysis = data.analysis ?? data
            const ai = data.aiSummary ?? null

            return (
              <div className="report-page-stack">
                <AnalyzeAnotherBar
                  value={searchValue}
                  onChange={setSearchValue}
                  onSubmit={handleAnalyzeAnother}
                />

                <div className="ai-result-page">
                  <div className="ai-result-profile-strip">
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
                    <div className="ai-result-profile-score">
                      <span className="ai-result-score-num">{analysis.score}</span>
                      <span className="ai-result-score-denom">/100</span>
                    </div>
                    <Link
                      to={`/report/${encodeURIComponent(username)}`}
                      className="ai-result-scored-link"
                    >
                      View scored report
                      <ExternalLink size={12} strokeWidth={1.8} aria-hidden />
                    </Link>
                  </div>

                  {!ai || !ai.available ? (
                    <div className="card ai-result-unavailable">
                      <Sparkles size={20} strokeWidth={1.6} aria-hidden className="ai-result-unavailable-icon" />
                      <p className="ai-result-unavailable-msg">
                        {ai?.unavailableReason ?? 'AI narrative is not available right now. Your scored report is still complete.'}
                      </p>
                      <Link to={`/report/${encodeURIComponent(username)}`} className="btn-primary" style={{ marginTop: '1rem', display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                        View scored report
                        <ExternalLink size={13} strokeWidth={1.8} aria-hidden />
                      </Link>
                    </div>
                  ) : (
                    <div className="ai-result-body">
                      {ai.overallSummary && (
                        <div className="ai-result-lead-block">
                          <div className="ai-result-lead-eyebrow">
                            <Sparkles size={13} strokeWidth={1.8} aria-hidden />
                            AI Perspective
                          </div>
                          <p className="ai-result-lead-text">{ai.overallSummary}</p>
                        </div>
                      )}

                      <div className="ai-result-sections-grid">
                        <AiSection label="Hiring impression" variant="impression">
                          {ai.hiringImpression}
                        </AiSection>

                        <AiSection label="What stands out" variant="standout">
                          {ai.whatStandsOut}
                        </AiSection>

                        <AiSection label="What weakens the profile" variant="weakens">
                          {ai.whatWeakens}
                        </AiSection>
                      </div>

                      <div className="ai-result-actions-row">
                        <ActionCard
                          icon={Zap}
                          label="Improve first"
                          body={ai.improveFirst}
                          variant="first"
                        />
                        <ActionCard
                          icon={Target}
                          label="Biggest unlock"
                          body={ai.biggestUnlock}
                          variant="unlock"
                        />
                      </div>

                      <p className="ai-result-footnote">
                        <TrendingUp size={12} strokeWidth={1.75} aria-hidden />
                        Generated from your existing analysis only — scores and repo metrics are unchanged.
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
