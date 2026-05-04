const trimTrailingSlash = (value) => String(value || '').trim().replace(/\/+$/, '')

const isLocalHost = (host) =>
  /^(localhost|127(?:\.\d+){3}|0\.0\.0\.0|::1)$/i.test(String(host || '').trim())

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
  const { hostname, origin } = window.location

  if (isLocalHost(hostname)) {
    return LOCAL_API_ORIGIN
  }

  const runtimeOverride = trimTrailingSlash(localStorage.getItem('dirtywave:api-origin'))
  if (isValidAbsoluteUrl(runtimeOverride)) {
    return runtimeOverride
  }

  const configured = trimTrailingSlash(import.meta.env.VITE_API_ORIGIN)
  if (isValidAbsoluteUrl(configured)) {
    return configured
  }

  // Production Vercel: gọi same-origin /api để vercel.json proxy qua backend
  return trimTrailingSlash(origin)
}