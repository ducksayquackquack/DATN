import axios from "axios"
import { resolveApiOrigin } from "../utils/apiOrigin"

const API = `${resolveApiOrigin()}/api/nhan-vien`

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
  // Prefer list+filter because many environments do not expose dedicated by-account routes.
  return axios.get(API).then((res) => {
    const list = Array.isArray(res.data) ? res.data : []
    const matched = list.find((item) => Number(item?.idTaiKhoan) === Number(taiKhoanId))

    if (matched) {
      return {
        ...res,
        data: matched
      }
    }

    const notFoundError = new Error("Không tìm thấy nhân viên theo tài khoản")
    notFoundError.response = { status: 404 }
    throw notFoundError
  })
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