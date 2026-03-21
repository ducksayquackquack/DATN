import axios from "axios"

const API = "http://localhost:8080/api/pttt"

export function getAllPTTT() {
  return axios.get(API)
}

export function getPTTTById(id) {
  return axios.get(`${API}/${id}`)
}

export function createPTTT(data) {
  return axios.post(API, data)
}

export function updatePTTT(id, data) {
  return axios.put(`${API}/${id}`, data)
}

export function deletePTTT(id) {
  return axios.delete(`${API}/${id}`)
}