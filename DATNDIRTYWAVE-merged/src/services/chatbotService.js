import axios from "axios"
import { resolveApiOrigin } from "../utils/apiOrigin"

const API_BASE = `${resolveApiOrigin()}`

export const sendChatMessage = (payload) =>
  axios.post(`${API_BASE}/api/chatbot/message`, payload)

export const getChatbotHealth = () =>
  axios.get(`${API_BASE}/api/chatbot/health`)

export const getChatbotStatus = (sessionCode) =>
  axios.get(`${API_BASE}/api/chatbot/status/${encodeURIComponent(sessionCode)}`)

export const getChatHistory = (sessionCode) =>
  axios.get(`${API_BASE}/api/chatbot/history/${encodeURIComponent(sessionCode)}`)

export const requestHumanSupport = (payload) =>
  axios.post(`${API_BASE}/api/chatbot/request-human`, payload)