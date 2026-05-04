const AUTH_STORAGE_KEYS = new Set(['role', 'userId', 'userEmail'])

let authStorageProxyInstalled = false

const isAuthStorageKey = (key) => AUTH_STORAGE_KEYS.has(String(key || ''))

export const installScopedAuthStorage = () => {
  if (authStorageProxyInstalled || typeof window === 'undefined' || typeof Storage === 'undefined') {
    return
  }

  authStorageProxyInstalled = true

  const storageProto = Storage.prototype
  const originalGetItem = storageProto.getItem
  const originalSetItem = storageProto.setItem
  const originalRemoveItem = storageProto.removeItem

  storageProto.getItem = function patchedGetItem(key) {
    const normalizedKey = String(key || '')
    if (this === window.localStorage && isAuthStorageKey(normalizedKey)) {
      return originalGetItem.call(window.sessionStorage, normalizedKey)
    }
    return originalGetItem.call(this, key)
  }

  storageProto.setItem = function patchedSetItem(key, value) {
    const normalizedKey = String(key || '')
    if (this === window.localStorage && isAuthStorageKey(normalizedKey)) {
      originalSetItem.call(window.sessionStorage, normalizedKey, String(value))
      originalRemoveItem.call(window.localStorage, normalizedKey)
      return
    }
    originalSetItem.call(this, key, value)
  }

  storageProto.removeItem = function patchedRemoveItem(key) {
    const normalizedKey = String(key || '')
    if (this === window.localStorage && isAuthStorageKey(normalizedKey)) {
      originalRemoveItem.call(window.sessionStorage, normalizedKey)
      originalRemoveItem.call(window.localStorage, normalizedKey)
      return
    }
    originalRemoveItem.call(this, key)
  }

  AUTH_STORAGE_KEYS.forEach((key) => {
    originalRemoveItem.call(window.localStorage, key)
  })
}