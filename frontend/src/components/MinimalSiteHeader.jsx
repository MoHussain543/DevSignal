import { Link } from 'react-router-dom'

/** DevSignal brand only — used on report flows without top nav clutter. */
export default function MinimalSiteHeader() {
  return (
    <header className="site-header site-header--minimal">
      <Link to="/" className="header-brand-link">
        <span className="header-brand">DevSignal</span>
      </Link>
    </header>
  )
}
