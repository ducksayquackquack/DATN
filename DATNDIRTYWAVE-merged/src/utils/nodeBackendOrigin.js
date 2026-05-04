const trimTrailingSlash = (value) => String(value || "").trim().replace(/\/+$/, "")

const isValidAbsoluteUrl = (value) => {
  const normalized = trimTrailingSlash(value)
  if (!normalized) return false
  try {
    const url = new URL(normalized)
    return url.protocol === "http:" || url.protocol === "https:"
  } catch {
    return false
  }
}

export const resolveNodeBackendOrigin = () => {
  const configured = trimTrailingSlash(import.meta.env.VITE_NODE_BACKEND_URL)
  if (isValidAbsoluteUrl(configured)) {
    return configured
  }

  if (typeof window !== "undefined") {
    const origin = trimTrailingSlash(window.location?.origin)
    const hostname = String(window.location?.hostname || "").trim().toLowerCase()
    if (/^(localhost|127(?:\.\d+){3}|0\.0\.0\.0|::1)$/i.test(hostname)) {
      return "http://localhost:3000"
    }

    // On public hosts, call Node-backed endpoints via same-origin rewrites.
    if (isValidAbsoluteUrl(origin)) {
      return origin
    }
  }

  return ""
}

export const hasNodeBackendOrigin = () => Boolean(resolveNodeBackendOrigin())