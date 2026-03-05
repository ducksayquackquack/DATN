import axios from "axios"

const API = "http://localhost:8080/api/san-pham"

export function getAllSanPham() {
  return axios.get(API)
}

export function getSanPhamById(id) {
  return axios.get(`${API}/${id}`)
}

export function createSanPham(data) {
  return axios.post(API, data)
}

export function updateSanPham(id, data) {
  return axios.put(`${API}/${id}`, data)
}

export function deleteSanPham(id) {
  return axios.delete(`${API}/${id}`)
}