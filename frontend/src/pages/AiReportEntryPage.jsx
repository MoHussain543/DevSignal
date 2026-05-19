import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Sparkles } from 'lucide-react'
import MinimalSiteHeader from '../components/MinimalSiteHeader.jsx'

export default function AiReportEntryPage() {
  const navigate = useNavigate()
  const [username, setUsername] = useState('')

  const handleSubmit = (e) => {
    e.preventDefault()
    const trimmed = username.trim().replace(/^@/, '')
    if (!trimmed) return
    navigate(`/ai-report/${encodeURIComponent(trimmed)}`)
  }

  return (
    <div className="app-shell">
      <div className="main-area">
        <MinimalSiteHeader />

        <main className="ai-entry-main">
          <div className="ai-entry-glow" aria-hidden />

          <div className="card ai-entry-card card-gradient-edge">
            <div className="ai-entry-eyebrow">
              <Sparkles size={13} strokeWidth={1.8} aria-hidden />
              Powered by AI
            </div>

            <h1 className="ai-entry-headline">
              Get a deeper read on any GitHub profile.
            </h1>
            <p className="ai-entry-sub">
              Enter a GitHub username and we'll run a full scored analysis, then layer a strategic
              AI narrative on top — covering overall impression, hiring read, what stands out,
              what weakens the profile, and two concrete growth actions.
            </p>

            <form className="ai-entry-form" onSubmit={handleSubmit}>
              <div className="ai-entry-input-wrap">
                <span className="ai-entry-at" aria-hidden>@</span>
                <input
                  className="ai-entry-input"
                  type="text"
                  placeholder="github-username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  autoFocus
                  autoComplete="off"
                  spellCheck={false}
                  aria-label="GitHub username"
                />
              </div>
              <button
                type="submit"
                className="btn-primary ai-entry-btn"
                disabled={!username.trim()}
              >
                <Sparkles size={15} strokeWidth={1.8} aria-hidden />
                Generate AI Report
              </button>
            </form>

            <p className="ai-entry-note">
              Reads public profile data only. No account needed.
            </p>
          </div>
        </main>
      </div>
    </div>
  )
}
