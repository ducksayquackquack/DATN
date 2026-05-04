import axios from "axios"
import { resolveApiOrigin } from "../utils/apiOrigin"

const API = `${resolveApiOrigin()}/api/khach-hang`

export const getAllKhachHang = (page = 0, size = 5) =>
  axios.get(`${API}?page=${page}&size=${size}`)

export const listAllKhachHang = async (pageSize = 100) => {
  const collected = []
  const visited = new Set()
  let page = 0
  let totalPages = 1

  while (page < totalPages) {
    const response = await getAllKhachHang(page, pageSize)
    const payload = response?.data
    const customers = Array.isArray(payload)
      ? payload
      : (Array.isArray(payload?.content) ? payload.content : [])

    for (const customer of customers) {
      const key = String(customer?.id || `${customer?.idTaiKhoan || ''}:${customer?.soDienThoai || ''}`)
      if (!key || visited.has(key)) continue
      visited.add(key)
      collected.push(customer)
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

  return collected
}

export const getKhachHangById = (id) =>
  axios.get(`${API}/${id}`)

export const getKhachHangByTaiKhoanIdDirect = (idTaiKhoan) =>
  axios.get(`${API}/by-tai-khoan/${idTaiKhoan}`)

export const getKhachHangByTaiKhoanId = async (idTaiKhoan) => {
  const targetId = Number(idTaiKhoan)
  if (!targetId) {
    return { data: null }
  }

  try {
    return await getKhachHangByTaiKhoanIdDirect(targetId)
  } catch (error) {
    if (error?.response?.status && error.response.status !== 404) {
      throw error
    }
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
  axios.get(`${resolveApiOrigin()}/api/hoa-don/by-khach-hang/${id}`)