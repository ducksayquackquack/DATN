import axios from "axios"
import { resolveApiOrigin } from "../utils/apiOrigin"

const API = `${resolveApiOrigin()}/api/kich-thuoc`

export const getAllKichThuoc = () =>
  axios.get(API)

export const getKichThuocById = (id) =>
  axios.get(`${API}/${id}`)

export const createKichThuoc = (data) =>
  axios.post(API, data)

export const updateKichThuoc = (id, data) =>
  axios.put(`${API}/${id}`, data)

export const deleteKichThuoc = (id) =>
  axios.delete(`${API}/${id}`)