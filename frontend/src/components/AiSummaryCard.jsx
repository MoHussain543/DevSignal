import { Sparkles, Target, TrendingUp } from 'lucide-react'
import SectionHeading from './SectionHeading.jsx'

function NarrativeSection({ label, children, variant = 'default' }) {
  if (!children) return null
  return (
    <div className={`ai-narrative-section ai-narrative-section--${variant}`}>
      <h3 className="ai-narrative-label">{label}</h3>
      <div className="ai-narrative-content">{children}</div>
    </div>
  )
}

export default function AiSummaryCard({ aiSummary }) {
  if (!aiSummary) {
    return null
  }

  const {
    available,
    overallSummary,
    hiringImpression,
    whatStandsOut,
    topPriorities,
    biggestUnlock,
    unavailableReason,
  } = aiSummary

  const firstPriority = topPriorities?.[0]

  return (
    <div className="card ai-summary-card card-gradient-edge-sm">
      <SectionHeading
        icon={Sparkles}
        tone="ai-report"
        title="AI Perspective"
        titleClassName="card-title--lead"
      />
      <p className="ai-summary-disclaimer text-muted">
        A strategic read of your scored report — open the full AI report for verdict, evidence, and priorities.
      </p>

      {!available ? (
        <p className="ai-summary-unavailable">{unavailableReason}</p>
      ) : (
        <div className="ai-narrative-body">
          {overallSummary ? (
            <p className="ai-narrative-lead">{overallSummary}</p>
          ) : null}

          {hiringImpression ? (
            <NarrativeSection label="Hiring impression" variant="impression">
              <p className="ai-narrative-text">{hiringImpression}</p>
            </NarrativeSection>
          ) : null}

          {whatStandsOut ? (
            <NarrativeSection label="What stands out" variant="standout">
              <p className="ai-narrative-text">{whatStandsOut}</p>
            </NarrativeSection>
          ) : null}

          <div className="ai-narrative-actions">
            {firstPriority?.action ? (
              <div className="ai-narrative-action ai-narrative-action--first">
                <span className="ai-narrative-action-icon" aria-hidden>
                  <Target size={15} strokeWidth={1.85} />
                </span>
                <div>
                  <h3 className="ai-narrative-label">Top priority</h3>
                  <p className="ai-narrative-text">{firstPriority.action}</p>
                </div>
              </div>
            ) : null}

            {biggestUnlock ? (
              <div className="ai-narrative-action ai-narrative-action--unlock">
                <span className="ai-narrative-action-icon" aria-hidden>
                  <Target size={15} strokeWidth={1.85} />
                </span>
                <div>
                  <h3 className="ai-narrative-label">Biggest unlock</h3>
                  <p className="ai-narrative-text">{biggestUnlock}</p>
                </div>
              </div>
            ) : null}
          </div>

          <p className="ai-narrative-footnote text-muted">
            <TrendingUp size={12} strokeWidth={1.75} aria-hidden className="ai-narrative-footnote-ico" />
            Generated from your existing analysis only — scores and repo metrics are unchanged.
          </p>
        </div>
      )}
    </div>
  )
}
