import { Home, Radar } from 'lucide-react'
import { NAV_KEYS } from '../navConfig.js'

const NAV = [
  { key: NAV_KEYS[0], label: 'Home', Icon: Home },
  { key: NAV_KEYS[1], label: 'Analysis', Icon: Radar },
]

export default function Sidebar({ activeNav, onNavClick }) {
  return (
    <aside className="sidebar">
      <div className="sidebar-logo">
        <div className="logo-text">
          <span className="logo-mark" aria-hidden="true">
            <span className="logo-live-ring" />
            <span className="logo-dot" />
          </span>
          DevSignal
        </div>
        <div className="logo-tagline">Portfolio Intelligence</div>
      </div>
      <nav className="sidebar-nav" aria-label="Page sections">
        <div className="nav-section-label">Navigation</div>
        {NAV.map(({ key, label, Icon }) => (
          <button
            key={key}
            type="button"
            className={`nav-item${activeNav === key ? ' active' : ''}`}
            onClick={() => onNavClick(key)}
          >
            <span className="nav-icon" aria-hidden>
              <Icon size={17} strokeWidth={1.7} />
            </span>
            <span className="nav-label">{label}</span>
          </button>
        ))}
      </nav>
      <div className="sidebar-footer">
        <span className="sidebar-version">v0.1.0 MVP</span>
      </div>
    </aside>
  )
}
