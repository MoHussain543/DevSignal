import {
  AlertTriangle,
  CheckCircle2,
  HelpCircle,
  ListOrdered,
  ScanSearch,
  Sparkles,
  Target,
} from 'lucide-react'

const VERDICT_TONES = {
  'Overall read': 'read',
  'Hiring signal': 'signal',
  'Main gap': 'gap',
  'Best signal': 'best',
}

function VerdictDial({ label, value }) {
  if (!value) return null
  const tone = VERDICT_TONES[label] || 'read'
  return (
    <div className={`pattern-dial pattern-dial--${tone}`}>
      <span className="pattern-dial-value">{value}</span>
      <span className="pattern-dial-label">{label}</span>
    </div>
  )
}

function InsightTile({ label, children, tone = 'neutral' }) {
  if (!children) return null
  return (
    <article className={`pattern-insight-tile pattern-insight-tile--${tone}`}>
      <span className="pattern-insight-tile-label">{label}</span>
      <p className="pattern-insight-tile-text">{children}</p>
    </article>
  )
}

function NarrativeStance({ label, children, variant = 'neutral' }) {
  if (!children) return null
  return (
    <div className={`pattern-stance pattern-stance--${variant}`}>
      <span className="pattern-stance-label">{label}</span>
      <p className="pattern-stance-text">{children}</p>
    </div>
  )
}

function EvidenceBand({ title, items, icon: Icon, tone = 'neutral' }) {
  if (!items?.length) return null
  return (
    <div className={`pattern-signal-band pattern-signal-band--${tone}`}>
      <h3 className="pattern-signal-band-head">
        <Icon size={14} strokeWidth={2} aria-hidden />
        {title}
      </h3>
      <div className="pattern-signal-chips">
        {items.map((item, i) => (
          <span key={i} className="pattern-signal-chip">{item}</span>
        ))}
      </div>
    </div>
  )
}

function PriorityCard({ rank, action, whyItMatters, visibleImprovement }) {
  if (!action) return null
  return (
    <article className="pattern-priority-card">
      <span className="pattern-priority-card-rank" aria-label={`Priority ${rank}`}>{rank}</span>
      <h3 className="pattern-priority-card-action">{action}</h3>
      {whyItMatters ? (
        <p className="pattern-priority-card-meta">
          <span className="pattern-priority-card-meta-label">Why it matters</span>
          {whyItMatters}
        </p>
      ) : null}
      {visibleImprovement ? (
        <p className="pattern-priority-card-meta pattern-priority-card-meta--github">
          <span className="pattern-priority-card-meta-label">Visible on GitHub</span>
          {visibleImprovement}
        </p>
      ) : null}
    </article>
  )
}

export default function AiReportContent({ ai }) {
  const hasHowReads =
    ai.howProfileReadsPattern ||
    ai.howProfileReadsDrivers ||
    ai.howProfileReadsHelps ||
    ai.howProfileReadsHoldsBack

  const hasEvidence =
    ai.positiveSignals?.length ||
    ai.warningSignals?.length ||
    ai.missingSignals?.length

  const hasVerdict =
    ai.overallRead || ai.hiringSignal || ai.mainGap || ai.bestSignal

  const hasOpening = ai.overallSummary || hasVerdict

  const hasNarrative =
    ai.hiringImpression || ai.whatStandsOut || ai.whatWeakens

  return (
    <div className="flow-doc flow-doc--ai ai-report-content pattern-layout">
      {hasOpening ? (
        <section className="flow-section flow-section--opening pattern-hero" aria-labelledby="ai-perspective-heading">
          <div className="pattern-hero-grid">
            <div className="pattern-hero-copy">
              <div className="flow-section-head">
                <span className="flow-section-icon" aria-hidden>
                  <Sparkles size={16} strokeWidth={1.75} />
                </span>
                <div>
                  <span className="flow-section-eyebrow" id="ai-perspective-heading">AI perspective</span>
                  <h2 className="flow-section-title">How DevSignal reads this profile</h2>
                </div>
              </div>
              {ai.overallSummary ? (
                <p className="pattern-hero-summary">{ai.overallSummary}</p>
              ) : null}
            </div>
            {hasVerdict ? (
              <div className="pattern-dial-grid" aria-label="Profile verdict">
                <VerdictDial label="Overall read" value={ai.overallRead} />
                <VerdictDial label="Hiring signal" value={ai.hiringSignal} />
                <VerdictDial label="Main gap" value={ai.mainGap} />
                <VerdictDial label="Best signal" value={ai.bestSignal} />
              </div>
            ) : null}
          </div>
        </section>
      ) : null}

      {hasHowReads ? (
        <section className="flow-section" aria-labelledby="ai-reads-heading">
          <div className="flow-section-head">
            <span className="flow-section-icon flow-section-icon--trim" aria-hidden>
              <ScanSearch size={16} strokeWidth={1.75} />
            </span>
            <div>
              <span className="flow-section-eyebrow">Interpretation</span>
              <h2 id="ai-reads-heading" className="flow-section-title">How this profile reads</h2>
            </div>
          </div>
          <p className="flow-section-hint">
            The pattern behind the score — not a repeat of category metrics.
          </p>
          <div className="pattern-insight-mosaic">
            <InsightTile label="Overall pattern" tone="violet">{ai.howProfileReadsPattern}</InsightTile>
            <InsightTile label="What drives the read" tone="indigo">{ai.howProfileReadsDrivers}</InsightTile>
            <InsightTile label="What helps" tone="positive">{ai.howProfileReadsHelps}</InsightTile>
            <InsightTile label="What holds it back" tone="caution">{ai.howProfileReadsHoldsBack}</InsightTile>
          </div>
        </section>
      ) : null}

      {hasNarrative ? (
        <section className="flow-section" aria-labelledby="ai-narrative-heading">
          <div className="flow-section-head">
            <span className="flow-section-icon" aria-hidden>
              <Sparkles size={16} strokeWidth={1.75} />
            </span>
            <div>
              <span className="flow-section-eyebrow">Narrative</span>
              <h2 id="ai-narrative-heading" className="flow-section-title">Current narrative</h2>
            </div>
          </div>
          <div className="pattern-stance-board">
            <NarrativeStance label="What stands out" variant="positive">{ai.whatStandsOut}</NarrativeStance>
            <NarrativeStance label="Hiring impression" variant="hero">{ai.hiringImpression}</NarrativeStance>
            <NarrativeStance label="What weakens the profile" variant="caution">{ai.whatWeakens}</NarrativeStance>
          </div>
        </section>
      ) : null}

      {hasEvidence ? (
        <section className="flow-section" aria-labelledby="ai-evidence-heading">
          <div className="flow-section-head">
            <span className="flow-section-icon flow-section-icon--trim" aria-hidden>
              <CheckCircle2 size={16} strokeWidth={1.75} />
            </span>
            <div>
              <span className="flow-section-eyebrow">Grounding</span>
              <h2 id="ai-evidence-heading" className="flow-section-title">Evidence found</h2>
            </div>
          </div>
          <p className="flow-section-hint">
            Signals from your analysis that support the narrative above.
          </p>
          <div className="pattern-signal-board">
            <EvidenceBand title="Positive signals" items={ai.positiveSignals} icon={CheckCircle2} tone="positive" />
            <EvidenceBand title="Warning signals" items={ai.warningSignals} icon={AlertTriangle} tone="warning" />
            <EvidenceBand title="Missing signals" items={ai.missingSignals} icon={HelpCircle} tone="missing" />
          </div>
        </section>
      ) : null}

      {ai.topPriorities?.length > 0 ? (
        <section className="flow-section" aria-labelledby="ai-priorities-heading">
          <div className="flow-section-head">
            <span className="flow-section-icon" aria-hidden>
              <ListOrdered size={16} strokeWidth={1.75} />
            </span>
            <div>
              <span className="flow-section-eyebrow">Action plan</span>
              <h2 id="ai-priorities-heading" className="flow-section-title">Top 3 priorities</h2>
            </div>
          </div>
          <p className="flow-section-hint">
            Near-term actions ranked by impact — start here for the fastest visible improvement.
          </p>
          <div className="pattern-priority-deck">
            {ai.topPriorities.map((p, i) => (
              <PriorityCard
                key={i}
                rank={i + 1}
                action={p.action}
                whyItMatters={p.whyItMatters}
                visibleImprovement={p.visibleImprovement}
              />
            ))}
          </div>
        </section>
      ) : null}

      {ai.biggestUnlock ? (
        <section className="flow-section pattern-unlock-banner" aria-labelledby="ai-unlock-heading">
          <div className="pattern-unlock-banner-inner">
            <span className="pattern-unlock-icon" aria-hidden>
              <Target size={20} strokeWidth={1.75} />
            </span>
            <div>
              <span className="flow-section-eyebrow">Strategic</span>
              <h2 id="ai-unlock-heading" className="flow-section-title">Biggest unlock</h2>
              <p className="pattern-unlock-text">{ai.biggestUnlock}</p>
              <p className="flow-section-hint pattern-unlock-hint">
                Longer-term leverage — distinct from the near-term priorities above.
              </p>
            </div>
          </div>
        </section>
      ) : null}
    </div>
  )
}
