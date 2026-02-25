import api from "./api";

export const getKichThuocList = () => {
  return api.get("/kich-thuoc");
};

export const getKichThuocById = (id) => {
  return api.get(`/kich-thuoc/${id}`);
};

export const createKichThuoc = (data) => {
  return api.post("/kich-thuoc", data);
};

export const updateKichThuoc = (id, data) => {
  return api.put(`/kich-thuoc/${id}`, data);
};

export const deleteKichThuoc = (id) => {
  return api.delete(`/kich-thuoc/${id}`);
};