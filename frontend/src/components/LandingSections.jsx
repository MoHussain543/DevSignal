import { Link } from 'react-router-dom'
import RevealSection from './RevealSection.jsx'
import {
  Activity,
  AlertTriangle,
  ArrowUpRight,
  BadgeCheck,
  BookOpen,
  CheckCircle2,
  Code2,
  FolderGit2,
  Map,
  Sparkles,
  Target,
  TrendingUp,
  ListChecks,
  Search,
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
    title: 'Technology variety',
    desc: 'Programming languages and stacks that show up across your public repositories.',
    tone: 'violet',
  },
  {
    icon: BookOpen,
    title: 'README & documentation',
    desc: 'Whether README files explain installation, usage, and what the project does.',
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
    icon: Search,
    step: '01',
    label: 'Input',
    title: 'Enter a GitHub username',
    desc: 'Use the search bar above. You do not need to create an account.',
    points: ['Paste any public GitHub username', 'No signup or setup required'],
  },
  {
    icon: FolderGit2,
    step: '02',
    label: 'Analysis',
    title: 'We review what is public',
    desc: 'DevSignal reads public profile and repository pages (such as README files) and prepares your report.',
    points: ['Checks repos, README files, activity, and portfolio signals', 'Uses what GitHub already exposes publicly'],
  },
  {
    icon: TrendingUp,
    step: '03',
    label: 'Output',
    title: 'Read your results',
    desc: 'See scores, charts, repository notes, strengths, and concrete suggestions on one page.',
    points: ['Get the scored report and feature-specific AI layers', 'See where the strongest signal and biggest gaps are'],
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

      {/* ── What DevSignal Analyzes ── */}
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

          <div className="hiw-shell">
            <div className="hiw-shell-head">
              <span className="hiw-shell-kicker">Simple flow</span>
              <p className="hiw-shell-copy">
                DevSignal turns one public username into a report without asking you to connect accounts,
                install anything, or prep your repositories first.
              </p>
            </div>

            <div className="hiw-steps">
              {HOW_IT_WORKS.map(({ icon: Icon, step, label, title, desc, points }) => (
                <div key={step} className="hiw-step">
                  <div className="hiw-step-top">
                    <span className="hiw-step-icon section-icon-slot section-icon-tone-mag">
                      <Icon size={16} strokeWidth={1.9} aria-hidden />
                    </span>
                    <div className="hiw-step-meta">
                      <span className="hiw-step-num">{step}</span>
                      <span className="hiw-step-label">{label}</span>
                    </div>
                  </div>
                  <div className="hiw-step-title">{title}</div>
                  <p className="hiw-step-desc">{desc}</p>
                  <ul className="hiw-step-points" aria-label={`${title} details`}>
                    {points.map((point) => (
                      <li key={point}>
                        <span className="hiw-step-point-dot" aria-hidden />
                        <span>{point}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              ))}
            </div>
          </div>
        </section>
      </RevealSection>

      {/* ── Coming Features divider ── */}
      <RevealSection>
        <div className="landing-coming-divider" aria-hidden>
          <div className="landing-coming-divider-line" />
          <span className="landing-coming-divider-label">
            <Sparkles size={11} strokeWidth={1.8} />
            Coming Features
          </span>
          <div className="landing-coming-divider-line" />
        </div>
      </RevealSection>

      {/* ── AI Report Summary (Coming Soon preview) ── */}
      <RevealSection>
        <section
          id="section-ai-summary"
          data-nav-section="ai-summary"
          className="landing-block feature-scope-ai-report"
          aria-labelledby="landing-ai-heading"
        >
          <div className="landing-feature-split">

            {/* Copy column */}
            <div className="landing-feature-copy">
              <div className="landing-section-eyebrow landing-section-eyebrow--live">
                <Sparkles size={12} strokeWidth={1.8} aria-hidden />
                Now Available
              </div>
              <h2 id="landing-ai-heading" className="landing-feature-title">
                AI Report Summary
              </h2>
              <p className="landing-feature-sub">
                Turn raw GitHub signals into a clear, human-readable portfolio review.
              </p>
              <ul className="landing-feature-bullets" aria-label="Feature highlights">
                <li><CheckCircle2 size={13} strokeWidth={2} aria-hidden /> Plain-English profile summary</li>
                <li><CheckCircle2 size={13} strokeWidth={2} aria-hidden /> Strengths and weaknesses explained clearly</li>
                <li><CheckCircle2 size={13} strokeWidth={2} aria-hidden /> Hiring-style impression</li>
                <li><CheckCircle2 size={13} strokeWidth={2} aria-hidden /> Personalized next steps</li>
              </ul>
              <Link to="/ai-report" className="btn-primary landing-feature-cta">
                <Sparkles size={14} strokeWidth={1.8} aria-hidden />
                Try AI Report
              </Link>
            </div>

            {/* Preview card column */}
            <div className="landing-preview-card-col">
              <div className="card card-gradient-edge-sm landing-preview-card">

                <div className="landing-preview-card-header">
                  <span className="section-icon-slot section-icon-tone-ai-report" aria-hidden>
                    <Sparkles size={16} strokeWidth={1.75} />
                  </span>
                  <span className="landing-preview-card-name">AI Report Summary</span>
                  <span className="badge badge-violet badge-ai-report">Preview</span>
                </div>

                <div className="landing-preview-quote">
                  <span className="landing-preview-quote-label">Overall Impression</span>
                  <p className="landing-preview-quote-text">
                    "This profile shows active project work and promising backend signals, but would
                    be stronger with clearer README files, setup instructions, and more polished
                    portfolio repositories."
                  </p>
                </div>

                <div className="landing-preview-mini-grid">
                  <div className="landing-preview-mini-item signal-item signal-green">
                    <CheckCircle2 size={12} strokeWidth={2} aria-hidden /> Main strengths identified
                  </div>
                  <div className="landing-preview-mini-item signal-item signal-yellow">
                    <AlertTriangle size={12} strokeWidth={2} aria-hidden /> Weak spots noted
                  </div>
                  <div className="landing-preview-mini-item landing-preview-mini-violet">
                    <ListChecks size={12} strokeWidth={2} aria-hidden /> What to fix first
                  </div>
                  <div className="landing-preview-mini-item landing-preview-mini-violet">
                    <ArrowUpRight size={12} strokeWidth={2} aria-hidden /> Hiring-style impression
                  </div>
                </div>

                <p className="landing-preview-card-note">Sample output — not real data</p>
              </div>
            </div>

          </div>
        </section>
      </RevealSection>

      {/* ── Improvement Roadmap (Coming Soon preview) ── */}
      <RevealSection>
        <section
          id="section-roadmap"
          data-nav-section="roadmap"
          className="landing-block feature-scope-roadmap"
          aria-labelledby="landing-roadmap-heading"
        >
          <div className="landing-feature-split landing-feature-split--reverse">

            {/* Preview card column (left on desktop) */}
            <div className="landing-preview-card-col">
              <div className="card card-gradient-edge-sm landing-preview-card">

                <div className="landing-preview-card-header">
                  <span className="section-icon-slot section-icon-tone-roadmap" aria-hidden>
                    <Map size={16} strokeWidth={1.75} />
                  </span>
                  <span className="landing-preview-card-name">Improvement Roadmap</span>
                  <span className="badge badge-violet badge-roadmap">Preview</span>
                </div>

                <div className="landing-roadmap-phases">

                  <div className="landing-roadmap-phase">
                    <div className="landing-roadmap-phase-header">
                      <Target size={13} strokeWidth={2} aria-hidden />
                      Quick Wins
                    </div>
                    <ul className="landing-roadmap-phase-list">
                      <li>Add setup instructions to your top repositories</li>
                      <li>Add screenshots to README files</li>
                      <li>Write clearer project descriptions</li>
                    </ul>
                  </div>

                  <div className="landing-roadmap-phase">
                    <div className="landing-roadmap-phase-header landing-roadmap-phase-header--impact">
                      <ArrowUpRight size={13} strokeWidth={2} aria-hidden />
                      Highest-Impact Fixes
                    </div>
                    <ul className="landing-roadmap-phase-list">
                      <li>Build one original full-stack project</li>
                      <li>Add tests or deployment notes</li>
                      <li>Improve documentation for your strongest repo</li>
                    </ul>
                  </div>

                  <div className="landing-roadmap-phase">
                    <div className="landing-roadmap-phase-header landing-roadmap-phase-header--direction">
                      <Map size={13} strokeWidth={2} aria-hidden />
                      Next Project Direction
                    </div>
                    <ul className="landing-roadmap-phase-list">
                      <li>Create a backend-heavy project with API, database, authentication, and deployment</li>
                    </ul>
                  </div>

                </div>

                <p className="landing-preview-card-note">Sample roadmap — not real data</p>
              </div>
            </div>

            {/* Copy column (right on desktop) */}
            <div className="landing-feature-copy">
              <div className="landing-section-eyebrow landing-section-eyebrow--live">
                <Map size={12} strokeWidth={1.8} aria-hidden />
                Now Available
              </div>
              <h2 id="landing-roadmap-heading" className="landing-feature-title">
                AI Roadmap
              </h2>
              <p className="landing-feature-sub">
                Build a clearer path to a stronger GitHub portfolio — tailored to your actual work.
              </p>
              <ul className="landing-feature-bullets" aria-label="Feature highlights">
                <li><CheckCircle2 size={13} strokeWidth={2} aria-hidden /> Quick wins you can make this week</li>
                <li><CheckCircle2 size={13} strokeWidth={2} aria-hidden /> Skills to learn based on your real gaps</li>
                <li><CheckCircle2 size={13} strokeWidth={2} aria-hidden /> Tailored project ideas to improve hiring signal</li>
                <li><CheckCircle2 size={13} strokeWidth={2} aria-hidden /> Realistic 3-month improvement plan</li>
              </ul>
              <Link to="/roadmap" className="btn-primary landing-feature-cta">
                <Map size={14} strokeWidth={1.8} aria-hidden />
                Generate My Roadmap
              </Link>
            </div>

          </div>
        </section>
      </RevealSection>

    </div>
  )
}
