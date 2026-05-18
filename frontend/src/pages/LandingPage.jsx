import { useNavigate } from 'react-router-dom'
import SearchBar from '../components/SearchBar.jsx'
import LandingSections from '../components/LandingSections.jsx'

export default function LandingPage() {
  const navigate = useNavigate()

  const handleSearch = (username) => {
    const trimmed = username.trim()
    if (!trimmed) return
    navigate(`/report/${encodeURIComponent(trimmed)}`)
  }

  return (
    <div className="app-shell">
      <div className="main-area">
        <header className="site-header">
          <span className="header-brand">DevSignal</span>
          <div className="header-sep" />
          <span className="header-tagline">GitHub Profile Analyzer</span>
          <nav className="header-nav" aria-label="Site navigation">
            <a href="#section-home"       className="nav-link">Home</a>
            <a href="#section-analysis"   className="nav-link">Features</a>
            <a href="#section-ai-summary" className="nav-link">AI Summary</a>
            <a href="#section-roadmap"    className="nav-link">Roadmap</a>
            <a href="#section-home"       className="nav-link nav-link--cta">Analyze</a>
          </nav>
        </header>

        <main className="content">
          <div
            id="section-home"
            data-nav-section="home"
            className="report-section-anchor content-hero-block"
          >
            <SearchBar onSearch={handleSearch} loading={false} />
          </div>
          <LandingSections />
        </main>
      </div>
    </div>
  )
}
