import api from "./api";

// GET ALL
export const getNhanVienList = () => {
  return api.get("/nhan-vien");
};

// GET BY ID
export const getNhanVienById = (id) => {
  return api.get(`/nhan-vien/${id}`);
};

// CREATE
export const createNhanVien = (data) => {
  return api.post("/nhan-vien", data);
};

// UPDATE
export const updateNhanVien = (id, data) => {
  return api.put(`/nhan-vien/${id}`, data);
};

// DELETE
export const deleteNhanVien = (id) => {
  return api.delete(`/nhan-vien/${id}`);
};