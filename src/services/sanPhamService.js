import axios from "axios"
import { resolveApiOrigin } from "../utils/apiOrigin"

const API = `${resolveApiOrigin()}/api/san-pham`
const API_ROOT = API.replace(/\/san-pham$/i, "")

export const createSanPham = (data) => {
  return axios.post(API, data)
}

export const updateSanPham = (id, data) => {
  return axios.put(`${API}/${id}`, data)
}

export const getAllSanPham = (params = {}) => {
  const mergedParams = {
    page: 0,
    size: 1000,
    ...params
  }
  return axios.get(API, { params: mergedParams })
}

export const getSanPhamPage = (params = {}) => {
  return axios.get(API, { params })
}

export const getSanPhamById = (id) => {
  return axios.get(`${API}/${id}`)
}

export const deleteSanPham = (id) => {
  return axios.delete(`${API}/${id}`)
}

export const updateSanPhamChiTietStatus = async (variantId, trangThai) => {
  const id = Number(variantId)
  if (!Number.isFinite(id) || id <= 0) {
    throw new Error("Variant ID không hợp lệ")
  }

  const statusText = String(trangThai || "Hoạt động")
  const normalized = statusText
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase()
  const active = !normalized.includes("ngung") && !normalized.includes("inactive")

  const payloads = [
    { trangThai: statusText },
    { trangThai: active ? "Hoạt động" : "Ngừng hoạt động" },
    { trangThai: active ? 1 : 0 },
    { trangThai: active },
    { isActive: active },
    { active }
  ]
  const endpoints = [
    `${API}/chi-tiet/${id}`,
    `${API_ROOT}/san-pham-chi-tiet/${id}`,
    `${API_ROOT}/san-pham-chi-tiets/${id}`
  ]

  let lastError = null
  for (const url of endpoints) {
    for (const payload of payloads) {
      try {
        return await axios.patch(url, payload)
      } catch (error) {
        lastError = error
        const status = Number(error?.response?.status || 0)
        if (status === 404 || status === 405) {
          try {
            return await axios.put(url, payload)
          } catch (putError) {
            lastError = putError
            continue
          }
        }
        if (status && status < 500) continue
      }

      try {
        return await axios.put(url, payload)
      } catch (putError) {
        lastError = putError
        const putStatus = Number(putError?.response?.status || 0)
        if (putStatus && putStatus < 500) continue
        try {
          return await axios.patch(url, payload)
        } catch (patchError) {
          lastError = patchError
        }
      }
    }
  }

  throw lastError || new Error("Không thể cập nhật trạng thái biến thể")
}

export const cleanupDeletedVariants = async (variantIds = [], productId = null) => {
  const ids = [...new Set((Array.isArray(variantIds) ? variantIds : [variantIds])
    .map((id) => Number(id))
    .filter((id) => Number.isFinite(id) && id > 0))]

  if (!ids.length) return []

  const unresolved = []

  for (const variantId of ids) {
    // Thử endpoint mới trước, sau đó fallback các endpoint cũ
    const endpoints = [
      `${API}/chi-tiet/${variantId}`,
      `${API_ROOT}/san-pham-chi-tiet/${variantId}`,
      `${API_ROOT}/san-pham-chi-tiets/${variantId}`
    ]

    if (Number.isFinite(Number(productId)) && Number(productId) > 0) {
      endpoints.push(`${API_ROOT}/san-pham/${Number(productId)}/chi-tiet/${variantId}`)
    }

    let deleted = false
    for (const url of endpoints) {
      try {
        await axios.delete(url)
        deleted = true
        break
      } catch (error) {
        const status = Number(error?.response?.status || 0)
        if (status && status < 500 && status !== 405) {
          continue
        }
      }
    }

    if (!deleted) unresolved.push(variantId)
  }

  return unresolved
}