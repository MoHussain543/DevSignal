import { useState, useEffect } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import AnalysisReport from '../components/AnalysisReport.jsx'
import { ArrowLeft, Search, Terminal } from 'lucide-react'

const LOADING_MESSAGES = [
  'Fetching public GitHub profile…',
  'Reviewing repositories…',
  'Checking README files…',
  'Scoring portfolio signals…',
  'Preparing your report…',
]

export default function ReportPage() {
  const { username } = useParams()
  const navigate = useNavigate()

  const [data,           setData]           = useState(null)
  const [loading,        setLoading]        = useState(true)
  const [error,          setError]          = useState(null)
  const [msgIdx,         setMsgIdx]         = useState(0)
  const [searchValue,    setSearchValue]    = useState('')

  /* Cycle loading messages */
  useEffect(() => {
    if (!loading) return
    const id = setInterval(() => {
      setMsgIdx((i) => (i + 1) % LOADING_MESSAGES.length)
    }, 1300)
    return () => clearInterval(id)
  }, [loading])

  /* Fetch whenever the username param changes */
  useEffect(() => {
    if (!username) return
    setData(null)
    setError(null)
    setLoading(true)
    setMsgIdx(0)
    window.scrollTo(0, 0)

    fetch(`http://localhost:8080/api/analyze/${encodeURIComponent(username)}`)
      .then((res) => {
        if (!res.ok) {
          return res.json().catch(() => null).then((body) => {
            throw new Error(body?.detail || `Error ${res.status}: ${res.statusText}`)
          })
        }
        return res.json()
      })
      .then((json) => {
        setData(json)
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
    const trimmed = searchValue.trim()
    if (!trimmed) return
    setSearchValue('')
    navigate(`/report/${encodeURIComponent(trimmed)}`)
  }

  return (
    <div className="app-shell">
      <div className="main-area">

        {/* ── Report page header ── */}
        <header className="site-header report-page-header">
          <Link to="/" className="report-back-link" aria-label="Back to home">
            <ArrowLeft size={14} strokeWidth={2} aria-hidden />
            Home
          </Link>
          <div className="header-sep" />
          <span className="header-brand">DevSignal</span>

          <span className="report-analyzing-chip" aria-label={`Analyzing @${username}`}>
            Analyzing&nbsp;<span className="report-analyzing-username">@{username}</span>
          </span>

          <form
            className="report-search-form"
            onSubmit={handleAnotherSearch}
            aria-label="Analyze another profile"
          >
            <Terminal size={13} strokeWidth={1.65} className="report-search-ico" aria-hidden />
            <input
              className="report-search-input"
              type="text"
              value={searchValue}
              onChange={(e) => setSearchValue(e.target.value)}
              placeholder="Analyze another username…"
              autoComplete="off"
              spellCheck={false}
              aria-label="GitHub username"
            />
            <button
              type="submit"
              className="report-search-btn"
              disabled={!searchValue.trim()}
              aria-label="Analyze"
            >
              <Search size={13} strokeWidth={2} aria-hidden />
            </button>
          </form>
        </header>

        <main className="content">

          {/* ── Polished loading state ── */}
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

          {/* ── Error state ── */}
          {error && !loading && (
            <div className="state-box error-state">
              <span className="state-icon">⚠</span>
              <p>{error}</p>
              <Link to="/" className="btn-outline report-error-back">
                <ArrowLeft size={14} aria-hidden /> Back to Home
              </Link>
            </div>
          )}

          {/* ── Report ── */}
          {data && !loading ? <AnalysisReport data={data} /> : null}

        </main>
      </div>
    </div>
  )
}
