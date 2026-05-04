import { ref } from 'vue'

const toasts = ref([])
let toastId = 0
const DEFAULT_TOAST_DURATION = 5000
const SUCCESS_TOAST_DURATION = 3000
const DUPLICATE_TOAST_WINDOW_MS = 800
const recentToasts = new Map()
const cartToastByKey = new Map()

// { id -> { timerId: ReturnType<setTimeout>|null, remainingMs: number, startedAt: number } }
const toastTimers = new Map()

const toPositiveInt = (value, fallback = 0) => {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) return fallback
  return Math.max(0, Math.round(parsed))
}

const cleanupToastRefs = (id) => {
  if (!Number.isFinite(Number(id))) return
  for (const [key, mappedId] of cartToastByKey.entries()) {
    if (mappedId === id) cartToastByKey.delete(key)
  }
}

const scheduleToastRemoval = (id, duration) => {
  const safeDuration = Math.max(Number(duration || DEFAULT_TOAST_DURATION), 200)
  const current = toastTimers.get(id)
  if (current?.timerId != null) clearTimeout(current.timerId)
  const timerId = setTimeout(() => {
    const index = toasts.value.findIndex(t => t.id === id)
    if (index > -1) toasts.value.splice(index, 1)
    toastTimers.delete(id)
    cleanupToastRefs(id)
  }, safeDuration)
  toastTimers.set(id, { timerId, remainingMs: safeDuration, startedAt: Date.now() })
}

const buildCartToastKey = (payload = {}) => {
  const explicitKey = String(payload?.cartToastKey || '').trim()
  if (explicitKey) return explicitKey
  const productId = String(payload?.productId || '').trim()
  const color = String(payload?.color || '').trim().toLowerCase()
  const size = String(payload?.size || '').trim().toLowerCase()
  if (!productId) return ''
  return `${productId}__${color}__${size}`
}

export function useToast() {
  const showToast = (message, type = 'success', durationOrOptions = DEFAULT_TOAST_DURATION, maybeOptions = {}) => {
    const baseDuration = typeof durationOrOptions === 'number'
      ? durationOrOptions
      : DEFAULT_TOAST_DURATION
    const options = typeof durationOrOptions === 'object' && durationOrOptions !== null
      ? durationOrOptions
      : maybeOptions
    const duration = Number(options?.duration || baseDuration || DEFAULT_TOAST_DURATION)
    const onceKey = String(options?.onceKey || '').trim()
    const skipDuplicateCheck = Boolean(options?.skipDuplicateCheck)
    const duplicateKey = `${type}:${String(message || '').trim()}`
    const now = Date.now()

    if (onceKey) {
      try {
        if (sessionStorage.getItem(onceKey)) return null
        sessionStorage.setItem(onceKey, String(now))
      } catch {
        // Ignore storage errors and continue showing toast.
      }
    }

    if (!skipDuplicateCheck) {
      const lastShownAt = recentToasts.get(duplicateKey) || 0
      if (now - lastShownAt < DUPLICATE_TOAST_WINDOW_MS) {
        return null
      }
      recentToasts.set(duplicateKey, now)
    }

    const id = toastId++

    toasts.value.push({
      id,
      message,
      type,
      duration: Math.max(duration, 200),
      visible: true,
      variant: String(options?.variant || '').trim(),
      payload: options?.payload || null,
      action: options?.action || null
    })

    scheduleToastRemoval(id, duration)

    return id
  }

  const dismissToast = (id) => {
    const info = toastTimers.get(id)
    if (info?.timerId != null) clearTimeout(info.timerId)
    toastTimers.delete(id)
    const index = toasts.value.findIndex((t) => t.id === id)
    if (index > -1) toasts.value.splice(index, 1)
    cleanupToastRefs(id)
  }

  const pauseToast = (id) => {
    const info = toastTimers.get(id)
    if (!info || info.timerId == null) return
    clearTimeout(info.timerId)
    const elapsed = Date.now() - info.startedAt
    toastTimers.set(id, { timerId: null, remainingMs: Math.max(400, info.remainingMs - elapsed), startedAt: info.startedAt })
  }

  const resumeToast = (id) => {
    const info = toastTimers.get(id)
    if (!info || info.timerId != null) return
    scheduleToastRemoval(id, info.remainingMs)
  }

  const cartAdded = (payload = {}, duration = 5000) => {
    const addedQty = Math.max(1, toPositiveInt(payload?.addedQty, 1))
    const cartToastKey = buildCartToastKey(payload)
    const existingId = cartToastKey ? cartToastByKey.get(cartToastKey) : null
    if (existingId != null) {
      const index = toasts.value.findIndex((item) => item.id === existingId && item.variant === 'cart-add')
      if (index > -1) {
        const currentToast = toasts.value[index]
        const currentPayload = currentToast?.payload || {}
        const currentAddedTotal = Math.max(1, toPositiveInt(currentPayload?.addedQtyTotal || currentPayload?.addedQty, 1))
        const currentAddCount = Math.max(1, toPositiveInt(currentPayload?.addCount, 1))
        const mergedPayload = {
          ...currentPayload,
          ...payload,
          cartToastKey,
          addCount: currentAddCount + 1,
          addedQty: addedQty,
          addedQtyTotal: currentAddedTotal + addedQty,
          inCartQty: toPositiveInt(payload?.inCartQty, toPositiveInt(currentPayload?.inCartQty, 0))
        }
        toasts.value[index] = {
          ...currentToast,
          message: 'Thêm vào giỏ hàng thành công',
          payload: mergedPayload
        }
        scheduleToastRemoval(existingId, duration)
        return existingId
      }
      cartToastByKey.delete(cartToastKey)
    }

    const nextPayload = {
      ...payload,
      cartToastKey,
      addCount: 1,
      addedQty,
      addedQtyTotal: addedQty,
      inCartQty: toPositiveInt(payload?.inCartQty, 0)
    }
    const newId = showToast('Thêm vào giỏ hàng thành công', 'success', {
      duration,
      variant: 'cart-add',
      preventAside: true,
      payload: nextPayload,
      skipDuplicateCheck: true
    })
    if (newId != null && cartToastKey) cartToastByKey.set(cartToastKey, newId)
    return newId
  }

  return {
    toasts,
    showToast,
    dismissToast,
    pauseToast,
    resumeToast,
    cartAdded,
    success: (msg, durationOrOptions, maybeOptions) => {
      if (durationOrOptions === undefined) {
        return showToast(msg, 'success', SUCCESS_TOAST_DURATION, maybeOptions)
      }
      return showToast(msg, 'success', durationOrOptions, maybeOptions)
    },
    error: (msg, durationOrOptions, maybeOptions) => showToast(msg, 'error', durationOrOptions, maybeOptions),
    warning: (msg, durationOrOptions, maybeOptions) => showToast(msg, 'warning', durationOrOptions, maybeOptions),
    info: (msg, durationOrOptions, maybeOptions) => showToast(msg, 'info', durationOrOptions, maybeOptions)
  }
}
