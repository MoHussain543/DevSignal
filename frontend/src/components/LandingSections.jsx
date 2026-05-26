import { Link } from 'react-router-dom'
import RevealSection from './RevealSection.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import {
  Activity,
  AlertTriangle,
  BadgeCheck,
  BookOpen,
  CheckCircle2,
  Code2,
  FolderGit2,
  HelpCircle,
  Map,
  Search,
  Sparkles,
  Target,
  TrendingUp,
  Zap,
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

const AI_REPORT_BENEFITS = [
  { num: '01', text: 'Plain-English profile summary' },
  { num: '02', text: 'Strengths and weaknesses explained clearly' },
  { num: '03', text: 'Hiring-style impression' },
  { num: '04', text: 'Personalized next steps' },
]

const ROADMAP_BENEFITS = [
  { num: '01', text: 'Quick wins you can make this week' },
  { num: '02', text: 'Skills to learn based on your real gaps' },
  { num: '03', text: 'Tailored project ideas to improve hiring signal' },
  { num: '04', text: 'Realistic 3-month improvement plan' },
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
  const { isAuthenticated } = useAuth()
  const aiReportLink = isAuthenticated ? '/ai-report' : '/auth'
  const aiReportLinkState = isAuthenticated ? undefined : { from: '/ai-report' }
  const roadmapLink = isAuthenticated ? '/roadmap' : '/auth'
  const roadmapLinkState = isAuthenticated ? undefined : { from: '/roadmap' }

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
          <div className="landing-section-header landing-section-header--analyze">
            <span className="landing-section-eyebrow">
              <FolderGit2 size={12} strokeWidth={1.8} aria-hidden />
              Portfolio signals
            </span>
            <h2 id="landing-analyze-heading" className="landing-section-title">
              What we analyze
            </h2>
          </div>

          <div className="analyze-spectrum">
            <aside className="analyze-spectrum-aside" aria-hidden="false">
              <div className="analyze-spectrum-hub" aria-hidden>
                <span className="analyze-spectrum-hub-ring analyze-spectrum-hub-ring--outer" />
                <span className="analyze-spectrum-hub-ring analyze-spectrum-hub-ring--inner" />
                <div className="analyze-spectrum-hub-core">
                  <div className="analyze-spectrum-hub-copy">
                    <span className="analyze-spectrum-hub-num">6</span>
                    <span className="analyze-spectrum-hub-label">
                      <span>signal</span>
                      <span>reads</span>
                    </span>
                  </div>
                </div>
                {[0, 1, 2, 3, 4, 5].map((i) => (
                  <span
                    key={i}
                    className="analyze-spectrum-hub-dot"
                    style={{ '--hub-i': i }}
                  />
                ))}
              </div>

              <p className="analyze-spectrum-aside-lead">
                We read what GitHub already shows publicly — repos, READMEs, activity, and portfolio patterns.
              </p>
              <p className="analyze-spectrum-aside-note">
                No source download. No account connection. Each read below feeds one overall score.
              </p>

              <div className="analyze-spectrum-flow">
                <span>Public profile</span>
                <span className="analyze-spectrum-flow-arrow" aria-hidden>→</span>
                <span className="analyze-spectrum-flow-accent">Six reads</span>
                <span className="analyze-spectrum-flow-arrow" aria-hidden>→</span>
                <span>One score</span>
              </div>
            </aside>

            <ol className="analyze-spectrum-track">
              {ANALYZES_FEATURES.map(({ icon: Icon, title, desc, tone }, index) => (
                <li key={title} className="analyze-spectrum-item">
                  <div className="analyze-spectrum-rail" aria-hidden>
                    <span className="analyze-spectrum-rail-dot" />
                    {index < ANALYZES_FEATURES.length - 1 ? (
                      <span className="analyze-spectrum-rail-line" />
                    ) : null}
                  </div>

                  <div className="analyze-spectrum-item-body">
                    <span className="analyze-spectrum-item-index" aria-hidden>
                      {String(index + 1).padStart(2, '0')}
                    </span>
                    <span className={`analyze-spectrum-item-icon section-icon-slot ${TONE_CLASS[tone]}`}>
                      <Icon size={17} strokeWidth={1.75} aria-hidden />
                    </span>
                    <div className="analyze-spectrum-item-copy">
                      <h3 className="analyze-spectrum-item-title">{title}</h3>
                      <p className="analyze-spectrum-item-desc">{desc}</p>
                    </div>
                  </div>
                </li>
              ))}
            </ol>
          </div>
        </section>
      </RevealSection>

      {/* ── How It Works ── */}
      <RevealSection delay={40}>
        <section className="landing-block" aria-labelledby="landing-hiw-heading">
          <div className="landing-section-header landing-section-header--hiw">
            <span className="landing-section-eyebrow">
              <TrendingUp size={12} strokeWidth={1.8} aria-hidden />
              Simple flow
            </span>
            <h2 id="landing-hiw-heading" className="landing-section-title">
              How it works
            </h2>
          </div>

          <div className="hiw-flow">
            <p className="hiw-flow-lead">
              Paste one public GitHub username and get a finished portfolio readout. No account connection, no repo prep, and no installation required.
            </p>

            <div className="hiw-flow-lanes">
              {HOW_IT_WORKS.map(({ icon: Icon, step, label, title, desc, points }, index) => (
                <div
                  key={step}
                  className={`hiw-flow-lane hiw-flow-lane--${index + 1}`}
                >
                  <div className="hiw-flow-lane-rail" aria-hidden>
                    <span className="hiw-flow-lane-node">
                      <span className="hiw-flow-lane-step">{step}</span>
                    </span>
                    {index < HOW_IT_WORKS.length - 1 ? (
                      <span className="hiw-flow-lane-spine" />
                    ) : null}
                  </div>

                  <div className="hiw-flow-lane-content">
                    <div className="hiw-flow-lane-header">
                      <span className={`hiw-flow-lane-icon section-icon-slot section-icon-tone-${index === 0 ? 'violet' : index === 1 ? 'mag' : 'pos'}`}>
                        <Icon size={16} strokeWidth={1.85} aria-hidden />
                      </span>
                      <div className="hiw-flow-lane-meta">
                        <span className="hiw-flow-lane-label">{label}</span>
                        <h3 className="hiw-flow-lane-title">{title}</h3>
                      </div>
                    </div>
                    <p className="hiw-flow-lane-desc">{desc}</p>
                    <ul className="hiw-flow-lane-points">
                      {points.map((point) => (
                        <li key={point}>{point}</li>
                      ))}
                    </ul>
                  </div>
                </div>
              ))}
            </div>

            <div className="hiw-flow-terminal" aria-hidden>
              <span className="hiw-flow-terminal-prompt">$</span>
              <span className="hiw-flow-terminal-cmd">devsignal analyze</span>
              <span className="hiw-flow-terminal-arg">@username</span>
              <span className="hiw-flow-terminal-arrow">→</span>
              <span className="hiw-flow-terminal-out">report ready</span>
            </div>
          </div>
        </section>
      </RevealSection>

      {/* ── More features intro ── */}
      <RevealSection>
        <div id="section-beyond-score" className="landing-features-intro">
          <p className="landing-features-intro-kicker">Beyond the score</p>
          <div className="landing-features-intro-rule">
            <span className="landing-features-intro-dot landing-features-intro-dot--ai" />
            <span className="landing-features-intro-line" />
            <span className="landing-features-intro-dot landing-features-intro-dot--roadmap" />
          </div>
        </div>
      </RevealSection>

      {/* ── AI Report Summary ── */}
      <RevealSection>
        <section
          id="section-ai-summary"
          data-nav-section="ai-summary"
          className="landing-block feature-scope-ai-report"
          aria-labelledby="landing-ai-heading"
        >
          <div className="landing-feature-showcase">

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
              <ol className="landing-feature-benefits" aria-label="Feature highlights">
                {AI_REPORT_BENEFITS.map((item) => (
                  <li key={item.num}>
                    <span className="landing-feature-benefit-num" aria-hidden>{item.num}</span>
                    <span>{item.text}</span>
                  </li>
                ))}
              </ol>
              <Link
                to={aiReportLink}
                state={aiReportLinkState}
                className="btn-primary landing-feature-cta"
              >
                <Sparkles size={14} strokeWidth={1.8} aria-hidden />
                Try AI Report
              </Link>
            </div>

            <div className="landing-feature-mock-col">
              <div className="feature-visual feature-visual--ai-synth" aria-hidden>
                <div className="ai-synth-scene">
                  <div className="ai-synth-raw">
                    <span className="ai-synth-chip">spring-api</span>
                    <span className="ai-synth-chip">TypeScript</span>
                    <span className="ai-synth-chip ai-synth-chip--dim">12 public repos</span>
                    <div className="ai-synth-activity" aria-hidden>
                      <span />
                      <span />
                      <span />
                      <span />
                      <span className="ai-synth-activity--dim" />
                    </div>
                  </div>

                  <svg className="ai-synth-funnel" viewBox="0 0 420 360" preserveAspectRatio="none" aria-hidden>
                    <path d="M 68 72 Q 148 108, 210 132" />
                    <path d="M 168 68 Q 198 104, 210 132" />
                    <path d="M 292 76 Q 248 108, 210 132" />
                  </svg>

                  <div className="ai-synth-spark" aria-hidden>
                    <Sparkles size={15} strokeWidth={1.85} />
                  </div>

                  <div className="ai-synth-readout">
                    <div className="ai-synth-stats">
                      <div className="ai-synth-stat">
                        <span className="ai-synth-stat-label">Overall read</span>
                        <span className="ai-synth-stat-value">Promising</span>
                      </div>
                      <div className="ai-synth-stat">
                        <span className="ai-synth-stat-label">Hiring signal</span>
                        <span className="ai-synth-stat-value">Moderate</span>
                      </div>
                      <div className="ai-synth-stat">
                        <span className="ai-synth-stat-label">Main gap</span>
                        <span className="ai-synth-stat-value ai-synth-stat-value--warn">README depth</span>
                      </div>
                    </div>

                    <p className="ai-synth-lead">
                      Active backend work — stronger with clearer setup docs on your top repos.
                    </p>

                    <ul className="ai-synth-evidence">
                      <li className="ai-synth-evidence-item ai-synth-evidence-item--positive">
                        <CheckCircle2 size={12} strokeWidth={2} aria-hidden />
                        Steady project velocity
                      </li>
                      <li className="ai-synth-evidence-item ai-synth-evidence-item--warning">
                        <AlertTriangle size={12} strokeWidth={2} aria-hidden />
                        Thin documentation signal
                      </li>
                      <li className="ai-synth-evidence-item ai-synth-evidence-item--missing">
                        <HelpCircle size={12} strokeWidth={2} aria-hidden />
                        Missing test evidence
                      </li>
                    </ul>
                  </div>
                </div>
                <p className="feature-visual-caption">Raw GitHub signals, distilled into a clear read</p>
              </div>
            </div>

          </div>
        </section>
      </RevealSection>

      {/* ── AI Roadmap ── */}
      <RevealSection>
        <section
          id="section-roadmap"
          data-nav-section="roadmap"
          className="landing-block feature-scope-roadmap"
          aria-labelledby="landing-roadmap-heading"
        >
          <div className="landing-feature-showcase landing-feature-showcase--reverse">

            <div className="landing-feature-mock-col">
              <div className="feature-visual feature-visual--roadmap-trail" aria-hidden>
                <div className="roadmap-trail-scene">
                  <svg className="roadmap-trail-svg" viewBox="0 0 420 360" preserveAspectRatio="none">
                    <defs>
                      <linearGradient id="roadmap-trail-grad" x1="0%" y1="100%" x2="100%" y2="0%">
                        <stop offset="0%" stopColor="rgba(124, 58, 237, 0.2)" />
                        <stop offset="100%" stopColor="rgba(165, 180, 252, 0.5)" />
                      </linearGradient>
                    </defs>
                    <path
                      className="roadmap-trail-path-glow"
                      d="M 56 288 C 98 258, 128 218, 162 176 S 248 98, 302 64 S 342 40, 358 32"
                    />
                    <path
                      className="roadmap-trail-path"
                      d="M 56 288 C 98 258, 128 218, 162 176 S 248 98, 302 64 S 342 40, 358 32"
                    />
                    <circle className="roadmap-trail-dot" cx="56" cy="288" r="5" />
                    <circle className="roadmap-trail-dot" cx="162" cy="176" r="5" />
                    <circle className="roadmap-trail-dot roadmap-trail-dot--end" cx="358" cy="32" r="6" />
                  </svg>

                  <div className="roadmap-trail-node roadmap-trail-node--start">
                    <span className="roadmap-trail-node-pin" aria-hidden>
                      <Zap size={13} strokeWidth={2.2} />
                    </span>
                    <div className="roadmap-trail-node-copy-block">
                      <span className="roadmap-trail-node-phase">Week 1</span>
                      <span className="roadmap-trail-node-label">Quick wins</span>
                    </div>
                  </div>

                  <div className="roadmap-trail-node roadmap-trail-node--mid">
                    <span className="roadmap-trail-node-pin" aria-hidden>
                      <BookOpen size={13} strokeWidth={2.2} />
                    </span>
                    <div className="roadmap-trail-node-copy-block">
                      <span className="roadmap-trail-node-phase">Month 1</span>
                      <span className="roadmap-trail-node-label">TypeScript · Testing</span>
                    </div>
                  </div>

                  <div className="roadmap-trail-node roadmap-trail-node--end">
                    <span className="roadmap-trail-node-pin" aria-hidden>
                      <Target size={13} strokeWidth={2.2} />
                    </span>
                    <div className="roadmap-trail-node-copy-block">
                      <span className="roadmap-trail-node-phase">Month 3</span>
                      <span className="roadmap-trail-node-label">Ship flagship project</span>
                    </div>
                  </div>
                </div>
                <p className="feature-visual-caption">A step-by-step path, not another scorecard</p>
              </div>
            </div>

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
              <ol className="landing-feature-benefits" aria-label="Feature highlights">
                {ROADMAP_BENEFITS.map((item) => (
                  <li key={item.num}>
                    <span className="landing-feature-benefit-num" aria-hidden>{item.num}</span>
                    <span>{item.text}</span>
                  </li>
                ))}
              </ol>
              <Link
                to={roadmapLink}
                state={roadmapLinkState}
                className="btn-primary landing-feature-cta"
              >
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
