/**
 * Heading row with optional eyebrow — prefer omitting eyebrow when the title is self-explanatory.
 */
export default function SectionHeading({ icon: Icon, eyebrow, title, tone = 'violet', titleClassName = '' }) {
  return (
    <div className="section-heading-row">
      {Icon != null ? (
        <span className={`section-icon-slot section-icon-tone-${tone}`} aria-hidden="true">
          <Icon strokeWidth={1.75} size={18} className="section-heading-ico" />
        </span>
      ) : null}
      <div className="section-label-wrap">
        {eyebrow ? <span className="section-eyebrow">{eyebrow}</span> : null}
        <p className={`card-title${titleClassName ? ` ${titleClassName}` : ''}`}>{title}</p>
      </div>
    </div>
  )
}
