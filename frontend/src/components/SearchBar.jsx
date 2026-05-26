import { useEffect, useRef, useState } from 'react'
import DevSignalLogo from './DevSignalLogo.jsx'
import { Search, Sparkles, Terminal } from 'lucide-react'

function randomGithubPlaceholder() {
  const stem = ['nova', 'orbit', 'flux', 'delta', 'vertex', 'signal'][Math.floor(Math.random() * 6)]
  const suffix = Math.random().toString(36).slice(2, 8)
  return `${stem}-${suffix}`
}

export default function SearchBar({ onSearch, loading }) {
  const [value, setValue] = useState('')
  const [inputFocused, setInputFocused] = useState(false)
  const [ghostUsername, setGhostUsername] = useState(() => randomGithubPlaceholder())
  const inputRef = useRef(null)

  useEffect(() => {
    if (inputFocused || value.trim() !== '' || loading) return
    const id = window.setInterval(() => {
      setGhostUsername(randomGithubPlaceholder())
    }, 2600)
    return () => window.clearInterval(id)
  }, [inputFocused, value, loading])

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

  const scrollToFeatures = () => {
    document.getElementById('section-beyond-score')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
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
            Developer intelligence for GitHub profiles
          </div>

          <h1 className="hero-headline">
            Turn GitHub profiles into
            <span className="hero-headline-accent"> hiring-ready insights.</span>
          </h1>

          <p className="hero-subheadline">
            Analyze repositories, documentation, project quality, technology variety, and portfolio
            signals from a single GitHub username.
          </p>

          <div className="hero-cta-row">
            <button type="button" className="btn-primary" onClick={scrollToInput}>
              <Search size={15} strokeWidth={2} aria-hidden />
              Analyze a Profile
            </button>
            <button type="button" className="btn-outline" onClick={scrollToFeatures}>
              <Sparkles size={15} strokeWidth={1.8} aria-hidden />
              See What DevSignal Can Do
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

        {/* ── Right column: brand mark ── */}
        <div className="hero-logo-col" aria-hidden>
          <div className="hero-logo-wrap">
            <div className="hero-logo-glow" />
            <DevSignalLogo size="hero" showWordmark />
            <p className="hero-logo-caption">Six reads · one score · clearer hiring signal</p>
          </div>
        </div>

      </div>
    </section>
  )
}
