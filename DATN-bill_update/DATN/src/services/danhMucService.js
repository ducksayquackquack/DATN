import axios from "axios"

const API = "http://localhost:8080/api/admin/danh-muc"

export const getAllDanhMuc = () => {
  return axios.get(API)
}

export const getDanhMucById = (id) => {
  return axios.get(`${API}/${id}`)
}

export const createDanhMuc = (data) => {
  return axios.post(API, data)
}

export const updateDanhMuc = (id, data) => {
  return axios.put(`${API}/${id}`, data)
}

export const deleteDanhMuc = (id) => {
  return axios.delete(`${API}/${id}`)
}