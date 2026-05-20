const configuredApiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim()

export const apiBaseUrl = (configuredApiBaseUrl || 'http://localhost:8080').replace(/\/+$/, '')

export function apiUrl(path) {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${apiBaseUrl}${normalizedPath}`
}

export async function readApiError(response, fallbackMessage) {
  const fallback = fallbackMessage || `Error ${response.status}: ${response.statusText}`

  try {
    const body = await response.json()
    return body?.detail || body?.errorMessage || fallback
  } catch {
    return fallback
  }
}
