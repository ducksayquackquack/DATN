export const AUTH_CONTEXT_CHANGED_EVENT = 'auth-context-changed'

export const extractAccountList = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.data?.content)) return payload.data.content
  return []
}

export const normalizeRole = (role) => {
  return String(role || '')
    .trim()
    .toUpperCase()
    .replace(/^ROLE_/, '')
    .replace(/\s+/g, '_')
}

export const matchesRole = (role, expectedRoles = []) => {
  const expected = Array.isArray(expectedRoles) ? expectedRoles : [expectedRoles]
  const normalizedExpected = expected.map((item) => normalizeRole(item)).filter(Boolean)
  return normalizedExpected.includes(normalizeRole(role))
}

export const syncAuthStorage = (account, fallbackRole = '') => {
  if (typeof window === 'undefined' || !account?.id) return

  localStorage.setItem('userId', String(account.id))
  localStorage.setItem('userEmail', String(account.email || ''))
  localStorage.setItem('role', normalizeRole(fallbackRole || account?.vaiTro || ''))
}

export const dispatchAuthContextChanged = () => {
  if (typeof window === 'undefined') return
  window.dispatchEvent(new Event(AUTH_CONTEXT_CHANGED_EVENT))
}

export const resolveAccountByRole = async ({ service, expectedRoles, allowFallback = true }) => {
  if (typeof window === 'undefined') return null

  const userId = localStorage.getItem('userId')
  const userEmail = String(localStorage.getItem('userEmail') || '').trim().toLowerCase()

  if (userId) {
    try {
      const byId = await service.getById(userId)
      const account = byId?.data
      const roleMatch = matchesRole(account?.vaiTro, expectedRoles)
      const emailMatch = !userEmail || String(account?.email || '').trim().toLowerCase() === userEmail
      if (account?.id && roleMatch && emailMatch) {
        syncAuthStorage(account, expectedRoles[0])
        return account
      }
    } catch {
      // Continue with list fallback.
    }
  }

  const allRes = await service.getAll()
  const accounts = extractAccountList(allRes?.data)
  const matchedByEmail = accounts.find((item) => {
    return matchesRole(item?.vaiTro, expectedRoles)
      && String(item?.email || '').trim().toLowerCase() === userEmail
  }) || null

  if (matchedByEmail?.id) {
    syncAuthStorage(matchedByEmail, expectedRoles[0])
    return matchedByEmail
  }

  if (!allowFallback) return null

  const fallback = accounts.find((item) => matchesRole(item?.vaiTro, expectedRoles)) || null
  if (fallback?.id) {
    syncAuthStorage(fallback, expectedRoles[0])
    return fallback
  }

  return null
}