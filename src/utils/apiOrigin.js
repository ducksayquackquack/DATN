const trimTrailingSlash = (value) => String(value || '').trim().replace(/\/+$/, '')

const isLocalHost = (host) => /^(localhost|127(?:\.\d+){3}|0\.0\.0\.0|::1)$/i.test(String(host || '').trim())
const isTryCloudflareOrigin = (origin) => /\.trycloudflare\.com$/i.test(String(origin || '').trim())

const isLocalOrigin = (origin) => {
  const normalized = trimTrailingSlash(origin)
  if (!normalized) return false
  try {
    return isLocalHost(new URL(normalized).hostname)
  } catch {
    return false
  }
}

const LOCAL_API_ORIGIN = 'http://localhost:8080'
const STABLE_PUBLIC_API_ORIGIN = 'https://triumph-throat-passage-folders.trycloudflare.com'

export const resolveApiOrigin = () => {
  const { protocol, hostname, origin } = window.location

  // Local browser should always call local backend directly.
  if (isLocalHost(hostname)) return LOCAL_API_ORIGIN

  const openingFromPublicHost = !isLocalHost(hostname)

  const configured = trimTrailingSlash(import.meta.env.VITE_API_ORIGIN)
  if (configured && (!openingFromPublicHost || (!isLocalOrigin(configured) && !isTryCloudflareOrigin(configured)))) {
    return configured
  }

  const runtimeOverride = trimTrailingSlash(localStorage.getItem('dirtywave:api-origin'))
  if (runtimeOverride && (!openingFromPublicHost || (!isLocalOrigin(runtimeOverride) && !isTryCloudflareOrigin(runtimeOverride)))) {
    return runtimeOverride
  }

  // Production on Vercel must not depend on temporary trycloudflare tunnels.
  if (/\.vercel\.app$/i.test(String(hostname || '').trim())) {
    return STABLE_PUBLIC_API_ORIGIN
  }

  // When accessed through a public URL, call API via same-origin so the
  // tunnel forwards requests to Vite proxy -> local backend.
  return trimTrailingSlash(origin || `${protocol}//${hostname}`)
}