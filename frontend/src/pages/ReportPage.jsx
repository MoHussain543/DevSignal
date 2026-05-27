import { useState, useEffect } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import AnalysisReport from '../components/AnalysisReport.jsx'
import MinimalSiteHeader from '../components/MinimalSiteHeader.jsx'
import AnalyzeAnotherBar from '../components/AnalyzeAnotherBar.jsx'
import ReportPageFooter from '../components/ReportPageFooter.jsx'
import { ArrowLeft } from 'lucide-react'
import { useAuth } from '../context/useAuth.js'
import { apiBaseUrl, apiUrl, readApiError } from '../lib/api.js'

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
  const { session } = useAuth()

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
      setLoading(false)
    }

    const pollJob = async (runKey) => {
      const res = await fetch(apiUrl(`/api/analyze/jobs/${runKey}`), { headers })
      if (!res.ok) {
        throw new Error(await readApiError(res))
      }

      const job = await res.json()
      if (cancelled) return true

      if (job.status === 'completed' || job.status === 'partial') {
        setData(job.result)
        setLoading(false)
        return true
      }

      if (job.status === 'failed') {
        throw new Error(job.errorMessage || 'Analysis failed. Please try again.')
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
        const res = await fetch(apiUrl(`/api/analyze/${encodeURIComponent(username)}/jobs`), {
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

  const handleAnotherSearch = (e) => {
    e.preventDefault()
    const trimmed = searchValue.trim().replace(/^@/, '')
    if (!trimmed) return
    setSearchValue('')
    navigate(`/report/${encodeURIComponent(trimmed)}`)
  }

  return (
    <div className="app-shell theme-analyzer">
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
              <Link to="/" className="btn-report-home report-error-back">
                <ArrowLeft size={14} aria-hidden /> Back to Home
              </Link>
            </div>
          )}

          {data && !loading && (
            <div className="report-page-stack">
              <AnalyzeAnotherBar
                value={searchValue}
                onChange={setSearchValue}
                onSubmit={handleAnotherSearch}
              />
              <AnalysisReport data={data} />
              <ReportPageFooter />
            </div>
          )}
        </main>
      </div>
    </div>
  )
}
