import axios from "axios"

const API = "http://localhost:8080/api/khach-hang"

export const getAllKhachHang = (page = 0, size = 5) =>
  axios.get(`${API}?page=${page}&size=${size}`)

export const getKhachHangById = (id) =>
  axios.get(`${API}/${id}`)

export const getKhachHangByTaiKhoanId = async (idTaiKhoan) => {
  const targetId = Number(idTaiKhoan)
  if (!targetId) {
    return { data: null }
  }

  const pageSize = 50
  let page = 0
  let totalPages = 1

  while (page < totalPages) {
    const response = await getAllKhachHang(page, pageSize)
    const payload = response?.data
    const customers = Array.isArray(payload)
      ? payload
      : (Array.isArray(payload?.content) ? payload.content : [])

    const matched = customers.find((item) => {
      return Number(item?.idTaiKhoan || item?.taiKhoan?.id) === targetId
    }) || null

    if (matched) {
      return { ...response, data: matched }
    }

    const detectedTotalPages = Number(payload?.totalPages)
    if (Number.isFinite(detectedTotalPages) && detectedTotalPages > 0) {
      totalPages = detectedTotalPages
    } else if (customers.length < pageSize) {
      break
    } else {
      totalPages = page + 2
    }

    page += 1
  }

  return { data: null }
}

export const createKhachHang = (data) =>
  axios.post(API, data)

export const updateKhachHang = (id, data) =>
  axios.put(`${API}/${id}`, data)

export const deleteKhachHang = (id) =>
  axios.delete(`${API}/${id}`)

export const getHoaDonByKhachHang = (id) =>
  axios.get(`http://localhost:8080/api/hoa-don/by-khach-hang/${id}`)