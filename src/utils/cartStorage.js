const LEGACY_CART_KEY = "cart"
const LEGACY_CHECKOUT_CART_KEY = "checkoutCart"
const LEGACY_CART_VARIANTS_KEY = "cartVariants"

const CART_KEY_PREFIX = "cart:"
const CHECKOUT_CART_KEY_PREFIX = "checkoutCart:"
const CART_VARIANTS_KEY_PREFIX = "cartVariants:"

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

const migrateLegacyGuestCart = (legacyKey, scopedStorageKey, emptyValue) => {
  if (getCurrentCartIdentity() !== "guest") return
  if (localStorage.getItem(scopedStorageKey) !== null) return

  const legacyRaw = localStorage.getItem(legacyKey)
  if (legacyRaw === null) return

  const parsed = safeJsonParse(legacyRaw, emptyValue)
  localStorage.setItem(scopedStorageKey, JSON.stringify(parsed))
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
  localStorage.setItem(key, JSON.stringify(safeValue))
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
  localStorage.setItem(key, JSON.stringify(Array.isArray(items) ? items : []))
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
  localStorage.setItem(key, JSON.stringify(safeValue))
}

export const clearCartVariantsObject = () => {
  localStorage.removeItem(scopedKey(CART_VARIANTS_KEY_PREFIX))
}
