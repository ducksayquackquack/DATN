import axios from 'axios'
import { resolveApiOrigin } from '../utils/apiOrigin'

const API = `${resolveApiOrigin()}/api/phieu-thu-chi`

/**
 * Get all phieu thu/chi for a shift
 * @param {number} idNhanVien
 * @param {string} ngayLam  – date key e.g. '2024-01-15'
 * @param {number} [idCaLam]
 */
export const getPhieuByShift = (idNhanVien, ngayLam, idCaLam) => {
  return axios.get(API, { params: { idNhanVien, ngayLam, idCaLam } })
}

/**
 * Create a phieu thu or phieu chi
 * @param {{ loai: 'THU'|'CHI', phuongThuc: string, soTien: number, lyDo: string, idNhanVien: number, ngayLam: string, idCaLam: number, nguoiTao: string }} data
 */
export const createPhieu = (data) => {
  return axios.post(API, data)
}

/**
 * Get aggregated phieu thu/chi summary for a date range
 * @param {string} startDate – e.g. '2024-01-01'
 * @param {string} endDate   – e.g. '2024-12-31'
 * @returns {Promise<{thu:{total,count}, chi:{total,count}}>}
 */
export const getPhieuSummary = (startDate, endDate) => {
  return axios.get(`${API}/summary`, { params: { startDate, endDate } })
}

/**
 * Soft-delete a phieu
 */
export const deletePhieu = (id) => {
  return axios.delete(`${API}/${id}`)
}
