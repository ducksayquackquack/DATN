import axios from 'axios'
import { resolveApiOrigin } from '../utils/apiOrigin'

const API_URL = `${resolveApiOrigin()}/api/dia-chi`

export const getDiaChiByKhachHang = (idKhachHang) => {
  return axios.get(`${API_URL}/khach-hang/${idKhachHang}`)
}

export const createDiaChi = (data) => {
  return axios.post(API_URL, data)
}

export const updateDiaChi = (id, data) => {
  return axios.put(`${API_URL}/${id}`, data)
}

export const deleteDiaChi = (id) => {
  return axios.delete(`${API_URL}/${id}`)
}

export default {
  getByKhachHang: getDiaChiByKhachHang,
  create: createDiaChi,
  update: updateDiaChi,
  delete: deleteDiaChi
}
