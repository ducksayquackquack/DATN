import api from "./api";

// GET ALL
export const getSanPhamList = () => {
  return api.get("/san-pham");
};

// GET BY ID
export const getSanPhamById = (id) => {
  return api.get(`/san-pham/${id}`);
};

// CREATE
export const createSanPham = (data) => {
  return api.post("/san-pham", data);
};

// UPDATE
export const updateSanPham = (id, data) => {
  return api.put(`/san-pham/${id}`, data);
};

// DELETE
export const deleteSanPham = (id) => {
  return api.delete(`/san-pham/${id}`);
};