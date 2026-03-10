import axios from "axios"

const API = "http://localhost:8080/api/khuyen-mai"

export const getAllKhuyenMai = () =>
  axios.get(API)

export const getKhuyenMaiById = (id) =>
  axios.get(`${API}/${id}`)

export const createKhuyenMai = (data) =>
  axios.post(API, data)

export const updateKhuyenMai = (id, data) =>
  axios.put(`${API}/${id}`, data)

export const deleteKhuyenMai = (id) =>
  axios.delete(`${API}/${id}`)