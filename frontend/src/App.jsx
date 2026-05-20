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
        <Route path="/ai-report"             element={<AiReportEntryPage />} />
        <Route path="/ai-report/:username"   element={<AiReportResultPage />} />
        <Route path="/roadmap"               element={<RoadmapEntryPage />} />
        <Route path="/roadmap/:username"     element={<RoadmapResultPage />} />
      </Routes>
    </BrowserRouter>
  )
}
