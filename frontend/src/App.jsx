import { BrowserRouter, Routes, Route } from 'react-router-dom'
import LandingPage from './pages/LandingPage.jsx'
import ReportPage from './pages/ReportPage.jsx'
import AiReportEntryPage from './pages/AiReportEntryPage.jsx'
import AiReportResultPage from './pages/AiReportResultPage.jsx'
import RoadmapEntryPage from './pages/RoadmapEntryPage.jsx'
import RoadmapResultPage from './pages/RoadmapResultPage.jsx'
import AuthPage from './pages/AuthPage.jsx'
import MyProfilePage from './pages/MyProfilePage.jsx'
import ProtectedRoute from './components/ProtectedRoute.jsx'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/"                      element={<LandingPage />} />
        <Route path="/auth"                  element={<AuthPage />} />
        <Route
          path="/me"
          element={(
            <ProtectedRoute>
              <MyProfilePage />
            </ProtectedRoute>
          )}
        />
        <Route path="/report/:username"      element={<ReportPage />} />
        <Route
          path="/ai-report"
          element={(
            <ProtectedRoute>
              <AiReportEntryPage />
            </ProtectedRoute>
          )}
        />
        <Route
          path="/ai-report/:username"
          element={(
            <ProtectedRoute>
              <AiReportResultPage />
            </ProtectedRoute>
          )}
        />
        <Route
          path="/roadmap"
          element={(
            <ProtectedRoute>
              <RoadmapEntryPage />
            </ProtectedRoute>
          )}
        />
        <Route
          path="/roadmap/:username"
          element={(
            <ProtectedRoute>
              <RoadmapResultPage />
            </ProtectedRoute>
          )}
        />
      </Routes>
    </BrowserRouter>
  )
}
