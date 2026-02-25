import api from "./api";

export const getLoaiList = () => api.get("/loai");

export const createLoai = (data) =>
    api.post("/loai", data);

export const updateLoai = (id, data) =>
    api.put(`/loai/${id}`, data);

export const deleteLoai = (id) =>
    api.delete(`/loai/${id}`);