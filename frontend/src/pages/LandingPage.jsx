import { Link, useNavigate } from 'react-router-dom'
import SearchBar from '../components/SearchBar.jsx'
import LandingSections from '../components/LandingSections.jsx'
import DevSignalLogo from '../components/DevSignalLogo.jsx'
import { useAuth } from '../context/useAuth.js'

export default function LandingPage() {
  const navigate = useNavigate()
  const { isAuthenticated } = useAuth()

  const handleSearch = (username) => {
    const trimmed = username.trim()
    if (!trimmed) return
    navigate(`/report/${encodeURIComponent(trimmed)}`)
  }

  return (
    <div className="app-shell theme-analyzer">
      <div className="main-area">
        <header className="site-header site-header--minimal">
          <Link to="/" className="header-brand-link">
            <DevSignalLogo size="sm" />
            <span className="header-brand">DevSignal</span>
          </Link>
          <div className="header-sep" />
          <span className="header-tagline">GitHub Profile Analyzer</span>
          <div className="site-header-actions">
            <Link to={isAuthenticated ? '/me' : '/auth'} className="site-header-auth-link">
              {isAuthenticated ? 'My DevSignal' : 'Sign in'}
            </Link>
          </div>
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
