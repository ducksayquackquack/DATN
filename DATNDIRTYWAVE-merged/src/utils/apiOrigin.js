const trimTrailingSlash = (value) => String(value || '').trim().replace(/\/+$/, '')

const isLocalHost = (host) => /^(localhost|127(?:\.\d+){3}|0\.0\.0\.0|::1)$/i.test(String(host || '').trim())
const getHostname = (value) => {
  try {
    return String(new URL(trimTrailingSlash(value)).hostname || '').trim().toLowerCase()
  } catch {
    return ''
  }
}
const isTryCloudflareOrigin = (value) => /\.trycloudflare\.com$/i.test(getHostname(value))
const isLocalOrigin = (value) => isLocalHost(getHostname(value))
const isValidAbsoluteUrl = (value) => {
  const normalized = trimTrailingSlash(value)
  if (!normalized) return false
  try {
    const url = new URL(normalized)
    return url.protocol === 'http:' || url.protocol === 'https:'
  } catch {
    return false
  }
}

const LOCAL_API_ORIGIN = 'http://localhost:8080'

export const resolveApiOrigin = () => {
  const { protocol, hostname, origin } = window.location
  const openingFromPublicHost = !isLocalHost(hostname)
  const runningOnVercel = /\.vercel\.app$/i.test(String(hostname || '').trim())

  // Local browser should always call local backend directly.
  if (isLocalHost(hostname)) return LOCAL_API_ORIGIN

  // Vercel production should stay same-origin so auth cookies/session work reliably.
  // Backend routing is handled by Vercel rewrites under /api.
  if (runningOnVercel) {
    localStorage.removeItem('dirtywave:api-origin')
    return trimTrailingSlash(origin)
  }

  const configured = trimTrailingSlash(import.meta.env.VITE_API_ORIGIN)
  if (isValidAbsoluteUrl(configured) && (!openingFromPublicHost || !isLocalOrigin(configured))) {
    return configured
  }

  const runtimeOverride = trimTrailingSlash(localStorage.getItem('dirtywave:api-origin'))
  if (isValidAbsoluteUrl(runtimeOverride) && (!openingFromPublicHost || !isLocalOrigin(runtimeOverride))) {
    return runtimeOverride
  }
  if (runtimeOverride && openingFromPublicHost) {
    // Prevent stale localhost values from poisoning production requests.
    if (isLocalOrigin(runtimeOverride)) localStorage.removeItem('dirtywave:api-origin')
  }

  // Public quick tunnel for local dev should use same-origin proxy.
  if (isTryCloudflareOrigin(origin)) {
    return trimTrailingSlash(origin)
  }

  return trimTrailingSlash(origin || `${protocol}//${hostname}`)
}