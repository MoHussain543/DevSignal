import { useId } from 'react'

const SIZE_MAP = {
  sm: { box: 22, className: 'devsignal-logo--sm' },
  md: { box: 48, className: 'devsignal-logo--md' },
  hero: { box: 280, className: 'devsignal-logo--hero' },
}

export default function DevSignalLogo({
  size = 'md',
  showWordmark = false,
  className = '',
}) {
  const uid = useId().replace(/:/g, '')
  const { box, className: sizeClass } = SIZE_MAP[size] ?? SIZE_MAP.md

  return (
    <div className={`devsignal-logo ${sizeClass} ${className}`.trim()}>
      <svg
        className="devsignal-logo-mark"
        viewBox="0 0 120 120"
        width={box}
        height={box}
        role="img"
        aria-label="DevSignal"
      >
        <defs>
          <linearGradient id={`${uid}-core`} x1="18%" y1="12%" x2="82%" y2="88%">
            <stop offset="0%" stopColor="#c084fc" />
            <stop offset="48%" stopColor="#a855f7" />
            <stop offset="100%" stopColor="#7c3aed" />
          </linearGradient>
          <linearGradient id={`${uid}-ring-a`} x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#8b5cf6" stopOpacity="0.95" />
            <stop offset="100%" stopColor="#a78bfa" stopOpacity="0.55" />
          </linearGradient>
          <linearGradient id={`${uid}-ring-b`} x1="100%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stopColor="#7c3aed" stopOpacity="0.85" />
            <stop offset="100%" stopColor="#8b5cf6" stopOpacity="0.45" />
          </linearGradient>
          <linearGradient id={`${uid}-wave`} x1="0%" y1="50%" x2="100%" y2="50%">
            <stop offset="0%" stopColor="#e9d5ff" stopOpacity="0.2" />
            <stop offset="50%" stopColor="#ddd6fe" stopOpacity="0.95" />
            <stop offset="100%" stopColor="#c4b5fd" stopOpacity="0.35" />
          </linearGradient>
          <filter id={`${uid}-glow`} x="-40%" y="-40%" width="180%" height="180%">
            <feGaussianBlur stdDeviation="2.4" result="blur" />
            <feMerge>
              <feMergeNode in="blur" />
              <feMergeNode in="SourceGraphic" />
            </feMerge>
          </filter>
        </defs>

        <circle className="devsignal-logo-orbit devsignal-logo-orbit--outer" cx="60" cy="60" r="46" stroke={`url(#${uid}-ring-b)`} />
        <circle className="devsignal-logo-orbit devsignal-logo-orbit--mid" cx="60" cy="60" r="34" stroke={`url(#${uid}-ring-a)`} />
        <circle className="devsignal-logo-orbit devsignal-logo-orbit--inner" cx="60" cy="60" r="22" />

        <g className="devsignal-logo-nodes">
          {[0, 60, 120, 180, 240, 300].map((deg, i) => {
            const rad = (deg * Math.PI) / 180
            const x = 60 + Math.cos(rad) * 46
            const y = 60 + Math.sin(rad) * 46
            return (
              <circle
                key={deg}
                className="devsignal-logo-node"
                style={{ '--node-i': i }}
                cx={x}
                cy={y}
                r="3.2"
              />
            )
          })}
        </g>

        <circle className="devsignal-logo-hub" cx="60" cy="60" r="15" fill={`url(#${uid}-core)`} filter={`url(#${uid}-glow)`} />

        <path
          className="devsignal-logo-wave"
          d="M38 60 L46 60 L50 48 L54 72 L58 54 L62 66 L66 58 L70 60 L78 60"
          fill="none"
          stroke={`url(#${uid}-wave)`}
          strokeWidth="2.6"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>

      {showWordmark && (
        <div className="devsignal-logo-wordmark" aria-hidden>
          <span className="devsignal-logo-wordmark-dev">Dev</span>
          <span className="devsignal-logo-wordmark-signal">Signal</span>
        </div>
      )}
    </div>
  )
}
