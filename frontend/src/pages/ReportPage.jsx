import { useState, useEffect } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import AnalysisReport from '../components/AnalysisReport.jsx'
import MinimalSiteHeader from '../components/MinimalSiteHeader.jsx'
import AnalyzeAnotherBar from '../components/AnalyzeAnotherBar.jsx'
import ReportPageFooter from '../components/ReportPageFooter.jsx'
import { ArrowLeft } from 'lucide-react'

const LOADING_MESSAGES = [
  'Fetching public GitHub profile…',
  'Reviewing repositories…',
  'Checking README files…',
  'Scoring portfolio signals…',
  'Preparing your report…',
  'Writing AI summary…',
]

export default function ReportPage() {
  const { username } = useParams()
  const navigate = useNavigate()

  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [msgIdx, setMsgIdx] = useState(0)
  const [searchValue, setSearchValue] = useState('')

  useEffect(() => {
    if (!loading) return
    const id = setInterval(() => {
      setMsgIdx((i) => (i + 1) % LOADING_MESSAGES.length)
    }, 1300)
    return () => clearInterval(id)
  }, [loading])

  useEffect(() => {
    if (!username) return
    setData(null)
    setError(null)
    setLoading(true)
    setMsgIdx(0)
    window.scrollTo(0, 0)

    fetch(`http://localhost:8080/api/report/${encodeURIComponent(username)}`)
      .then((res) => {
        if (!res.ok) {
          return res.json().catch(() => null).then((body) => {
            throw new Error(body?.detail || `Error ${res.status}: ${res.statusText}`)
          })
        }
        return res.json()
      })
      .then((json) => {
        setData({
          ...json.analysis,
          aiSummary: json.aiSummary,
        })
        setLoading(false)
      })
      .catch((e) => {
        setError(
          e.name === 'TypeError' && e.message.includes('fetch')
            ? 'Cannot reach the backend at localhost:8080. Make sure the Spring Boot server is running.'
            : e.message
        )
        setLoading(false)
      })
  }, [username])

  const handleAnotherSearch = (e) => {
    e.preventDefault()
    const trimmed = searchValue.trim().replace(/^@/, '')
    if (!trimmed) return
    setSearchValue('')
    navigate(`/report/${encodeURIComponent(trimmed)}`)
  }

  return (
    <div className="app-shell">
      <div className="main-area">
        <MinimalSiteHeader />

        <main className="content">
          {loading && (
            <div className="report-loading-shell" role="status" aria-live="polite">
              <div className="report-loading-card">
                <div className="report-loading-ring-wrap" aria-hidden>
                  <div className="report-loading-orb" />
                  <div className="report-loading-ring" />
                </div>
                <div className="report-loading-text-col">
                  <div className="report-loading-handle">@{username}</div>
                  <div className="report-loading-message" key={msgIdx}>
                    {LOADING_MESSAGES[msgIdx]}
                  </div>
                  <div className="report-loading-dots" aria-hidden>
                    <span /><span /><span />
                  </div>
                </div>
              </div>
            </div>
          )}

          {error && !loading && (
            <div className="state-box error-state">
              <span className="state-icon">⚠</span>
              <p>{error}</p>
              <Link to="/" className="btn-outline report-error-back">
                <ArrowLeft size={14} aria-hidden /> Back to Home
              </Link>
            </div>
          )}

          {data && !loading ? (
            <div className="report-page-stack">
              <AnalyzeAnotherBar
                value={searchValue}
                onChange={setSearchValue}
                onSubmit={handleAnotherSearch}
              />
              <AnalysisReport data={data} />
              <ReportPageFooter />
            </div>
          ) : null}
        </main>
      </div>
    </div>
  )
}
