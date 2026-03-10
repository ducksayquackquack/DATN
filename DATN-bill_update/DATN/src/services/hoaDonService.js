import axios from "axios"

const API = "http://localhost:8080/api/hoa-don"
const NHAN_VIEN_API = "http://localhost:8080/api/nhan-vien"
const KHACH_HANG_API = "http://localhost:8080/api/khach-hang"
const SAN_PHAM_API = "http://localhost:8080/api/san-pham"

/* ================= GIỮ NGUYÊN HÀM CŨ ================= */

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

/* ================= FORM OPTIONS ================= */

export const getNhanVienOptions = () => {
  return axios.get(NHAN_VIEN_API)
}

export const getKhachHangOptions = () => {
  return axios.get(KHACH_HANG_API)
}

/* ================= DETAIL ================= */

export const getHoaDonItems = (id) => {
  return axios.get(`${API}/${id}/chi-tiet`)
}

export const getHoaDonHistory = (id) => {
  return axios.get(`${API}/${id}/history`)
}

/* ================= STATUS ACTION ================= */
/* Dùng params để khớp Spring @RequestParam, đỡ bug hơn gửi JSON body */

export const confirmHoaDon = (id) => {
  return axios.post(`${API}/${id}/confirm`)
}

export const shipHoaDon = (id) => {
  return axios.post(`${API}/${id}/ship`)
}

export const completeHoaDon = (id) => {
  return axios.post(`${API}/${id}/complete`)
}

export const cancelHoaDon = (id, reason) => {
  return axios.post(`${API}/${id}/cancel`, null, {
    params: { reason }
  })
}

export const failDeliveryHoaDon = (id, note) => {
  return axios.post(`${API}/${id}/fail-delivery`, null, {
    params: { note }
  })
}

export const returnedHoaDon = (id, note) => {
  return axios.post(`${API}/${id}/returned`, null, {
    params: { note }
  })
}

/* ================= SAN PHAM / BIEN THE ================= */

export const searchSanPham = (q) => {
  return axios.get(`${SAN_PHAM_API}/search`, {
    params: { q }
  })
}

export const getSizesByProduct = (productId) => {
  return axios.get(`${SAN_PHAM_API}/${productId}/sizes`)
}

export const getColorsByProductAndSize = (productId, sizeId) => {
  return axios.get(`${SAN_PHAM_API}/${productId}/sizes/${sizeId}/colors`)
}

/* ================= HOA DON ITEMS ================= */
/* Backend Spring kiểu cũ thường nhận @RequestParam -> gửi params */

export const addItemByVariant = (hoaDonId, data) => {
  return axios.post(`${API}/${hoaDonId}/items/add-by-variant`, null, {
    params: {
      productId: data.productId,
      sizeId: data.sizeId,
      colorId: data.colorId,
      soLuong: data.soLuong
    }
  })
}

export const updateItemQty = (hoaDonId, itemId, soLuong) => {
  return axios.post(`${API}/${hoaDonId}/items/${itemId}/qty`, null, {
    params: { soLuong }
  })
}

export const deleteItem = (hoaDonId, itemId) => {
  return axios.post(`${API}/${hoaDonId}/items/${itemId}/delete`)
}