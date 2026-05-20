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
  Sparkles,
  Target,
  TrendingUp,
  UserCircle2,
} from 'lucide-react'
import MinimalSiteHeader from '../components/MinimalSiteHeader.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import { supabase, supabaseConfigured } from '../lib/supabase.js'

function normalizeUsername(value) {
  return value.trim().replace(/^@/, '').toLowerCase()
}

function formatDateTime(value) {
  if (!value) return 'Not analyzed yet'
  return new Date(value).toLocaleString()
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

function StatCard({ label, value, muted = false }) {
  return (
    <div className="card my-profile-stat-card">
      <span className="my-profile-stat-label">{label}</span>
      <span className={`my-profile-stat-value ${muted ? 'my-profile-stat-value--text' : ''}`}>
        {value}
      </span>
    </div>
  )
}

function ActionCard({ icon: Icon, title, description, to, disabled = false, tone = 'violet' }) {
  const content = (
    <>
      <div className={`my-profile-action-icon my-profile-action-icon--${tone}`} aria-hidden>
        <Icon size={18} strokeWidth={1.8} />
      </div>
      <div className="my-profile-action-copy">
        <div className="my-profile-action-title">{title}</div>
        <p className="my-profile-action-desc">{description}</p>
      </div>
      <ArrowRight className="my-profile-action-arrow" size={15} strokeWidth={1.8} aria-hidden />
    </>
  )

  if (disabled) {
    return (
      <div className="card my-profile-action-card is-disabled">
        {content}
      </div>
    )
  }

  return (
    <Link to={to} className="card my-profile-action-card">
      {content}
    </Link>
  )
}

function HighlightCard({ eyebrow, title, body, tone = 'violet' }) {
  return (
    <div className={`card my-profile-highlight-card my-profile-highlight-card--${tone}`}>
      <span className="my-profile-highlight-eyebrow">{eyebrow}</span>
      <h3 className="my-profile-highlight-title">{title}</h3>
      <p className="my-profile-highlight-body">{body}</p>
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

  const overallRead =
    latestAiReport?.overallRead ||
    latestAnalysis?.candidateLevel ||
    'No saved portfolio read yet'

  const nextMove =
    latestAiReport?.topPriorities?.[0]?.action ||
    latestRoadmap?.quickWins?.[0]?.action ||
    'Run the AI report or roadmap to generate your next best move.'

  const aiSnapshot =
    latestAiReport?.overallSummary ||
    latestAiReport?.hiringImpression ||
    'No saved AI report summary yet.'

  const roadmapSnapshot =
    latestRoadmap?.highestImpactChange ||
    latestRoadmap?.roadmapSummary ||
    'No saved roadmap yet.'

  const resolvedProfileName =
    (profile?.display_name && profile.display_name.trim()) ||
    (profile?.github_username ? `@${profile.github_username}` : null) ||
    user?.email

  const displayNameHint = nextDisplayNameChangeAt && nextDisplayNameChangeAt > new Date()
    ? `Can be changed again after ${formatShortDate(nextDisplayNameChangeAt)}`
    : 'You can change this once every 7 days'

  return (
    <div className="app-shell theme-analyzer">
      <div className="main-area">
        <MinimalSiteHeader />

        <main className="my-profile-main">
          <div className="my-profile-shell">
            <section className="card card-gradient-edge my-profile-hero">
              <div className="my-profile-hero-top">
                <div className="my-profile-user">
                  <span className="my-profile-avatar" aria-hidden>
                    <UserCircle2 size={30} strokeWidth={1.6} />
                  </span>
                  <div>
                    <div className="my-profile-eyebrow">
                      <Sparkles size={12} strokeWidth={1.8} aria-hidden />
                      My DevSignal
                    </div>
                    <h1 className="my-profile-title">{resolvedProfileName}</h1>
                    <p className="my-profile-email">{user?.email}</p>
                    <p className="my-profile-sub">
                      Your saved DevSignal home for one linked GitHub profile, your latest score, and
                      the most important takeaways from your most recent analysis.
                    </p>
                  </div>
                </div>

                <button type="button" className="btn-outline my-profile-signout" onClick={handleSignOut}>
                  <LogOut size={14} strokeWidth={1.8} aria-hidden />
                  Sign out
                </button>
              </div>

              <div className="my-profile-hero-summary">
                <div className="my-profile-hero-score">
                  <span className="my-profile-hero-score-label">Current score</span>
                  <div className="my-profile-hero-score-row">
                    <span className="my-profile-hero-score-value">
                      {loading ? '…' : latestScore ?? '—'}
                    </span>
                    <span className="my-profile-hero-score-denom">/100</span>
                  </div>
                </div>

                <div className="my-profile-hero-statuses">
                  <div className="my-profile-status-chip">
                    <span className="my-profile-status-chip-label">Linked GitHub</span>
                    <span className="my-profile-status-chip-value">
                      {hasSavedUsername ? `@${profile.github_username}` : 'Not linked yet'}
                    </span>
                  </div>
                  <div className="my-profile-status-chip">
                    <span className="my-profile-status-chip-label">AI report</span>
                    <span className="my-profile-status-chip-value">
                      {aiAvailable ? 'Saved' : 'Not saved yet'}
                    </span>
                  </div>
                  <div className="my-profile-status-chip">
                    <span className="my-profile-status-chip-label">Roadmap</span>
                    <span className="my-profile-status-chip-value">
                      {roadmapAvailable ? 'Saved' : 'Not saved yet'}
                    </span>
                  </div>
                </div>
              </div>

              <form className="my-profile-link-form" onSubmit={handleSave}>
                {!supabaseConfigured ? (
                  <p className="auth-card-error">
                    Local account features are unavailable because the Supabase frontend environment variables are missing.
                  </p>
                ) : null}
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

                <div className="my-profile-link-actions">
                  <button type="submit" className="btn-primary" disabled={saving || !supabaseConfigured}>
                    <Sparkles size={14} strokeWidth={1.8} aria-hidden />
                    {saving ? 'Saving…' : 'Save linked profile'}
                  </button>

                  {hasSavedUsername ? (
                    <Link to={`/report/${encodeURIComponent(profile.github_username)}`} className="btn-outline">
                      <RefreshCcw size={14} strokeWidth={1.8} aria-hidden />
                      Refresh analysis
                    </Link>
                  ) : null}
                </div>

                {error ? <p className="auth-card-error">{error}</p> : null}
                {message ? <p className="auth-card-message">{message}</p> : null}
              </form>
            </section>

            <section className="my-profile-stats-grid">
              <StatCard label="Candidate level" value={loading ? 'Loading…' : latestCandidateLevel ?? 'Not saved yet'} muted />
              <StatCard label="Last analyzed" value={loading ? 'Loading…' : lastAnalyzedAt} muted />
              <StatCard label="Overall read" value={loading ? 'Loading…' : overallRead} muted />
            </section>

            <section className="my-profile-actions-grid">
              <ActionCard
                icon={LayoutDashboard}
                title="Scored report"
                description={hasSavedUsername ? 'Open the full scored analysis for your linked profile.' : 'Link a GitHub username first.'}
                to={hasSavedUsername ? `/report/${encodeURIComponent(profile.github_username)}` : '#'}
                disabled={!hasSavedUsername}
                tone="violet"
              />
              <ActionCard
                icon={Brain}
                title="AI report"
                description={hasSavedUsername ? 'Open your saved AI perspective or generate a fresh one.' : 'Link a GitHub username first.'}
                to={hasSavedUsername ? `/ai-report/${encodeURIComponent(profile.github_username)}` : '#'}
                disabled={!hasSavedUsername}
                tone="magenta"
              />
              <ActionCard
                icon={Goal}
                title="Roadmap"
                description={hasSavedUsername ? 'Open your improvement roadmap and next project direction.' : 'Link a GitHub username first.'}
                to={hasSavedUsername ? `/roadmap/${encodeURIComponent(profile.github_username)}` : '#'}
                disabled={!hasSavedUsername}
                tone="blue"
              />
            </section>

            <section className="my-profile-highlights-grid">
              <HighlightCard
                eyebrow="Best signal"
                title={bestSignal}
                body={featuredRepo?.name
                  ? `Your latest saved run currently points to ${featuredRepo.name} as a standout repo.`
                  : 'This is the strongest visible signal currently saved on your profile.'}
                tone="pos"
              />
              <HighlightCard
                eyebrow="Main gap"
                title={mainGap}
                body="This is the biggest issue currently softening how your GitHub profile comes across."
                tone="warn"
              />
              <HighlightCard
                eyebrow="Next move"
                title={nextMove}
                body="This is the most immediate high-signal action surfaced by your saved AI state."
                tone="violet"
              />
            </section>

            <section className="my-profile-duo-grid">
              <div className="card my-profile-snapshot-card my-profile-snapshot-card--magenta">
                <div className="my-profile-note-eyebrow">Saved AI report</div>
                <h3 className="my-profile-snapshot-title">
                  {aiAvailable ? 'Latest AI perspective is saved' : 'No saved AI perspective yet'}
                </h3>
                <p className="my-profile-snapshot-body">{aiSnapshot}</p>
              </div>

              <div className="card my-profile-snapshot-card my-profile-snapshot-card--blue">
                <div className="my-profile-note-eyebrow">Saved roadmap</div>
                <h3 className="my-profile-snapshot-title">
                  {roadmapAvailable ? 'Latest roadmap is saved' : 'No saved roadmap yet'}
                </h3>
                <p className="my-profile-snapshot-body">{roadmapSnapshot}</p>
              </div>
            </section>

            <section className="card my-profile-note-card">
              <div className="my-profile-note-eyebrow">
                <Target size={12} strokeWidth={1.8} aria-hidden />
                Personalized snapshot
              </div>
              <p className="my-profile-note-text">
                This page now reflects what your latest saved DevSignal state says about you. Each time you
                re-run analysis, AI report, or roadmap while signed in, this hub updates automatically.
              </p>
              {hasSavedUsername ? (
                <Link to={`/report/${encodeURIComponent(profile.github_username)}`} className="my-profile-inline-link">
                  Open latest report
                  <TrendingUp size={13} strokeWidth={1.8} aria-hidden />
                </Link>
              ) : null}
            </section>
          </div>
        </main>
      </div>
    </div>
  )
}
