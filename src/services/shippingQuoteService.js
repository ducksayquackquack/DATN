import axios from "axios"
import { resolveApiOrigin } from "../utils/apiOrigin"

const API = `${resolveApiOrigin()}/api/shipping`

export const quoteShippingFee = (payload) => {
  return axios.post(`${API}/quote`, payload)
}

export const quoteShippingFeeAll = (payload) => {
  return axios.post(`${API}/quote/all`, payload)
}
