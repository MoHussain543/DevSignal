/** Sidebar keys match data-nav-section / id prefix section- */
export const NAV_KEYS = ['home', 'analysis']

export function scrollToNavSection(navKey) {
  const el =
    typeof document !== 'undefined' ? document.getElementById(`section-${navKey}`) : null
  if (!el || typeof window === 'undefined') return

  const headerOffset =
    document.querySelector('.site-header')?.getBoundingClientRect().height ?? 52

  const rect = el.getBoundingClientRect()
  const targetTop = rect.top + window.scrollY - headerOffset - 12
  window.scrollTo({ top: Math.max(0, targetTop), behavior: 'smooth' })
}
