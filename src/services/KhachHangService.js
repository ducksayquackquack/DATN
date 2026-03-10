import axios from "axios"

const API = "http://localhost:8080/api/khach-hang"

export const getAllKhachHang = (page = 0, size = 5) =>
  axios.get(`${API}?page=${page}&size=${size}`)

export const getKhachHangById = (id) =>
  axios.get(`${API}/${id}`)

export const createKhachHang = (data) =>
  axios.post(API, data)

export const updateKhachHang = (id, data) =>
  axios.put(`${API}/${id}`, data)

export const deleteKhachHang = (id) =>
  axios.delete(`${API}/${id}`)

export const getHoaDonByKhachHang = (id) =>
  axios.get(`http://localhost:8080/api/hoa-don/by-khach-hang/${id}`)