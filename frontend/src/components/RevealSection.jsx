import { useEffect, useRef } from 'react'

/**
 * Entrance: fade + slide + subtle scale via CSS when .revealed is set.
 * While off-screen bottom: dormant state; exiting viewport optionally dims (off).
 */
export default function RevealSection({ children, delay = 0 }) {
  const ref = useRef(null)

  useEffect(() => {
    const el = ref.current
    if (!el) return

    const observer = new IntersectionObserver(
      ([entry]) => {
        el.classList.toggle('revealed', entry.isIntersecting)
        el.classList.toggle('reveal-soft', entry.isIntersecting ? false : entry.boundingClientRect.top > 0)
      },
      { threshold: [0, 0.06, 0.14], rootMargin: '0px 0px -8% 0px' },
    )

    observer.observe(el)
    return () => observer.disconnect()
  }, [])

  return (
    <div
      ref={ref}
      className="reveal motion-reveal"
      style={delay ? { transitionDelay: `${delay}ms` } : undefined}
    >
      {children}
    </div>
  )
}
