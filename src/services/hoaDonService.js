import axios from "axios"
import { resolveApiOrigin } from "../utils/apiOrigin"

const API = `${resolveApiOrigin()}/api/hoa-don`

const normalizeHoaDonId = (id) => {
  const raw = String(id ?? "").trim()
  if (!raw) return null

  const lowered = raw.toLowerCase()
  if (["null", "undefined", "nan"].includes(lowered)) return null

  const parsed = Number(raw)
  if (!Number.isFinite(parsed) || parsed <= 0) return null
  return parsed
}

const ensureHoaDonId = (id) => {
  const normalized = normalizeHoaDonId(id)
  if (!normalized) {
    throw new Error("ID hóa đơn không hợp lệ")
  }
  return normalized
}

export const getAllHoaDon = () => {
  return axios.get(API)
}

export const getHoaDonPage = (params = {}) => {
  return axios.get(API, { params })
}

export const getHoaDonById = (id) => {
  return axios.get(`${API}/${ensureHoaDonId(id)}`)
}

export const lookupHoaDon = (maHoaDon, options = {}) => {
  const params = { maHoaDon }
  const soDienThoai = String(options?.soDienThoai || "").trim()
  const email = String(options?.email || "").trim()

  if (soDienThoai) {
    params.soDienThoai = soDienThoai
  }
  if (email) {
    params.email = email
  }

  return axios.get(`${API}/lookup`, {
    params
  })
}

export const sendOrderLookupMail = ({ maHoaDon, soDienThoai = "", email, trackingUrl = "" }) => {
  const payload = {
    maHoaDon,
    email,
    trackingUrl
  }

  if (String(soDienThoai || "").trim()) {
    payload.soDienThoai = String(soDienThoai).trim()
  }

  return axios.post(`${API}/lookup/send-mail`, payload)
}

export const createHoaDon = (data) => {
  return axios.post(API, data)
}

export const updateHoaDon = (id, data) => {
  return axios.put(`${API}/${ensureHoaDonId(id)}`, data)
}

export const updateHoaDonBySystemEvent = (id, eventCode, note = "", trackingUrl = "") => {
  return axios.post(`${API}/${ensureHoaDonId(id)}/system-events`, {
    eventCode,
    note,
    trackingUrl
  })
}

export const addHoaDonItem = (id, data) => {
  return axios.post(`${API}/${ensureHoaDonId(id)}/items`, data)
}

export const updateHoaDonItemQty = (id, itemId, data) => {
  return axios.put(`${API}/${ensureHoaDonId(id)}/items/${itemId}`, data)
}

export const deleteHoaDonItem = (id, itemId) => {
  return axios.delete(`${API}/${ensureHoaDonId(id)}/items/${itemId}`)
}

export const cancelHoaDon = (id, reason = "") => {
  return axios.post(`${API}/${id}/cancel`, null, {
    params: reason ? { reason } : {}
  })
}

export const getHoaDonChiTiet = async (id) => {
  const res = await getHoaDonById(id)
  const items = Array.isArray(res.data?.items) ? res.data.items : []
  return {
    ...res,
    data: items
  }
}