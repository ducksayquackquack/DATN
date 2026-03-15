import axios from 'axios'

const API_URL = 'http://localhost:8080/api/lich-su-ca'

export const getAllLichSuCa = () => {
  return axios.get(API_URL)
}

export const getLichSuCaByNhanVien = (idNhanVien) => {
  return axios.get(`${API_URL}/nhan-vien/${idNhanVien}`)
}

export const createLichSuCa = (data) => {
  return axios.post(API_URL, data)
}

export const updateLichSuCa = (id, data) => {
  return axios.put(`${API_URL}/${id}`, data)
}

export const deleteLichSuCa = (id) => {
  return axios.delete(`${API_URL}/${id}`)
}

export default {
  getAll: getAllLichSuCa,
  getByNhanVien: getLichSuCaByNhanVien,
  create: createLichSuCa,
  update: updateLichSuCa,
  delete: deleteLichSuCa
}
