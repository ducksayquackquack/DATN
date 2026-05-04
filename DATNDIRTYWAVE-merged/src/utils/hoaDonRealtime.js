import { resolveApiOrigin } from "./apiOrigin"

// Node.js backend origin (port 3000) — used for WebSocket auto-derivation
const _resolveNodeOrigin = () => {
  const configured = String(import.meta.env.VITE_NODE_BACKEND_URL || "").trim().replace(/\/$/, "")
  if (configured) return configured
  try {
    const base = new URL(resolveApiOrigin())
    base.port = "3000"
    return base.origin
  } catch {
    return "http://localhost:3000"
  }
}

function toWebSocketUrl(origin, path = "/ws/hoa-don") {
  try {
    const url = new URL(String(origin || "").trim())
    url.protocol = url.protocol === "https:" ? "wss:" : "ws:"
    url.pathname = path
    url.search = ""
    return url.toString()
  } catch {
    return ""
  }
}

export function createHoaDonRealtimeChannel(options = {}) {
  const onMessage = typeof options.onMessage === "function" ? options.onMessage : () => {}
  const onState = typeof options.onState === "function" ? options.onState : () => {}
  const pollIntervalMs = Math.max(Number(options.pollIntervalMs || 15000), 3000)
  const reconnectDelayMs = Math.max(Number(options.reconnectDelayMs || 4000), 1000)
  const wsPath = String(options.wsPath || "/ws/hoa-don")

  const configuredWs = String(import.meta.env.VITE_HOADON_WS_URL || "").trim()
  const autoWs = String(import.meta.env.VITE_HOADON_WS_AUTO || "").trim().toLowerCase() === "true"
    ? toWebSocketUrl(_resolveNodeOrigin(), wsPath)
    : ""
  const wsUrl = configuredWs || autoWs

  let ws = null
  let stopped = false
  let reconnectTimer = null
  let pollTimer = null

  const emitState = (partial = {}) => {
    onState({
      connected: Boolean(ws && ws.readyState === WebSocket.OPEN),
      mode: wsUrl ? "websocket+polling" : "polling",
      lastEventAt: new Date().toISOString(),
      ...partial,
    })
  }

  const scheduleReconnect = () => {
    if (stopped || !wsUrl || reconnectTimer) return
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      connect()
    }, reconnectDelayMs)
  }

  const connect = () => {
    if (stopped || !wsUrl || typeof WebSocket === "undefined") return

    try {
      ws = new WebSocket(wsUrl)
      emitState({ connected: false })

      ws.onopen = () => {
        emitState({ connected: true })
      }

      ws.onmessage = (event) => {
        onMessage({ source: "ws", payload: event?.data })
        emitState({ connected: true })
      }

      ws.onerror = () => {
        emitState({ connected: false })
      }

      ws.onclose = () => {
        emitState({ connected: false })
        scheduleReconnect()
      }
    } catch {
      scheduleReconnect()
    }
  }

  const startPolling = () => {
    if (pollTimer) clearInterval(pollTimer)
    pollTimer = setInterval(() => {
      if (stopped) return
      onMessage({ source: "poll" })
      emitState({ connected: Boolean(ws && ws.readyState === WebSocket.OPEN) })
    }, pollIntervalMs)
  }

  const start = () => {
    stopped = false
    connect()
    startPolling()
    emitState({ connected: false })
  }

  const stop = () => {
    stopped = true
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
    if (ws) {
      try {
        ws.close()
      } catch {
        // no-op
      }
      ws = null
    }
  }

  return {
    start,
    stop,
    wsUrl,
  }
}
