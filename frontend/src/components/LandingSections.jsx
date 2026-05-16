import RevealSection from './RevealSection.jsx'
import {
  Activity,
  BadgeCheck,
  BookOpen,
  Code2,
  FolderGit2,
  TrendingUp,
} from 'lucide-react'

const ANALYZES_FEATURES = [
  {
    icon: FolderGit2,
    title: 'Repository quality',
    desc: 'How polished each public repository looks: descriptions, README depth, stars, and overall completeness.',
    tone: 'violet',
  },
  {
    icon: Code2,
    title: 'Technical breadth',
    desc: 'Programming languages and frameworks that appear across your public repositories.',
    tone: 'violet',
  },
  {
    icon: BookOpen,
    title: 'Documentation',
    desc: 'Whether README files explain installation, usage, and structure in a helpful way.',
    tone: 'violet',
  },
  {
    icon: BadgeCheck,
    title: 'Original work',
    desc: 'How much reflects your own projects versus forks or tutorial-style copies.',
    tone: 'violet',
  },
  {
    icon: Activity,
    title: 'Recent activity',
    desc: 'Whether repositories show signs of ongoing updates when GitHub exposes that information.',
    tone: 'violet',
  },
  {
    icon: TrendingUp,
    title: 'Overall summary',
    desc: 'A single score with a short explanation, written the way a reviewer might summarize your profile.',
    tone: 'violet',
  },
]

const HOW_IT_WORKS = [
  {
    step: '01',
    title: 'Enter a GitHub username',
    desc: 'Use the search bar above. You do not need to create an account.',
  },
  {
    step: '02',
    title: 'We review what is public',
    desc: 'DevSignal reads public profile and repository pages (such as README files) and prepares your report.',
  },
  {
    step: '03',
    title: 'Read your results',
    desc: 'See scores, charts, repository notes, strengths, and concrete suggestions on one page.',
  },
]

const TONE_CLASS = {
  violet: 'section-icon-tone-violet',
  cyan:   'section-icon-tone-cyan',
  pos:    'section-icon-tone-pos',
  mag:    'section-icon-tone-mag',
  warm:   'section-icon-tone-warm',
}

export default function LandingSections() {
  return (
    <div className="landing-sections landing-sections--preview-offset">

      {/* ── What DevSignal Analyzes — sidebar “Analysis” scroll target before first search ── */}
      <RevealSection>
        <section
          id="section-analysis"
          data-nav-section="analysis"
          className="landing-block landing-analyze-anchor report-section-anchor"
          aria-labelledby="landing-analyze-heading"
        >
          <div className="landing-section-header">
            <h2 id="landing-analyze-heading" className="landing-section-title">
              What we analyze
            </h2>
            <p className="landing-section-sub">
              Six areas we look at using information GitHub already shows on profiles and repositories.
              Your source code is not downloaded.
            </p>
          </div>

          <div className="analyzes-grid">
            {ANALYZES_FEATURES.map(({ icon: Icon, title, desc, tone }) => (
              <div key={title} className="analyzes-card">
                <span className={`analyzes-card-icon section-icon-slot ${TONE_CLASS[tone]}`}>
                  <Icon size={18} strokeWidth={1.75} aria-hidden />
                </span>
                <div className="analyzes-card-title">{title}</div>
                <p className="analyzes-card-desc">{desc}</p>
              </div>
            ))}
          </div>
        </section>
      </RevealSection>

      {/* ── How It Works ── */}
      <RevealSection delay={40}>
        <section className="landing-block">
          <div className="landing-section-header">
            <h2 className="landing-section-title">How it works</h2>
            <p className="landing-section-sub">
              Three steps from username to a finished report.
            </p>
          </div>

          <div className="hiw-steps">
            {HOW_IT_WORKS.map(({ step, title, desc }) => (
              <div key={step} className="hiw-step">
                <div className="hiw-step-num">{step}</div>
                <div className="hiw-step-title">{title}</div>
                <p className="hiw-step-desc">{desc}</p>
              </div>
            ))}
          </div>
        </section>
      </RevealSection>

    </div>
  )
}
