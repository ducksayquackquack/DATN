import axios from "axios"

const API = "http://localhost:8080/api/san-pham"

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