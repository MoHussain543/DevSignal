import { Link } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'

export default function ReportPageFooter() {
  return (
    <footer className="report-page-footer">
      <Link to="/" className="btn-report-home report-footer-home-btn">
        <ArrowLeft size={15} strokeWidth={2} aria-hidden />
        Back to Home
      </Link>
    </footer>
  )
}
