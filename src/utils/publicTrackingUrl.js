const RUNTIME_PUBLIC_ORIGIN_KEY = 'dirtywave:public-origin'

const normalizeBaseUrl = (value) => String(value || '').trim().replace(/\/+$/, '')

const normalizeOriginCandidate = (value) => {
  const raw = String(value || '').trim()
  if (!raw) return ''

  const withProtocol = /^https?:\/\//i.test(raw) ? raw : `https://${raw}`
  try {
    return normalizeBaseUrl(new URL(withProtocol).origin)
  } catch {
    return normalizeBaseUrl(raw)
  }
}

const isLocalHost = (host) => /^(localhost|127(?:\.\d+){3}|0\.0\.0\.0|::1)$/i.test(String(host || '').trim())

const isPrivateIpv4Host = (host) => {
  const value = String(host || '').trim()
  return /^(10\.|192\.168\.|172\.(1[6-9]|2\d|3[0-1])\.)/.test(value)
}

const isBlockedTunnelHost = (host) => {
  const value = String(host || '').trim().toLowerCase()
  return /(^|\.)ngrok\.io$/.test(value)
    || /(^|\.)ngrok-free\.app$/.test(value)
    || /(^|\.)ngrok-free\.dev$/.test(value)
}

const getHostFromUrl = (value) => {
  try {
    return new URL(value).hostname
  } catch {
    return ''
  }
}

export const isShareableOrigin = (origin) => {
  const normalized = normalizeOriginCandidate(origin)
  if (!normalized) return false
  const host = getHostFromUrl(normalized)
  if (!host) return false
  return !isLocalHost(host) && !isPrivateIpv4Host(host) && !isBlockedTunnelHost(host)
}

export const getRuntimePublicAppOrigin = () => {
  const runtimeRaw = localStorage.getItem(RUNTIME_PUBLIC_ORIGIN_KEY)
  const runtimeOverride = normalizeOriginCandidate(runtimeRaw)
  if (runtimeOverride && isBlockedTunnelHost(getHostFromUrl(runtimeOverride))) {
    localStorage.removeItem(RUNTIME_PUBLIC_ORIGIN_KEY)
    return ''
  }
  return isShareableOrigin(runtimeOverride) ? runtimeOverride : ''
}

export const setRuntimePublicAppOrigin = (origin) => {
  const normalized = normalizeOriginCandidate(origin)
  if (!isShareableOrigin(normalized)) return ''
  localStorage.setItem(RUNTIME_PUBLIC_ORIGIN_KEY, normalized)
  return normalized
}

export const clearRuntimePublicAppOrigin = () => {
  localStorage.removeItem(RUNTIME_PUBLIC_ORIGIN_KEY)
}

export const resolvePublicAppOrigin = () => {
  // Prefer the current browser origin when it's a stable public host,
  // since env/localStorage values can become stale when switching providers.
  const runtimeOrigin = normalizeOriginCandidate(window.location.origin)
  const runtimeHost = String(window.location.hostname || '').trim()

  if (!isLocalHost(runtimeHost) && !isPrivateIpv4Host(runtimeHost) && isShareableOrigin(runtimeOrigin)) {
    return runtimeOrigin
  }

  const configuredOrigin = normalizeOriginCandidate(import.meta.env.VITE_PUBLIC_APP_ORIGIN)
  if (isShareableOrigin(configuredOrigin)) return configuredOrigin

  const runtimeOverride = getRuntimePublicAppOrigin()
  if (isShareableOrigin(runtimeOverride)) return runtimeOverride

  return ''
}

export const buildOrderLookupTrackingUrl = ({ maHoaDon }) => {
  const ma = String(maHoaDon || '').trim()
  if (!ma) return ''

  const appOrigin = resolvePublicAppOrigin()
  if (!appOrigin) return ''

  return `${appOrigin}/dirtywave/tra-cuu-don-hang?${new URLSearchParams({ ma }).toString()}`
}