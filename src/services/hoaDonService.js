import axios from "axios"

const API = "http://localhost:8080/api/hoa-don"

export const getAllHoaDon = () => {
  return axios.get(API)
}

export const getHoaDonById = (id) => {
  return axios.get(`${API}/${id}`)
}

export const createHoaDon = (data) => {
  return axios.post(API, data)
}

export const updateHoaDon = (id, data) => {
  return axios.put(`${API}/${id}`, data)
}

export const deleteHoaDon = (id) => {
  return axios.delete(`${API}/${id}`)
}

export const getHoaDonChiTiet = (id) => {
  return axios.get(`${API}/${id}/chi-tiet`)
}