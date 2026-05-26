import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Map, CheckCircle2 } from 'lucide-react'
import MinimalSiteHeader from '../components/MinimalSiteHeader.jsx'

const FEATURES = [
  'Quick wins you can act on this week',
  'Skills to learn based on your actual GitHub gaps',
  'Tailored project ideas that improve hiring signal',
  'The single highest-impact change you can make',
  'A realistic 3-month improvement plan',
  'How your profile will look after following it',
]

export default function RoadmapEntryPage() {
  const navigate = useNavigate()
  const [username, setUsername] = useState('')

  const handleSubmit = (e) => {
    e.preventDefault()
    const trimmed = username.trim().replace(/^@/, '')
    if (!trimmed) return
    navigate(`/roadmap/${encodeURIComponent(trimmed)}`)
  }

  return (
    <div className="app-shell theme-roadmap">
      <div className="main-area">
        <MinimalSiteHeader />

        <main className="roadmap-entry-main">
          <div className="roadmap-entry-glow" aria-hidden />

          <div className="roadmap-entry-layout">

            {/* Left: copy column */}
            <div className="roadmap-entry-copy">
              <div className="roadmap-entry-eyebrow">
                <Map size={13} strokeWidth={1.8} aria-hidden />
                AI Roadmap
              </div>

              <h1 className="roadmap-entry-headline">
                Build a clearer path to a stronger{' '}
                <span className="roadmap-entry-headline-accent">GitHub portfolio.</span>
              </h1>

              <p className="roadmap-entry-sub">
                Get a tailored, AI-generated roadmap based on your current GitHub profile —
                including quick wins, missing skills, stronger project directions, and a realistic
                3-month improvement plan.
              </p>

              <ul className="roadmap-entry-features" aria-label="What's in the roadmap">
                {FEATURES.map((f) => (
                  <li key={f}>
                    <CheckCircle2 size={13} strokeWidth={2} aria-hidden />
                    {f}
                  </li>
                ))}
              </ul>

              <p className="roadmap-entry-privacy">
                Uses public GitHub profile and repository information only.
              </p>
            </div>

            {/* Right: form card */}
            <div className="roadmap-entry-form-col">
              <div className="card card-gradient-edge roadmap-entry-card">
                <div className="roadmap-entry-card-eyebrow">
                  <Map size={13} strokeWidth={1.8} aria-hidden />
                  Enter a GitHub username
                </div>

                <form className="roadmap-entry-form" onSubmit={handleSubmit}>
                  <div className="roadmap-entry-input-wrap">
                    <span className="roadmap-entry-at" aria-hidden>@</span>
                    <input
                      className="roadmap-entry-input"
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
                    className="btn-primary roadmap-entry-btn"
                    disabled={!username.trim()}
                  >
                    <Map size={15} strokeWidth={1.8} aria-hidden />
                    Generate Roadmap
                  </button>
                </form>

                <p className="roadmap-entry-note">
                  Sign in required. Reads public GitHub data only.
                </p>
              </div>
            </div>

          </div>
        </main>
      </div>
    </div>
  )
}
