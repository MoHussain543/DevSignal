import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Github, LogOut, RefreshCcw, Sparkles, UserCircle2 } from 'lucide-react'
import MinimalSiteHeader from '../components/MinimalSiteHeader.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import { supabase } from '../lib/supabase.js'

function normalizeUsername(value) {
  return value.trim().replace(/^@/, '').toLowerCase()
}

export default function MyProfilePage() {
  const navigate = useNavigate()
  const { user } = useAuth()

  const [profile, setProfile] = useState(null)
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
    if (!user?.id) return
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
    if (!user?.id) return

    const trimmed = githubUsername.trim().replace(/^@/, '')
    if (!trimmed) {
      setError('Enter a GitHub username first.')
      return
    }

    setSaving(true)
    setError('')
    setMessage('')

    const payload = {
      id: user.id,
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
      setGithubUsername(data.github_username ?? trimmed)
      setMessage('Linked GitHub username saved.')
    }

    setSaving(false)
  }

  const handleSignOut = async () => {
    await supabase.auth.signOut()
    navigate('/', { replace: true })
  }

  const latestScore = profile?.latest_score ?? null
  const latestCandidateLevel = profile?.latest_candidate_level ?? null
  const lastAnalyzedAt = profile?.last_analyzed_at
    ? new Date(profile.last_analyzed_at).toLocaleString()
    : 'Not analyzed yet'

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
                    <h1 className="my-profile-title">{user?.email}</h1>
                    <p className="my-profile-sub">
                      Save the GitHub username you want DevSignal to track as your current profile state.
                    </p>
                  </div>
                </div>

                <button type="button" className="btn-outline my-profile-signout" onClick={handleSignOut}>
                  <LogOut size={14} strokeWidth={1.8} aria-hidden />
                  Sign out
                </button>
              </div>

              <form className="my-profile-link-form" onSubmit={handleSave}>
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
                  <button type="submit" className="btn-primary" disabled={saving}>
                    <Sparkles size={14} strokeWidth={1.8} aria-hidden />
                    {saving ? 'Saving…' : 'Save linked profile'}
                  </button>

                  {hasSavedUsername ? (
                    <Link to={`/report/${encodeURIComponent(profile.github_username)}`} className="btn-outline">
                      <RefreshCcw size={14} strokeWidth={1.8} aria-hidden />
                      Run latest analysis
                    </Link>
                  ) : null}
                </div>

                {error ? <p className="auth-card-error">{error}</p> : null}
                {message ? <p className="auth-card-message">{message}</p> : null}
              </form>
            </section>

            <section className="my-profile-stats-grid">
              <div className="card my-profile-stat-card">
                <span className="my-profile-stat-label">Latest score</span>
                <span className="my-profile-stat-value">
                  {loading ? '…' : latestScore ?? '—'}
                </span>
              </div>

              <div className="card my-profile-stat-card">
                <span className="my-profile-stat-label">Candidate level</span>
                <span className="my-profile-stat-value my-profile-stat-value--text">
                  {loading ? 'Loading…' : latestCandidateLevel ?? 'Not saved yet'}
                </span>
              </div>

              <div className="card my-profile-stat-card">
                <span className="my-profile-stat-label">Last analyzed</span>
                <span className="my-profile-stat-value my-profile-stat-value--text">
                  {loading ? 'Loading…' : lastAnalyzedAt}
                </span>
              </div>
            </section>

            <section className="card my-profile-note-card">
              <div className="my-profile-note-eyebrow">Current scope</div>
              <p className="my-profile-note-text">
                This page sets up your account and linked GitHub username first. The next backend step
                will sync new analysis, AI report, and roadmap results directly into this saved profile state.
              </p>
            </section>
          </div>
        </main>
      </div>
    </div>
  )
}
