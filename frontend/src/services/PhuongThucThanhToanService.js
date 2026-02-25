import api from "./api";

// GET ALL
export const getPTTT = () =>
  api.get("/pttt");

// GET BY ID
export const getPTTTById = (id) =>
  api.get(`/pttt/${id}`);

// CREATE
export const createPTTT = (data) =>
  api.post("/pttt", data);

// UPDATE
export const updatePTTT = (id, data) =>
  api.put(`/pttt/${id}`, data);

// DELETE
export const deletePTTT = (id) =>
  api.delete(`/pttt/${id}`);