import api from "./api";

export const getDanhMucList = () => api.get("/danh-muc");

export const createDanhMuc = (data) =>
  api.post("/danh-muc", data);

export const updateDanhMuc = (id, data) =>
  api.put(`/danh-muc/${id}`, data);

export const deleteDanhMuc = (id) =>
  api.delete(`/danh-muc/${id}`);