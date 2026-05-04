import axios from "axios"
import { resolveApiOrigin } from "../utils/apiOrigin"

const API = `${resolveApiOrigin()}/api/loai`

export const getAllLoai = () =>
  axios.get(API)

export const getLoaiById = (id) =>
  axios.get(`${API}/${id}`)

export const createLoai = (data) =>
  axios.post(API, data)

export const updateLoai = (id, data) =>
  axios.put(`${API}/${id}`, data)

export const deleteLoai = (id) =>
  axios.delete(`${API}/${id}`)