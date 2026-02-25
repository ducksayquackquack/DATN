import api from "./api";

export const getTaiKhoanList = () => api.get("/tai-khoan");

export const createTaiKhoan = (data) =>
  api.post("/tai-khoan", data);

export const updateTaiKhoan = (id, data) =>
  api.put(`/tai-khoan/${id}`, data);

export const deleteTaiKhoan = (id) =>
  api.delete(`/tai-khoan/${id}`);