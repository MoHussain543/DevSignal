import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { LockKeyhole, Mail, Sparkles } from 'lucide-react'
import MinimalSiteHeader from '../components/MinimalSiteHeader.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import { supabase, supabaseConfigured } from '../lib/supabase.js'

const BENEFITS = [
  'Save your linked GitHub username',
  'Return to your latest DevSignal profile state',
  'Build toward a personal score and roadmap dashboard',
]

export default function AuthPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { isAuthenticated, user } = useAuth()

  const [mode, setMode] = useState('signin')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const redirectPath = useMemo(() => {
    const from = location.state?.from
    return typeof from === 'string' && from.startsWith('/') ? from : '/me'
  }, [location.state])

  useEffect(() => {
    if (isAuthenticated && user) {
      navigate(redirectPath, { replace: true })
    }
  }, [isAuthenticated, navigate, redirectPath, user])

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!supabase) {
      setError('Supabase auth is not configured in this local environment yet.')
      return
    }
    setSubmitting(true)
    setError('')
    setMessage('')

    try {
      const credentials = {
        email: email.trim(),
        password,
      }

      if (mode === 'signup') {
        const { error: signUpError, data } = await supabase.auth.signUp(credentials)
        if (signUpError) throw signUpError

        if (data.session) {
          setMessage('Account created. You are signed in now.')
          navigate(redirectPath, { replace: true })
        } else {
          setMessage('Account created. Check your email if confirmation is enabled.')
        }
      } else {
        const { error: signInError } = await supabase.auth.signInWithPassword(credentials)
        if (signInError) throw signInError
        navigate(redirectPath, { replace: true })
      }
    } catch (err) {
      setError(err.message || 'Authentication failed. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="app-shell theme-analyzer">
      <div className="main-area">
        <MinimalSiteHeader />

        <main className="auth-page-main">
          <div className="auth-page-glow" aria-hidden />

          <div className="auth-page-layout">
            <div className="auth-page-copy">
              <div className="auth-page-eyebrow">
                <Sparkles size={13} strokeWidth={1.8} aria-hidden />
                DevSignal Account
              </div>
              <h1 className="auth-page-headline">
                Save your latest DevSignal state and come back to it later.
              </h1>
              <p className="auth-page-sub">
                Sign in to connect one GitHub profile to your account. This is the foundation for
                a personal score, saved AI report state, and roadmap progress over time.
              </p>
              {!supabaseConfigured ? (
                <p className="auth-card-error auth-page-config-warning">
                  Local auth is unavailable because the Supabase frontend environment variables are missing.
                </p>
              ) : null}

              <ul className="auth-page-benefits">
                {BENEFITS.map((item) => (
                  <li key={item}>
                    <Sparkles size={13} strokeWidth={1.8} aria-hidden />
                    <span>{item}</span>
                  </li>
                ))}
              </ul>

              <Link to="/" className="btn-outline auth-page-home-link">
                Back to home
              </Link>
            </div>

            <div className="auth-page-form-col">
              <div className="card card-gradient-edge auth-card">
                <div className="auth-card-mode-toggle" role="tablist" aria-label="Authentication mode">
                  <button
                    type="button"
                    className={`auth-card-mode-btn ${mode === 'signin' ? 'is-active' : ''}`}
                    onClick={() => setMode('signin')}
                  >
                    Sign in
                  </button>
                  <button
                    type="button"
                    className={`auth-card-mode-btn ${mode === 'signup' ? 'is-active' : ''}`}
                    onClick={() => setMode('signup')}
                  >
                    Create account
                  </button>
                </div>

                <form className="auth-card-form" onSubmit={handleSubmit}>
                  <label className="auth-field">
                    <span className="auth-field-label">Email</span>
                    <div className="auth-input-wrap">
                      <Mail size={15} strokeWidth={1.8} aria-hidden />
                      <input
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        placeholder="you@example.com"
                        autoComplete="email"
                        required
                      />
                    </div>
                  </label>

                  <label className="auth-field">
                    <span className="auth-field-label">Password</span>
                    <div className="auth-input-wrap">
                      <LockKeyhole size={15} strokeWidth={1.8} aria-hidden />
                      <input
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        placeholder="At least 6 characters"
                        autoComplete={mode === 'signup' ? 'new-password' : 'current-password'}
                        required
                        minLength={6}
                      />
                    </div>
                  </label>

                  {error ? <p className="auth-card-error">{error}</p> : null}
                  {message ? <p className="auth-card-message">{message}</p> : null}

                  <button
                    type="submit"
                    className="btn-primary auth-submit-btn"
                    disabled={submitting || !supabaseConfigured}
                  >
                    <Sparkles size={15} strokeWidth={1.8} aria-hidden />
                    {submitting
                      ? mode === 'signup'
                        ? 'Creating account…'
                        : 'Signing in…'
                      : mode === 'signup'
                        ? 'Create account'
                        : 'Sign in'}
                  </button>
                </form>

                <p className="auth-card-note">
                  Uses Supabase Auth. Your latest DevSignal profile state will live in your own account row.
                </p>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  )
}
