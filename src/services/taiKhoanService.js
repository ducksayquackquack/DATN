import axios from "axios"
import { resolveApiOrigin } from "../utils/apiOrigin"

const API = `${resolveApiOrigin()}/api/tai-khoan`

const taiKhoanService = {
  getAll(params = {}) {
    return axios.get(API, { params })
  },

  getById(id) {
    return axios.get(`${API}/${id}`)
  },

  create(data) {
    return axios.post(API, data)
  },

  update(id, data) {
    return axios.put(`${API}/${id}`, data)
  },

  updatePassword(id, currentPassword, newPassword) {
    const payload = {
      currentPassword,
      newPassword,
    }

    // Keep compatibility with different backend routes used across environments.
    return axios
      .put(`${API}/${id}/doi-mat-khau`, payload)
      .catch(() => axios.put(`${API}/${id}/change-password`, payload))
  },

  delete(id) {
    return axios.delete(`${API}/${id}`)
  }
}

export default taiKhoanService
