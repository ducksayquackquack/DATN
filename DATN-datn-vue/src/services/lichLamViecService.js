import axios from 'axios'

const API_URL = 'http://localhost:8080/api/lich-lam-viec'

export const getAllLichLamViec = () => {
  return axios.get(API_URL)
}

export const getAllLichLamViecFull = () => {
  return axios.get(`${API_URL}/full`)
}

export const getLichLamViecByNhanVien = (idNhanVien) => {
  return axios.get(`${API_URL}/nhan-vien/${idNhanVien}`)
}

export const createLichLamViec = (data) => {
  return axios.post(API_URL, data)
}

export const deleteLichLamViec = (id) => {
  return axios.delete(`${API_URL}/${id}`)
}

export default {
  getAll: getAllLichLamViec,
  getFull: getAllLichLamViecFull,
  getByNhanVien: getLichLamViecByNhanVien,
  create: createLichLamViec,
  delete: deleteLichLamViec
}
