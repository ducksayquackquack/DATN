import axios from "axios"
import { resolveApiOrigin } from "../utils/apiOrigin"

const API = `${resolveApiOrigin()}/api/payment`
const FALLBACK_API = `${resolveApiOrigin()}/api/vnpay`

const getDefaultReturnUrl = () => {
  if (typeof window !== "undefined" && window.location?.origin) {
    return `${window.location.origin}/payment-return`
  }
  return "http://localhost:5173/payment-return"
}

export const createVnpayPayment = async (payload = {}) => {
  const amount = Number(payload?.amount || 0)
  const orderId = String(payload?.orderId || "").trim()
  const bankCode = String(payload?.bankCode || "").trim()
  const returnUrl = String(payload?.returnUrl || getDefaultReturnUrl()).trim()

  const requestPayload = {
    amount,
    orderId,
    returnUrl
  }

  if (bankCode) {
    requestPayload.bankCode = bankCode
  }

  const endpoints = [
    `${API}/create`,
    "/api/vnpay/create-payment",
    `${FALLBACK_API}/create-payment`
  ]

  let response = null
  let lastError = null

  for (const endpoint of endpoints) {
    try {
      response = await axios.post(endpoint, requestPayload)
      break
    } catch (error) {
      lastError = error
    }
  }

  if (!response) {
    throw lastError || new Error("Không thể tạo link thanh toán VNPay")
  }

  const paymentUrl = response?.data?.url || response?.data?.paymentUrl || ""
  return {
    ...response,
    data: {
      ...response.data,
      url: paymentUrl,
      paymentUrl
    }
  }
}
