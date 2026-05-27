import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  ArrowRight,
  Brain,
  Github,
  Goal,
  LayoutDashboard,
  LogOut,
  RefreshCcw,
  Settings2,
  Sparkles,
  TrendingUp,
  UserCircle2,
} from 'lucide-react'
import MinimalSiteHeader from '../components/MinimalSiteHeader.jsx'
import { useAuth } from '../context/useAuth.js'
import { supabase, supabaseConfigured } from '../lib/supabase.js'

function normalizeUsername(value) {
  return value.trim().replace(/^@/, '').toLowerCase()
}

function formatDateTime(value) {
  if (!value) return 'Not analyzed yet'
  return new Date(value).toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })
}

function addDays(value, days) {
  const date = new Date(value)
  date.setDate(date.getDate() + days)
  return date
}

function formatShortDate(value) {
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
  }).format(value)
}

function StatusDot({ active }) {
  return (
    <span
      className={`my-profile-status-dot ${active ? 'my-profile-status-dot--on' : ''}`}
      aria-hidden
    />
  )
}

function Metric({ label, value, accent = false }) {
  return (
    <div className="my-profile-metric">
      <span className="my-profile-metric-label">{label}</span>
      <span className={`my-profile-metric-value ${accent ? 'my-profile-metric-value--accent' : ''}`}>
        {value}
      </span>
    </div>
  )
}

function QuickLink({ icon: Icon, title, description, to, disabled = false, tone = 'violet' }) {
  const inner = (
    <>
      <span className={`my-profile-quick-icon my-profile-quick-icon--${tone}`} aria-hidden>
        <Icon size={17} strokeWidth={1.75} />
      </span>
      <span className="my-profile-quick-copy">
        <span className="my-profile-quick-title">{title}</span>
        <span className="my-profile-quick-desc">{description}</span>
      </span>
      <ArrowRight className="my-profile-quick-arrow" size={15} strokeWidth={1.8} aria-hidden />
    </>
  )

  if (disabled) {
    return <div className="my-profile-quick-link is-disabled">{inner}</div>
  }

  return (
    <Link to={to} className={`my-profile-quick-link my-profile-quick-link--${tone}`}>
      {inner}
    </Link>
  )
}

function ReadoutRow({ icon: Icon, eyebrow, title, body, tone = 'violet' }) {
  return (
    <div className="flow-row my-profile-readout-row">
      <span className={`my-profile-readout-icon my-profile-readout-icon--${tone}`} aria-hidden>
        <Icon size={15} strokeWidth={1.85} />
      </span>
      <div className="my-profile-readout-copy">
        <span className="flow-section-eyebrow">{eyebrow}</span>
        <h3 className="my-profile-readout-title">{title}</h3>
        <p className="my-profile-readout-body">{body}</p>
      </div>
    </div>
  )
}

export default function MyProfilePage() {
  const navigate = useNavigate()
  const { user } = useAuth()

  const [profile, setProfile] = useState(null)
  const [displayName, setDisplayName] = useState('')
  const [githubUsername, setGithubUsername] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const hasSavedUsername = useMemo(
    () => !!profile?.github_username,
    [profile],
  )

  useEffect(() => {
    if (!user?.id || !supabase) {
      setLoading(false)
      return
    }
    let active = true

    async function loadProfile() {
      setLoading(true)
      setError('')

      const { data, error: queryError } = await supabase
        .from('user_profiles')
        .select('*')
        .eq('id', user.id)
        .maybeSingle()

      if (!active) return

      if (queryError) {
        setError(queryError.message || 'Could not load your DevSignal profile.')
      } else {
        setProfile(data)
        setDisplayName(data?.display_name ?? '')
        setGithubUsername(data?.github_username ?? '')
      }
      setLoading(false)
    }

    loadProfile()
    return () => {
      active = false
    }
  }, [user?.id])

  const handleSave = async (e) => {
    e.preventDefault()
    if (!user?.id || !supabase) {
      setError('Supabase auth is not configured in this local environment yet.')
      return
    }

    const trimmedDisplayName = displayName.trim()
    const trimmed = githubUsername.trim().replace(/^@/, '')
    if (!trimmed) {
      setError('Enter a GitHub username first.')
      return
    }

    const existingDisplayName = (profile?.display_name ?? '').trim()
    const displayNameChanged = trimmedDisplayName !== existingDisplayName
    const displayNameUpdatedAt = profile?.display_name_updated_at
    const nextAllowedDate = displayNameUpdatedAt ? addDays(displayNameUpdatedAt, 7) : null

    if (displayNameChanged && nextAllowedDate && nextAllowedDate > new Date()) {
      setError(`Display name can only be changed once a week. You can change it again after ${formatShortDate(nextAllowedDate)}.`)
      return
    }

    setSaving(true)
    setError('')
    setMessage('')

    const payload = {
      id: user.id,
      display_name: trimmedDisplayName || null,
      display_name_updated_at: displayNameChanged ? new Date().toISOString() : profile?.display_name_updated_at ?? null,
      github_username: trimmed,
      github_username_normalized: normalizeUsername(trimmed),
    }

    const { data, error: upsertError } = await supabase
      .from('user_profiles')
      .upsert(payload)
      .select('*')
      .single()

    if (upsertError) {
      setError(upsertError.message || 'Could not save your linked GitHub username.')
    } else {
      setProfile(data)
      setDisplayName(data.display_name ?? trimmedDisplayName)
      setGithubUsername(data.github_username ?? trimmed)
      setMessage(displayNameChanged
        ? 'Profile updated. Your display name is now locked for 7 days.'
        : 'Linked GitHub username saved.')
    }

    setSaving(false)
  }

  const handleSignOut = async () => {
    if (supabase) {
      await supabase.auth.signOut()
    }
    navigate('/', { replace: true })
  }

  const latestScore = profile?.latest_score ?? null
  const latestCandidateLevel = profile?.latest_candidate_level ?? null
  const lastAnalyzedAt = formatDateTime(profile?.last_analyzed_at)
  const displayNameUpdatedAt = profile?.display_name_updated_at ?? null
  const nextDisplayNameChangeAt = displayNameUpdatedAt ? addDays(displayNameUpdatedAt, 7) : null

  const latestAnalysis = profile?.latest_analysis_json ?? null
  const latestAiReport = profile?.latest_ai_report_json ?? null
  const latestRoadmap = profile?.latest_roadmap_json ?? null

  const analysisStrengths = latestAnalysis?.strengths ?? []
  const analysisWeaknesses = latestAnalysis?.weaknesses ?? []
  const featuredRepo = latestAnalysis?.featuredRepo ?? null

  const aiAvailable = latestAiReport?.available === true
  const roadmapAvailable = latestRoadmap?.available === true

  const bestSignal =
    latestAiReport?.bestSignal ||
    analysisStrengths[0] ||
    featuredRepo?.name ||
    'Run a fresh analysis to surface your strongest visible signal.'

  const mainGap =
    latestAiReport?.mainGap ||
    latestAiReport?.whatWeakens ||
    analysisWeaknesses[0] ||
    'No clear gap saved yet — run a fresh analysis to update this view.'

  const nextMove =
    latestAiReport?.topPriorities?.[0]?.action ||
    latestRoadmap?.quickWins?.[0]?.action ||
    'Run the AI report or roadmap to generate your next best move.'

  const resolvedProfileName =
    (profile?.display_name && profile.display_name.trim()) ||
    (profile?.github_username ? `@${profile.github_username}` : null) ||
    user?.email

  const displayNameHint = nextDisplayNameChangeAt && nextDisplayNameChangeAt > new Date()
    ? `Can be changed again after ${formatShortDate(nextDisplayNameChangeAt)}`
    : 'You can change this once every 7 days'

  const reportPath = hasSavedUsername
    ? `/report/${encodeURIComponent(profile.github_username)}`
    : null

  return (
    <div className="app-shell theme-analyzer">
      <div className="main-area">
        <MinimalSiteHeader />

        <main className="content my-profile-main">
          <div className="my-profile-shell">

            <div className="flow-doc my-profile-doc">
              <header className="my-profile-identity">
                <div className="my-profile-identity-main">
                  <span className="my-profile-avatar" aria-hidden>
                    <UserCircle2 size={28} strokeWidth={1.6} />
                  </span>
                  <div className="my-profile-identity-copy">
                    <span className="my-profile-eyebrow">
                      <Sparkles size={12} strokeWidth={1.8} aria-hidden />
                      My DevSignal
                    </span>
                    <h1 className="my-profile-title">{resolvedProfileName}</h1>
                    <p className="my-profile-email">{user?.email}</p>
                  </div>
                </div>
                <button type="button" className="btn-outline my-profile-signout" onClick={handleSignOut}>
                  <LogOut size={14} strokeWidth={1.8} aria-hidden />
                  Sign out
                </button>
              </header>

              <div className="my-profile-metrics-band">
                <div className="my-profile-score-panel">
                  <span className="my-profile-score-label">Current score</span>
                  <div className="my-profile-score-row">
                    <span className="my-profile-score-value">
                      {loading ? '…' : latestScore ?? '—'}
                    </span>
                    <span className="my-profile-score-denom">/100</span>
                  </div>
                  {latestCandidateLevel ? (
                    <span className="badge badge-violet my-profile-score-badge">
                      {latestCandidateLevel}
                    </span>
                  ) : null}
                </div>

                <div className="my-profile-metrics-grid">
                  <Metric
                    label="Linked GitHub"
                    value={hasSavedUsername ? `@${profile.github_username}` : 'Not linked'}
                    accent={hasSavedUsername}
                  />
                  <Metric
                    label="Last analyzed"
                    value={loading ? '…' : lastAnalyzedAt}
                  />
                  <div className="my-profile-metric my-profile-metric--status">
                    <span className="my-profile-metric-label">Saved reports</span>
                    <div className="my-profile-saved-row">
                      <span className="my-profile-saved-item">
                        <StatusDot active={!!reportPath} />
                        Scored
                      </span>
                      <span className="my-profile-saved-item">
                        <StatusDot active={aiAvailable} />
                        AI
                      </span>
                      <span className="my-profile-saved-item">
                        <StatusDot active={roadmapAvailable} />
                        Roadmap
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              <section className="flow-section my-profile-settings">
                <div className="flow-section-head">
                  <span className="flow-section-icon" aria-hidden>
                    <Settings2 size={16} strokeWidth={1.75} />
                  </span>
                  <div>
                    <span className="flow-section-eyebrow">Account</span>
                    <h2 className="flow-section-title">Linked profile</h2>
                  </div>
                </div>
                <p className="flow-section-hint">
                  Connect one GitHub username to save scores, AI reads, and roadmap state to this account.
                </p>

                <form className="my-profile-link-form" onSubmit={handleSave}>
                  {!supabaseConfigured ? (
                    <p className="auth-card-error">
                      Local account features are unavailable because the Supabase frontend environment variables are missing.
                    </p>
                  ) : null}

                  <div className="my-profile-form-grid">
                    <label className="auth-field">
                      <span className="auth-field-label">Display name</span>
                      <div className="auth-input-wrap">
                        <UserCircle2 size={15} strokeWidth={1.8} aria-hidden />
                        <input
                          type="text"
                          value={displayName}
                          onChange={(e) => setDisplayName(e.target.value)}
                          placeholder="How you want your profile to appear"
                          autoComplete="name"
                          spellCheck={false}
                        />
                      </div>
                      <span className="my-profile-field-note">{displayNameHint}</span>
                    </label>

                    <label className="auth-field">
                      <span className="auth-field-label">Linked GitHub username</span>
                      <div className="auth-input-wrap">
                        <Github size={15} strokeWidth={1.8} aria-hidden />
                        <input
                          type="text"
                          value={githubUsername}
                          onChange={(e) => setGithubUsername(e.target.value)}
                          placeholder="octocat"
                          autoComplete="off"
                          spellCheck={false}
                        />
                      </div>
                    </label>
                  </div>

                  <div className="my-profile-link-actions">
                    <button type="submit" className="btn-primary" disabled={saving || !supabaseConfigured}>
                      <Sparkles size={14} strokeWidth={1.8} aria-hidden />
                      {saving ? 'Saving…' : 'Save linked profile'}
                    </button>

                    {reportPath ? (
                      <Link to={reportPath} className="btn-outline">
                        <RefreshCcw size={14} strokeWidth={1.8} aria-hidden />
                        Refresh analysis
                      </Link>
                    ) : null}
                  </div>

                  {error ? <p className="auth-card-error">{error}</p> : null}
                  {message ? <p className="auth-card-message">{message}</p> : null}
                </form>
              </section>
            </div>

            <div className="my-profile-lower">
              <div className="flow-doc my-profile-readout">
                <section className="flow-section">
                  <div className="flow-section-head">
                    <span className="flow-section-icon flow-section-icon--trim" aria-hidden>
                      <TrendingUp size={16} strokeWidth={1.75} />
                    </span>
                    <div>
                      <span className="flow-section-eyebrow">Saved reading</span>
                      <h2 className="flow-section-title">What your profile is saying</h2>
                    </div>
                  </div>
                  <p className="flow-section-hint">
                    Pulled from your most recent saved analysis — refresh any report to update this view.
                  </p>

                  <div className="flow-rows my-profile-readout-rows">
                    <ReadoutRow
                      icon={TrendingUp}
                      eyebrow="Best signal"
                      title={bestSignal}
                      body={featuredRepo?.name
                        ? `Your latest run points to ${featuredRepo.name} as a standout repo.`
                        : 'The strongest visible signal currently saved on your profile.'}
                      tone="pos"
                    />
                    <ReadoutRow
                      icon={Brain}
                      eyebrow="Main gap"
                      title={mainGap}
                      body="The biggest issue softening how your GitHub profile comes across right now."
                      tone="warn"
                    />
                    <ReadoutRow
                      icon={Goal}
                      eyebrow="Next move"
                      title={nextMove}
                      body="The most immediate high-signal action from your saved AI state."
                      tone="violet"
                    />
                  </div>

                  {reportPath ? (
                    <div className="my-profile-readout-footer">
                      <Link to={reportPath} className="my-profile-inline-link">
                        Open latest scored report
                        <ArrowRight size={13} strokeWidth={1.8} aria-hidden />
                      </Link>
                    </div>
                  ) : null}
                </section>
              </div>

              <aside className="my-profile-quick-nav">
                <span className="my-profile-quick-nav-label">Your reports</span>
                <QuickLink
                  icon={LayoutDashboard}
                  title="Scored report"
                  description="Full portfolio score and repo breakdown."
                  to={reportPath ?? '#'}
                  disabled={!reportPath}
                  tone="violet"
                />
                <QuickLink
                  icon={Brain}
                  title="AI report"
                  description="Verdict, evidence, and top priorities."
                  to={hasSavedUsername ? `/ai-report/${encodeURIComponent(profile.github_username)}` : '#'}
                  disabled={!hasSavedUsername}
                  tone="ai"
                />
                <QuickLink
                  icon={Goal}
                  title="Roadmap"
                  description="Skills to learn and projects to build."
                  to={hasSavedUsername ? `/roadmap/${encodeURIComponent(profile.github_username)}` : '#'}
                  disabled={!hasSavedUsername}
                  tone="roadmap"
                />
              </aside>
            </div>

          </div>
        </main>
      </div>
    </div>
  )
}
