import api from "./api";

// GET ALL
export const getKhuyenMaiList = () => {
  return api.get("/khuyen-mai");
};

// GET BY ID
export const getKhuyenMaiById = (id) => {
  return api.get(`/khuyen-mai/${id}`);
};

// CREATE
export const createKhuyenMai = (data) => {
  return api.post("/khuyen-mai", data);
};

// UPDATE
export const updateKhuyenMai = (id, data) => {
  return api.put(`/khuyen-mai/${id}`, data);
};

// DELETE
export const deleteKhuyenMai = (id) => {
  return api.delete(`/khuyen-mai/${id}`);
};