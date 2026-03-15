import axios from "axios"

const API = "http://localhost:8080/api/hoa-don"

export const getAllHoaDon = () => {
  return axios.get(API)
}

export const getHoaDonPage = (params = {}) => {
  return axios.get(API, { params })
}

export const getHoaDonById = (id) => {
  return axios.get(`${API}/${id}`)
}

export const createHoaDon = (data) => {
  return axios.post(API, data)
}

export const updateHoaDon = (id, data) => {
  return axios.put(`${API}/${id}`, data)
}

export const updateHoaDonBySystemEvent = (id, eventCode, note = "") => {
  return axios.post(`${API}/${id}/system-events`, {
    eventCode,
    note
  })
}

export const addHoaDonItem = (id, data) => {
  return axios.post(`${API}/${id}/items`, data)
}

export const updateHoaDonItemQty = (id, itemId, data) => {
  return axios.put(`${API}/${id}/items/${itemId}`, data)
}

export const deleteHoaDonItem = (id, itemId) => {
  return axios.delete(`${API}/${id}/items/${itemId}`)
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