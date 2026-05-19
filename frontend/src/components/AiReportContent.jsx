import {
  AlertTriangle,
  CheckCircle2,
  HelpCircle,
  ListOrdered,
  ScanSearch,
  Sparkles,
  Target,
} from 'lucide-react'

function VerdictChip({ label, value, variant }) {
  if (!value) return null
  return (
    <div className={`ai-verdict-chip ai-verdict-chip--${variant}`}>
      <span className="ai-verdict-chip-label">{label}</span>
      <span className="ai-verdict-chip-value">{value}</span>
    </div>
  )
}

function NarrativeCard({ label, children, variant }) {
  if (!children) return null
  return (
    <article className={`ai-narrative-card ai-narrative-card--${variant}`}>
      <h2 className="ai-narrative-card-label">{label}</h2>
      <p className="ai-narrative-card-body">{children}</p>
    </article>
  )
}

function EvidenceColumn({ title, items, variant, icon: Icon }) {
  if (!items?.length) return null
  return (
    <div className={`ai-evidence-col ai-evidence-col--${variant}`}>
      <h3 className="ai-evidence-col-title">
        <Icon size={14} strokeWidth={2} aria-hidden />
        {title}
      </h3>
      <ul className="ai-evidence-list">
        {items.map((item, i) => (
          <li key={i}>{item}</li>
        ))}
      </ul>
    </div>
  )
}

function PriorityCard({ rank, action, whyItMatters, visibleImprovement }) {
  if (!action) return null
  return (
    <article className="ai-priority-card">
      <span className="ai-priority-rank" aria-label={`Priority ${rank}`}>
        {rank}
      </span>
      <div className="ai-priority-content">
        <h3 className="ai-priority-action">{action}</h3>
        {whyItMatters ? (
          <p className="ai-priority-meta">
            <span className="ai-priority-meta-label">Why it matters</span>
            {whyItMatters}
          </p>
        ) : null}
        {visibleImprovement ? (
          <p className="ai-priority-meta ai-priority-meta--outcome">
            <span className="ai-priority-meta-label">Visible on GitHub</span>
            {visibleImprovement}
          </p>
        ) : null}
      </div>
    </article>
  )
}

function ReadBlock({ label, children }) {
  if (!children) return null
  return (
    <div className="ai-read-block">
      <h3 className="ai-read-block-label">{label}</h3>
      <p className="ai-read-block-text">{children}</p>
    </div>
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

  return (
    <div className="ai-report-content">
      {ai.overallSummary ? (
        <section className="ai-report-section ai-report-perspective" aria-labelledby="ai-perspective-heading">
          <div className="ai-report-perspective-inner">
            <div className="ai-report-section-eyebrow" id="ai-perspective-heading">
              <Sparkles size={14} strokeWidth={1.8} aria-hidden />
              AI Perspective
            </div>
            <p className="ai-report-perspective-lead">{ai.overallSummary}</p>
          </div>
        </section>
      ) : null}

      {(ai.overallRead || ai.hiringSignal || ai.mainGap || ai.bestSignal) ? (
        <section className="ai-report-section" aria-label="Profile verdict">
          <div className="ai-verdict-row">
            <VerdictChip label="Overall read" value={ai.overallRead} variant="read" />
            <VerdictChip label="Hiring signal" value={ai.hiringSignal} variant="signal" />
            <VerdictChip label="Main gap" value={ai.mainGap} variant="gap" />
            <VerdictChip label="Best signal" value={ai.bestSignal} variant="best" />
          </div>
        </section>
      ) : null}

      {hasHowReads ? (
        <section className="ai-report-section ai-report-reads" aria-labelledby="ai-reads-heading">
          <h2 id="ai-reads-heading" className="ai-report-section-title">
            <ScanSearch size={17} strokeWidth={1.75} aria-hidden />
            How this profile reads
          </h2>
          <p className="ai-report-section-intro">
            Interpretation of the pattern behind the score — not a repeat of category metrics.
          </p>
          <div className="ai-reads-grid">
            <ReadBlock label="Overall pattern">{ai.howProfileReadsPattern}</ReadBlock>
            <ReadBlock label="What drives the read">{ai.howProfileReadsDrivers}</ReadBlock>
            <ReadBlock label="What helps">{ai.howProfileReadsHelps}</ReadBlock>
            <ReadBlock label="What holds it back">{ai.howProfileReadsHoldsBack}</ReadBlock>
          </div>
        </section>
      ) : null}

      {(ai.hiringImpression || ai.whatStandsOut || ai.whatWeakens) ? (
        <section className="ai-report-section" aria-labelledby="ai-narrative-heading">
          <h2 id="ai-narrative-heading" className="ai-report-section-title">
            Current narrative
          </h2>
          <div className="ai-narrative-grid">
            <NarrativeCard label="Hiring impression" variant="impression">
              {ai.hiringImpression}
            </NarrativeCard>
            <NarrativeCard label="What stands out" variant="standout">
              {ai.whatStandsOut}
            </NarrativeCard>
            <NarrativeCard label="What weakens the profile" variant="weakens">
              {ai.whatWeakens}
            </NarrativeCard>
          </div>
        </section>
      ) : null}

      {hasEvidence ? (
        <section className="ai-report-section ai-report-evidence" aria-labelledby="ai-evidence-heading">
          <h2 id="ai-evidence-heading" className="ai-report-section-title">
            Evidence found
          </h2>
          <p className="ai-report-section-intro">
            Signals from your analysis that ground the narrative above.
          </p>
          <div className="ai-evidence-grid">
            <EvidenceColumn
              title="Positive signals"
              items={ai.positiveSignals}
              variant="positive"
              icon={CheckCircle2}
            />
            <EvidenceColumn
              title="Warning signals"
              items={ai.warningSignals}
              variant="warning"
              icon={AlertTriangle}
            />
            <EvidenceColumn
              title="Missing signals"
              items={ai.missingSignals}
              variant="missing"
              icon={HelpCircle}
            />
          </div>
        </section>
      ) : null}

      {ai.topPriorities?.length > 0 ? (
        <section className="ai-report-section ai-report-priorities" aria-labelledby="ai-priorities-heading">
          <h2 id="ai-priorities-heading" className="ai-report-section-title">
            <ListOrdered size={17} strokeWidth={1.75} aria-hidden />
            Top 3 priorities
          </h2>
          <p className="ai-report-section-intro">
            Near-term actions ranked by impact — start here for the fastest visible improvement.
          </p>
          <div className="ai-priorities-list">
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
        <section className="ai-report-section ai-report-unlock" aria-labelledby="ai-unlock-heading">
          <div className="ai-unlock-block">
            <div className="ai-unlock-icon" aria-hidden>
              <Target size={20} strokeWidth={1.75} />
            </div>
            <div>
              <h2 id="ai-unlock-heading" className="ai-unlock-title">Biggest unlock</h2>
              <p className="ai-unlock-body">{ai.biggestUnlock}</p>
              <p className="ai-unlock-hint">
                Strategic and longer-term — distinct from the near-term priorities above.
              </p>
            </div>
          </div>
        </section>
      ) : null}
    </div>
  )
}
