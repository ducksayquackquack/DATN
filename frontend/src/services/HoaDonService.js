import api from "./api";

export const getHoaDonList = () =>
  api.get("/hoa-don");

export const getHoaDonById = (id) =>
  api.get(`/hoa-don/${id}`);

export const getHoaDonChiTiet = (id) =>
  api.get(`/hoa-don/${id}/chi-tiet`);

export const updateHoaDon = (id, data) =>
  api.put(`/hoa-don/${id}`, data);

export const deleteHoaDon = (id) =>
  api.delete(`/hoa-don/${id}`);