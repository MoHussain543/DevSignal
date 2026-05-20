import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

export default function ProtectedRoute({ children }) {
  const { loading, isAuthenticated } = useAuth()
  const location = useLocation()

  if (loading) {
    return (
      <div className="auth-guard-shell">
        <div className="auth-guard-card">
          <div className="report-loading-ring-wrap" aria-hidden>
            <div className="report-loading-orb" />
            <div className="report-loading-ring" />
          </div>
          <p className="auth-guard-message">Checking your DevSignal session…</p>
        </div>
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/auth" replace state={{ from: location.pathname }} />
  }

  return children
}
