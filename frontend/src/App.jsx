import { BrowserRouter, Routes, Route } from 'react-router-dom'
import LandingPage from './pages/LandingPage.jsx'
import ReportPage from './pages/ReportPage.jsx'
import AiReportEntryPage from './pages/AiReportEntryPage.jsx'
import AiReportResultPage from './pages/AiReportResultPage.jsx'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/"                      element={<LandingPage />} />
        <Route path="/report/:username"      element={<ReportPage />} />
        <Route path="/ai-report"             element={<AiReportEntryPage />} />
        <Route path="/ai-report/:username"   element={<AiReportResultPage />} />
      </Routes>
    </BrowserRouter>
  )
}
