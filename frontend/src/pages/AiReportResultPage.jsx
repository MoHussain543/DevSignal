import { useState, useEffect } from 'react'
import { Link, useParams, useNavigate } from 'react-router-dom'
import { ExternalLink, TrendingUp, UserCircle2 } from 'lucide-react'
import MinimalSiteHeader from '../components/MinimalSiteHeader.jsx'
import AnalyzeAnotherBar from '../components/AnalyzeAnotherBar.jsx'
import ReportPageFooter from '../components/ReportPageFooter.jsx'
import AiReportContent from '../components/AiReportContent.jsx'

const AI_LOADING_MESSAGES = [
  'Fetching public profile…',
  'Reviewing repositories…',
  'Scoring your portfolio…',
  'Running AI analysis…',
  'Building verdict and evidence…',
  'Writing priorities…',
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
    <div className="app-shell theme-ai-report">
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
              <div className="report-page-stack ai-report-page-stack">
                <div className="ai-report-page">
                  <AnalyzeAnotherBar
                    value={searchValue}
                    onChange={setSearchValue}
                    onSubmit={handleAnalyzeAnother}
                  />

                  <header className="ai-report-page-header">
                    <div className="ai-result-profile-strip ai-report-profile-strip">
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
                        <span className="ai-report-page-tagline">AI portfolio interpretation</span>
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
                  </header>

                  {!ai || !ai.available ? (
                    <div className="card ai-result-unavailable">
                      <p className="ai-result-unavailable-msg">
                        {ai?.unavailableReason ?? 'AI narrative is not available right now. Your scored report is still complete.'}
                      </p>
                      <Link to={`/report/${encodeURIComponent(username)}`} className="btn-primary" style={{ marginTop: '1rem', display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                        View scored report
                        <ExternalLink size={13} strokeWidth={1.8} aria-hidden />
                      </Link>
                    </div>
                  ) : (
                    <>
                      <AiReportContent ai={ai} />
                      <p className="ai-result-footnote ai-report-footnote">
                        <TrendingUp size={12} strokeWidth={1.75} aria-hidden />
                        Generated from your existing analysis only — scores and repo metrics are unchanged.
                      </p>
                    </>
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
