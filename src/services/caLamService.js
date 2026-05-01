import axios from 'axios'
import { resolveApiOrigin } from '../utils/apiOrigin'

const API_URL = `${resolveApiOrigin()}/api/lich-ca-lam`

export const getAllCaLam = () => {
  return axios.get(API_URL)
}

export const getCaLamById = (id) => {
  return axios.get(`${API_URL}/${id}`)
}

export const createCaLam = (data) => {
  return axios.post(API_URL, data)
}

export const updateCaLam = (id, data) => {
  return axios.put(`${API_URL}/${id}`, data)
}

export const deleteCaLam = (id) => {
  return axios.delete(`${API_URL}/${id}`)
}

export default {
  getAll: getAllCaLam,
  getById: getCaLamById,
  create: createCaLam,
  update: updateCaLam,
  delete: deleteCaLam
}
