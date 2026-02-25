import api from "./api";

export const getKhachHangList = () =>
  api.get("/khach-hang");

export const createKhachHang = (data) =>
  api.post("/khach-hang", data);

export const updateKhachHang = (id, data) =>
  api.put(`/khach-hang/${id}`, data);

export const deleteKhachHang = (id) =>
  api.delete(`/khach-hang/${id}`);