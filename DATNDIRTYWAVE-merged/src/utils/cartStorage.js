const LEGACY_CART_KEY = "cart"
const LEGACY_CHECKOUT_CART_KEY = "checkoutCart"
const LEGACY_CART_VARIANTS_KEY = "cartVariants"

const CART_KEY_PREFIX = "cart:"
const CHECKOUT_CART_KEY_PREFIX = "checkoutCart:"
const CART_VARIANTS_KEY_PREFIX = "cartVariants:"

const NON_CRITICAL_STORAGE_KEYS = [
  "checkoutSelectedVoucher",
  "checkoutItemVoucherSelections",
  "checkoutPendingItemVoucherSelections",
  "orderItemVoucherSnapshots",
  "pendingOfflineOrders",
  "currentOrder"
]

const safeJsonParse = (raw, fallback) => {
  try {
    const parsed = JSON.parse(raw)
    return parsed ?? fallback
  } catch {
    return fallback
  }
}

export const getCurrentCartIdentity = () => {
  const userId = String(localStorage.getItem("userId") || "").trim()
  const userEmail = String(localStorage.getItem("userEmail") || "").trim().toLowerCase()

  if (!userId && !userEmail) return "guest"
  return `${userId || "guest"}:${userEmail || "guest"}`
}

const scopedKey = (prefix) => `${prefix}${getCurrentCartIdentity()}`

const isQuotaExceeded = (error) => {
  if (!error) return false
  const name = String(error?.name || "")
  const code = Number(error?.code || 0)
  return name === "QuotaExceededError" || code === 22 || code === 1014
}

const sanitizeImageValue = (value = "") => {
  const raw = String(value || "").trim()
  if (!raw) return ""
  if (raw.startsWith("data:image/")) return ""
  if (raw.length > 1024) return ""
  return raw
}

const compactCheckoutItems = (items = []) => {
  if (!Array.isArray(items)) return []
  return items.map((item) => ({
    ...item,
    image: sanitizeImageValue(item?.image || "")
  }))
}

const compactCartVariants = (variants = {}) => {
  if (!variants || typeof variants !== "object" || Array.isArray(variants)) return {}
  return Object.fromEntries(
    Object.entries(variants).map(([key, value]) => {
      const row = value && typeof value === "object" ? value : {}
      return [key, {
        color: String(row?.color || "").trim(),
        size: String(row?.size || "").trim(),
        voucherCode: String(row?.voucherCode || "").trim().toUpperCase(),
        spctId: Number(row?.spctId || 0) || null,
        image: sanitizeImageValue(row?.image || "")
      }]
    })
  )
}

const compactValueForKey = (key, value) => {
  if (String(key || "").startsWith(CHECKOUT_CART_KEY_PREFIX)) {
    return compactCheckoutItems(value)
  }
  if (String(key || "").startsWith(CART_VARIANTS_KEY_PREFIX)) {
    return compactCartVariants(value)
  }
  return value
}

const removeOtherIdentityCartKeys = () => {
  const identity = getCurrentCartIdentity()
  const prefixes = [CART_KEY_PREFIX, CHECKOUT_CART_KEY_PREFIX, CART_VARIANTS_KEY_PREFIX]
  for (let i = localStorage.length - 1; i >= 0; i -= 1) {
    const key = localStorage.key(i)
    if (!key) continue
    const isScopedCartKey = prefixes.some((prefix) => key.startsWith(prefix))
    if (!isScopedCartKey) continue
    if (key.endsWith(identity)) continue
    localStorage.removeItem(key)
  }
}

const removeNonCriticalKeys = () => {
  for (const key of NON_CRITICAL_STORAGE_KEYS) {
    localStorage.removeItem(key)
  }
}

const safeSetJson = (key, value) => {
  const serialized = JSON.stringify(value)
  try {
    localStorage.setItem(key, serialized)
    return true
  } catch (error) {
    if (!isQuotaExceeded(error)) return false

    try {
      removeOtherIdentityCartKeys()
      removeNonCriticalKeys()
      localStorage.setItem(key, serialized)
      return true
    } catch (retryError) {
      if (!isQuotaExceeded(retryError)) return false
      try {
        const compacted = compactValueForKey(key, value)
        localStorage.setItem(key, JSON.stringify(compacted))
        return true
      } catch {
        return false
      }
    }
  }
}

const migrateLegacyGuestCart = (legacyKey, scopedStorageKey, emptyValue) => {
  if (getCurrentCartIdentity() !== "guest") return
  if (localStorage.getItem(scopedStorageKey) !== null) return

  const legacyRaw = localStorage.getItem(legacyKey)
  if (legacyRaw === null) return

  const parsed = safeJsonParse(legacyRaw, emptyValue)
  safeSetJson(scopedStorageKey, parsed)
}

export const readCartObject = () => {
  const key = scopedKey(CART_KEY_PREFIX)
  migrateLegacyGuestCart(LEGACY_CART_KEY, key, {})
  const parsed = safeJsonParse(localStorage.getItem(key) || "{}", {})
  return parsed && typeof parsed === "object" && !Array.isArray(parsed) ? parsed : {}
}

export const writeCartObject = (cartObj = {}) => {
  const key = scopedKey(CART_KEY_PREFIX)
  const safeValue = cartObj && typeof cartObj === "object" && !Array.isArray(cartObj) ? cartObj : {}
  return safeSetJson(key, safeValue)
}

export const clearCartObject = () => {
  localStorage.removeItem(scopedKey(CART_KEY_PREFIX))
}

export const readCheckoutCartArray = () => {
  const key = scopedKey(CHECKOUT_CART_KEY_PREFIX)
  migrateLegacyGuestCart(LEGACY_CHECKOUT_CART_KEY, key, [])
  const parsed = safeJsonParse(localStorage.getItem(key) || "[]", [])
  return Array.isArray(parsed) ? parsed : []
}

export const writeCheckoutCartArray = (items = []) => {
  const key = scopedKey(CHECKOUT_CART_KEY_PREFIX)
  return safeSetJson(key, Array.isArray(items) ? items : [])
}

export const clearCheckoutCartArray = () => {
  localStorage.removeItem(scopedKey(CHECKOUT_CART_KEY_PREFIX))
}

export const readCartVariantsObject = () => {
  const key = scopedKey(CART_VARIANTS_KEY_PREFIX)
  migrateLegacyGuestCart(LEGACY_CART_VARIANTS_KEY, key, {})
  const parsed = safeJsonParse(localStorage.getItem(key) || "{}", {})
  return parsed && typeof parsed === "object" && !Array.isArray(parsed) ? parsed : {}
}

export const writeCartVariantsObject = (variants = {}) => {
  const key = scopedKey(CART_VARIANTS_KEY_PREFIX)
  const safeValue = variants && typeof variants === "object" && !Array.isArray(variants) ? variants : {}
  return safeSetJson(key, safeValue)
}

export const clearCartVariantsObject = () => {
  localStorage.removeItem(scopedKey(CART_VARIANTS_KEY_PREFIX))
}
