import axios from "axios"

const API = "http://localhost:8080/api/nhan-vien"

// GET ALL
export function getAllNhanVien() {
  return axios.get(API)
}

// GET BY ID
export function getNhanVienById(id) {
  return axios.get(`${API}/${id}`)
}

// GET BY TAI KHOAN ID
export function getNhanVienByTaiKhoanId(taiKhoanId) {
  // Support multiple backend route styles used across environments.
  return axios
    .get(`${API}/tai-khoan/${taiKhoanId}`)
    .catch(() => axios.get(`${API}/by-tai-khoan/${taiKhoanId}`))
    .catch(() => axios.get(`${API}/tai-khoan-id/${taiKhoanId}`))
}

// CREATE
export function createNhanVien(data) {
  return axios.post(API, data)
}

// UPDATE
export function updateNhanVien(id, data) {
  return axios.put(`${API}/${id}`, data)
}

// DELETE
export function deleteNhanVien(id) {
  return axios.delete(`${API}/${id}`)
}