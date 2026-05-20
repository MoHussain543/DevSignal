import { createClient } from '@supabase/supabase-js'

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL
const supabasePublishableKey = import.meta.env.VITE_SUPABASE_PUBLISHABLE_KEY

export const supabaseConfigured = !!(supabaseUrl && supabasePublishableKey)

if (!supabaseConfigured) {
  console.warn('Supabase environment variables are missing. Auth features will be unavailable.')
}

export const supabase = supabaseConfigured
  ? createClient(supabaseUrl, supabasePublishableKey)
  : null
