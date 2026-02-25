import api from "./api";

// GET ALL
export const getMauSacList = () => {
  return api.get("/mau-sac");
};

// GET BY ID
export const getMauSacById = (id) => {
  return api.get(`/mau-sac/${id}`);
};

// CREATE
export const createMauSac = (data) => {
  return api.post("/mau-sac", data);
};

// UPDATE
export const updateMauSac = (id, data) => {
  return api.put(`/mau-sac/${id}`, data);
};

// DELETE
export const deleteMauSac = (id) => {
  return api.delete(`/mau-sac/${id}`);
};