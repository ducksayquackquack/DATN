import { ref } from 'vue'

const toasts = ref([])
let toastId = 0
const DEFAULT_TOAST_DURATION = 5000
const SUCCESS_TOAST_DURATION = 3000
const DUPLICATE_TOAST_WINDOW_MS = 800
const recentToasts = new Map()

// { id -> { timerId: ReturnType<setTimeout>|null, remainingMs: number, startedAt: number } }
const toastTimers = new Map()

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

    const lastShownAt = recentToasts.get(duplicateKey) || 0
    if (now - lastShownAt < DUPLICATE_TOAST_WINDOW_MS) {
      return null
    }
    recentToasts.set(duplicateKey, now)

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

    const timerId = setTimeout(() => {
      const index = toasts.value.findIndex(t => t.id === id)
      if (index > -1) toasts.value.splice(index, 1)
      toastTimers.delete(id)
    }, duration)
    toastTimers.set(id, { timerId, remainingMs: duration, startedAt: Date.now() })

    return id
  }

  const dismissToast = (id) => {
    const info = toastTimers.get(id)
    if (info?.timerId != null) clearTimeout(info.timerId)
    toastTimers.delete(id)
    const index = toasts.value.findIndex((t) => t.id === id)
    if (index > -1) toasts.value.splice(index, 1)
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
    const timerId = setTimeout(() => {
      const index = toasts.value.findIndex(t => t.id === id)
      if (index > -1) toasts.value.splice(index, 1)
      toastTimers.delete(id)
    }, info.remainingMs)
    toastTimers.set(id, { ...info, timerId, startedAt: Date.now() })
  }

  const cartAdded = (payload = {}, duration = 5000) => {
    return showToast('Thêm vào giỏ hàng thành công', 'success', {
      duration,
      variant: 'cart-add',
      payload
    })
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
