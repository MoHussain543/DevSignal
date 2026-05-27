import { Link } from 'react-router-dom'
import DevSignalLogo from './DevSignalLogo.jsx'
import { useAuth } from '../context/useAuth.js'

/** DevSignal brand only — used on report flows without top nav clutter. */
export default function MinimalSiteHeader() {
  const { isAuthenticated } = useAuth()

  return (
    <header className="site-header site-header--minimal">
      <Link to="/" className="header-brand-link">
        <DevSignalLogo size="sm" />
        <span className="header-brand">DevSignal</span>
      </Link>
      <div className="site-header-actions">
        <Link to={isAuthenticated ? '/me' : '/auth'} className="site-header-auth-link">
          {isAuthenticated ? 'My DevSignal' : 'Sign in'}
        </Link>
      </div>
    </header>
  )
}
