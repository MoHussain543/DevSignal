import { useEffect, useRef, useState } from 'react'
import {
  Activity,
  AlertTriangle,
  CheckCircle2,
  Gauge,
  Radar,
  Search,
  Sparkles,
  Target,
  Terminal,
  TrendingUp,
} from 'lucide-react'

/** Neutral placeholder slugs only — no personal or fixed celebrity handles. */
function randomGithubPlaceholder() {
  const stem = ['nova', 'orbit', 'flux', 'delta', 'vertex', 'signal'][Math.floor(Math.random() * 6)]
  const suffix = Math.random().toString(36).slice(2, 8)
  return `${stem}-${suffix}`
}

/** Decorative icons inside hero preview score ring — rotate every 15s */
const MOCKUP_SCORE_CYCLE_ICONS = [TrendingUp, Gauge, Radar, Activity, Target]

const MOCKUP_SIGNALS = [
  'Your own repositories stand out',
  'Recent commits and updates',
  'Several languages and tools in use',
]

export default function SearchBar({ onSearch, loading }) {
  const [value, setValue] = useState('')
  const [mockupScoreIconIdx, setMockupScoreIconIdx] = useState(0)
  const [inputFocused, setInputFocused] = useState(false)
  const [ghostUsername, setGhostUsername] = useState(() => randomGithubPlaceholder())
  const inputRef = useRef(null)

  useEffect(() => {
    const id = window.setInterval(() => {
      setMockupScoreIconIdx((i) => (i + 1) % MOCKUP_SCORE_CYCLE_ICONS.length)
    }, 15000)
    return () => window.clearInterval(id)
  }, [])

  useEffect(() => {
    if (inputFocused || value.trim() !== '' || loading) return
    const id = window.setInterval(() => {
      setGhostUsername(randomGithubPlaceholder())
    }, 2600)
    return () => window.clearInterval(id)
  }, [inputFocused, value, loading])

  const MockupScoreIcon = MOCKUP_SCORE_CYCLE_ICONS[mockupScoreIconIdx]

  const handleSubmit = (e) => {
    e.preventDefault()
    if (!loading) onSearch(value)
  }

  const placeholder =
    value.trim() !== ''
      ? ''
      : inputFocused
        ? 'GitHub username'
        : `@${ghostUsername}`

  const scrollToInput = () => {
    inputRef.current?.focus()
    inputRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }

  return (
    <section className="hero-section hero-surface">
      {/* Background glow orbs */}
      <div className="hero-orbs" aria-hidden>
        <span className="hero-orb hero-orb--violet" />
        <span className="hero-orb hero-orb--mag" />
        <span className="hero-orb hero-orb--cyan-lg" />
      </div>

      {/* Decorative star dots */}
      <div className="hero-stars" aria-hidden>
        <span className="hero-star" style={{ top: '10%', left: '4%',  width: 3, height: 3, opacity: 0.55, animationDelay: '0s'   }} />
        <span className="hero-star" style={{ top: '28%', left: '1%',  width: 2, height: 2, opacity: 0.3,  animationDelay: '1.1s' }} />
        <span className="hero-star" style={{ top: '58%', left: '6%',  width: 2, height: 2, opacity: 0.42, animationDelay: '2.2s' }} />
        <span className="hero-star" style={{ top: '8%',  right: '7%', width: 4, height: 4, opacity: 0.35, animationDelay: '0.5s' }} />
        <span className="hero-star" style={{ top: '40%', right: '2%', width: 2, height: 2, opacity: 0.28, animationDelay: '1.7s' }} />
        <span className="hero-star" style={{ top: '72%', right: '9%', width: 3, height: 3, opacity: 0.45, animationDelay: '0.9s' }} />
      </div>

      <div className="hero-two-col">

        {/* ── Left column: copy + search ── */}
        <div className="hero-copy-col">

          <div className="hero-eyebrow">
            <Sparkles size={12} strokeWidth={1.8} aria-hidden />
            Straightforward feedback on your public GitHub work
          </div>

          <h1 className="hero-headline">
            Understand how your GitHub profile
            <span className="hero-headline-accent"> comes across at a glance.</span>
          </h1>

          <div className="hero-cta-row">
            <button type="button" className="btn-primary" onClick={scrollToInput}>
              <Search size={15} strokeWidth={2} aria-hidden />
              Analyze a profile
            </button>
          </div>

          <form className="terminal-form terminal-form-live" onSubmit={handleSubmit}>
            <Terminal className="terminal-form-ico eyebrow-ico-tech" aria-hidden strokeWidth={1.65} size={16} />
            <span className="terminal-prefix">$ devsignal analyze</span>
            <input
              ref={inputRef}
              className="terminal-input"
              type="text"
              placeholder={placeholder}
              value={value}
              onChange={(e) => setValue(e.target.value)}
              onFocus={() => setInputFocused(true)}
              onBlur={() => setInputFocused(false)}
              disabled={loading}
              autoComplete="off"
              spellCheck={false}
              aria-label="GitHub username to analyze"
            />
            <button className="terminal-btn terminal-btn-live" type="submit" disabled={loading || !value.trim()}>
              {loading ? 'Analyzing…' : 'Analyze'}
            </button>
          </form>

          <p className="search-hint search-hint-subtle">
            No signup required. Only public profile and repository information are used.
          </p>
        </div>

        {/* ── Right column: floating mockup card ── */}
        <div className="hero-mockup-col" aria-hidden>
          <div className="hero-mockup-wrap">
            <div className="hero-mockup-glow" />

            <div className="hero-mockup-card">
              {/* macOS-style window chrome */}
              <div className="mockup-titlebar">
                <div className="mockup-dots">
                  <span className="mockup-dot mockup-dot-red" />
                  <span className="mockup-dot mockup-dot-yellow" />
                  <span className="mockup-dot mockup-dot-green" />
                </div>
                <span className="mockup-title">DevSignal</span>
              </div>

              <div className="mockup-body">
                {/* Score + level */}
                <div className="mockup-score-row">
                  <div className="mockup-score-ring">
                    <div className="mockup-score-inner">
                      <span className="mockup-score-num">82</span>
                      <span className="mockup-score-sub">/100</span>
                    </div>
                  </div>
                  <div className="mockup-score-meta">
                    <div className="mockup-score-meta-head">
                      <span className="mockup-score-meta-icon-slot" aria-hidden key={mockupScoreIconIdx}>
                        <MockupScoreIcon size={17} strokeWidth={1.85} className="mockup-score-meta-ico" />
                      </span>
                      <div className="mockup-level">Promising candidate</div>
                    </div>
                    <div className="mockup-signal-text">
                      Strong projects overall; README setup could be clearer.
                    </div>
                  </div>
                </div>

                {/* Top signals */}
                <div className="mockup-group">
                  <div className="mockup-section-label">Standouts</div>
                  {MOCKUP_SIGNALS.map((s) => (
                    <div key={s} className="mockup-signal-item mockup-item-green">
                      <CheckCircle2 size={12} strokeWidth={2} aria-hidden /> {s}
                    </div>
                  ))}
                </div>

                {/* Growth area */}
                <div className="mockup-group">
                  <div className="mockup-section-label">Room to improve</div>
                  <div className="mockup-signal-item mockup-item-yellow">
                    <AlertTriangle size={12} strokeWidth={2} aria-hidden /> Add clearer README installation steps
                  </div>
                </div>

              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
