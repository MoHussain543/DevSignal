import { Search, Terminal } from 'lucide-react'

export default function AnalyzeAnotherBar({ value, onChange, onSubmit }) {
  return (
    <form
      className="report-analyze-another"
      onSubmit={onSubmit}
      aria-label="Analyze another GitHub profile"
    >
      <Terminal size={14} strokeWidth={1.65} className="report-analyze-another-ico" aria-hidden />
      <input
        className="report-analyze-another-input"
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder="Analyze another username…"
        autoComplete="off"
        spellCheck={false}
        aria-label="GitHub username"
      />
      <button
        type="submit"
        className="report-analyze-another-btn"
        disabled={!value.trim()}
      >
        <Search size={14} strokeWidth={2} aria-hidden />
        Analyze
      </button>
    </form>
  )
}
