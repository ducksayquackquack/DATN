import axios from "axios"

const API = "http://localhost:8080/api/san-pham"
const API_ROOT = API.replace(/\/san-pham$/i, "")

export const createSanPham = (data) => {
  return axios.post(API, data)
}

export const updateSanPham = (id, data) => {
  return axios.put(`${API}/${id}`, data)
}

export const getAllSanPham = () => {
  return axios.get(API)
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

export const cleanupDeletedVariants = async (variantIds = [], productId = null) => {
  const ids = [...new Set((Array.isArray(variantIds) ? variantIds : [variantIds])
    .map((id) => Number(id))
    .filter((id) => Number.isFinite(id) && id > 0))]

  if (!ids.length) return []

  const unresolved = []

  for (const variantId of ids) {
    const endpoints = [
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