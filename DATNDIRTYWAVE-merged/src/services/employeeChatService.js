import axios from "axios"
import { resolveApiOrigin } from "../utils/apiOrigin"

const API_BASE = resolveApiOrigin().replace(/\/$/, "")

const API = `${API_BASE}/api/employee-chat`

const requestConfig = {
  withCredentials: true,
  timeout: 30000
}

export function getEmployeeChatSessions() {
  return axios.get(`${API}/sessions`, requestConfig)
}

export function getEmployeeChatMessages(sessionId) {
  return axios.get(`${API}/sessions/${sessionId}/messages`, requestConfig)
}

export function acceptEmployeeChatSession(sessionId, employeeId, employeeName) {
  return axios.post(
    `${API}/sessions/${sessionId}/accept`,
    {
      employeeId: Number(employeeId),
      employeeName: String(employeeName || "Nhân viên DirtyWave").trim()
    },
    requestConfig
  )
}

export function replyEmployeeChat(sessionId, payload) {
  return axios.post(
    `${API}/sessions/${sessionId}/reply`,
    {
      message: String(payload?.content || payload?.message || "").trim(),
      employeeName: String(payload?.senderName || payload?.employeeName || "").trim(),
      employeeId: String(payload?.senderId || payload?.employeeId || "").trim()
    },
    requestConfig
  )
}

export function closeEmployeeChatSession(sessionId) {
  return axios.post(`${API}/sessions/${sessionId}/close`, {}, requestConfig)
}