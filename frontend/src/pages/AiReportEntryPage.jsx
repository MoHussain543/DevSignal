import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Sparkles, CheckCircle2 } from 'lucide-react'
import MinimalSiteHeader from '../components/MinimalSiteHeader.jsx'

const FEATURES = [
  'Strategic overall summary of the portfolio',
  'Hiring impression in plain language',
  'What stands out — and what weakens the profile',
  'One immediate action to improve first',
  'The highest-leverage unlock for long-term signal',
  'Grounded in your scored GitHub analysis only',
]

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
    <div className="app-shell theme-ai-report">
      <div className="main-area">
        <MinimalSiteHeader />

        <main className="ai-entry-main">
          <div className="ai-entry-glow" aria-hidden />

          <div className="ai-entry-layout">
            {/* Left: copy column */}
            <div className="ai-entry-copy">
              <div className="ai-entry-eyebrow">
                <Sparkles size={13} strokeWidth={1.8} aria-hidden />
                AI Report
              </div>

              <h1 className="ai-entry-headline">
                Get a deeper, strategic read on any{' '}
                <span className="ai-entry-headline-accent">GitHub profile.</span>
              </h1>

              <p className="ai-entry-sub">
                We run a full scored analysis first, then layer an AI narrative on top — focused on
                hiring signal, credibility gaps, and concrete next steps. No duplicate bullet lists;
                this is the editorial view of your report.
              </p>

              <ul className="ai-entry-features" aria-label="What's in the AI report">
                {FEATURES.map((f) => (
                  <li key={f}>
                    <CheckCircle2 size={13} strokeWidth={2} aria-hidden />
                    {f}
                  </li>
                ))}
              </ul>

              <p className="ai-entry-privacy">
                Uses public GitHub profile and repository information only.
              </p>
            </div>

            {/* Right: form card */}
            <div className="ai-entry-form-col">
              <div className="card card-gradient-edge ai-entry-card">
                <div className="ai-entry-card-eyebrow">
                  <Sparkles size={13} strokeWidth={1.8} aria-hidden />
                  Enter a GitHub username
                </div>

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
                  No account needed. Reads public data only.
                </p>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  )
}
