import axios from "axios"

const API = "http://localhost:8080/api/mau-sac"

export const getAllMauSac = () =>
  axios.get(API)

export const getMauSacById = (id) =>
  axios.get(`${API}/${id}`)

export const createMauSac = (data) =>
  axios.post(API, data)

export const updateMauSac = (id, data) =>
  axios.put(`${API}/${id}`, data)

export const deleteMauSac = (id) =>
  axios.delete(`${API}/${id}`)