import { Sparkles, Target, TrendingUp, Zap } from 'lucide-react'
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
    improveFirst,
    biggestUnlock,
    unavailableReason,
  } = aiSummary

  return (
    <div className="card ai-summary-card card-gradient-edge-sm">
      <SectionHeading
        icon={Sparkles}
        tone="violet"
        title="AI Perspective"
        titleClassName="card-title--lead"
      />
      <p className="ai-summary-disclaimer text-muted">
        A strategic read of your scored report — not a second checklist. Tactical strengths and growth
        areas stay in the section below.
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
            {improveFirst ? (
              <div className="ai-narrative-action ai-narrative-action--first">
                <span className="ai-narrative-action-icon" aria-hidden>
                  <Zap size={15} strokeWidth={1.85} />
                </span>
                <div>
                  <h3 className="ai-narrative-label">Improve first</h3>
                  <p className="ai-narrative-text">{improveFirst}</p>
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
