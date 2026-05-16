import { useCallback, useEffect, useRef, useState } from 'react'
import Sidebar from './components/Sidebar.jsx'
import SearchBar from './components/SearchBar.jsx'
import AnalysisReport from './components/AnalysisReport.jsx'
import LandingSections from './components/LandingSections.jsx'
import { NAV_KEYS, scrollToNavSection } from './navConfig.js'

export default function App() {
  const contentRef = useRef(null)
  const [activeNav, setActiveNav] = useState('home')
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [searched, setSearched] = useState(false)

  const handleSearch = async (username) => {
    if (!username.trim()) return
    setLoading(true)
    setError(null)
    setData(null)
    setSearched(true)
    try {
      const res = await fetch(`http://localhost:8080/api/analyze/${encodeURIComponent(username.trim())}`)
      if (!res.ok) {
        const err = await res.json().catch(() => null)
        throw new Error(err?.detail || `Error ${res.status}: ${res.statusText}`)
      }
      const json = await res.json()
      setData(json)
    } catch (e) {
      if (e.name === 'TypeError' && e.message.includes('fetch')) {
        setError('Cannot reach the backend at localhost:8080. Make sure the Spring Boot server is running.')
      }
      else {
        setError(e.message)
      }
    }
    finally {
      setLoading(false)
    }
  }

  const showPreview = !searched && !loading

  /** Document scroll spy: prefers section closest to focal line in the viewport (below sticky header). */
  useEffect(() => {
    const root = contentRef.current
    if (!root) return

    let raf = 0
    const tick = () => {
      cancelAnimationFrame(raf)
      raf = requestAnimationFrame(() => {
        const sections = [...root.querySelectorAll('[data-nav-section]')]
        if (!sections.length) {
          setActiveNav(NAV_KEYS[0])
          return
        }
        const headerH =
          typeof document !== 'undefined'
            ? (document.querySelector('.site-header')?.getBoundingClientRect().height ?? 52)
            : 52
        const viewportSlice = Math.max(0, window.innerHeight - headerH)
        const focal = headerH + viewportSlice * 0.26
        let bestKey = NAV_KEYS[0]
        let bestDelta = Infinity
        sections.forEach((node) => {
          const rect = node.getBoundingClientRect()
          if (rect.bottom < headerH + 70 || rect.top > window.innerHeight) return
          const mid = rect.top + rect.height * 0.35
          const d = Math.abs(mid - focal)
          if (d < bestDelta) {
            bestDelta = d
            bestKey = node.getAttribute('data-nav-section')
          }
        })
        setActiveNav(bestKey)
      })
    }

    tick()
    window.addEventListener('scroll', tick, { passive: true })
    const ro = typeof ResizeObserver !== 'undefined' ? new ResizeObserver(tick) : null
    if (ro) ro.observe(root)
    window.addEventListener('resize', tick)

    const mo = typeof MutationObserver !== 'undefined'
      ? new MutationObserver(tick)
      : null
    if (mo) mo.observe(root, { childList: true, subtree: true })

    return () => {
      cancelAnimationFrame(raf)
      window.removeEventListener('scroll', tick)
      window.removeEventListener('resize', tick)
      ro?.disconnect()
      mo?.disconnect()
    }
  }, [searched, data, loading, error])

  const navigateSection = useCallback((key) => {
    scrollToNavSection(key)
    setActiveNav(key)
  }, [])

  return (
    <div className="app-shell">
      <Sidebar activeNav={activeNav} onNavClick={navigateSection} />
      <div className="main-area">
        <header className="site-header">
          <span className="header-brand">DevSignal</span>
          <div className="header-sep" />
          <span className="header-tagline">GitHub Profile Analyzer</span>
        </header>
        <main ref={contentRef} className="content">
          {/* ── Hero / Home section ── */}
          <div id="section-home" data-nav-section="home" className="report-section-anchor content-hero-block">
            <SearchBar onSearch={handleSearch} loading={loading} />
          </div>

          {/* ── Before-search landing content ── */}
          {showPreview ? <LandingSections /> : null}

          {loading && (
            <div className="state-box">
              <div className="spinner" />
              <p>Analyzing GitHub profile&hellip;</p>
            </div>
          )}

          {error && !loading && (
            <div className="state-box error-state">
              <span className="state-icon">⚠</span>
              <p>{error}</p>
            </div>
          )}

          {data && !loading ? <AnalysisReport data={data} /> : null}
        </main>
      </div>
    </div>
  )
}
