import axios from "axios"

const API = "http://localhost:8080/api/upload"

export const uploadImage = (file) => {
  const formData = new FormData()
  formData.append("file", file)
  return axios.post(API, formData, {
    headers: { "Content-Type": "multipart/form-data" }
  })
}

export const getImageUrl = (path) => {
  if (!path) return null
  if (path.startsWith("http")) return path
  return "http://localhost:8080" + path
}
