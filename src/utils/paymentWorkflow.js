const TAG_PREFIX = "[PAYMENT_FLOW:"

export const PAYMENT_FLOW_TAGS = {
  VN_PAY_CUSTOMER_CONFIRMED: "VNPAY_CUSTOMER_CONFIRMED",
  VN_PAY_EMPLOYEE_CONFIRMED: "VNPAY_EMPLOYEE_CONFIRMED"
}

const normalize = (value) => String(value || "").trim()

export const hasPaymentFlowTag = (statusNote, tag) => {
  const note = normalize(statusNote)
  const normalizedTag = normalize(tag)
  if (!note || !normalizedTag) return false
  return note.includes(`${TAG_PREFIX}${normalizedTag}]`)
}

export const appendPaymentFlowTag = (statusNote, tag, message = "") => {
  const note = normalize(statusNote)
  const normalizedTag = normalize(tag)
  const normalizedMessage = normalize(message)

  if (!normalizedTag) return note || normalizedMessage

  const tagToken = `${TAG_PREFIX}${normalizedTag}]`
  const parts = []

  if (note) parts.push(note)
  if (!hasPaymentFlowTag(note, normalizedTag)) {
    const payload = normalizedMessage ? `${tagToken} ${normalizedMessage}` : tagToken
    parts.push(payload)
  }

  return parts.join(" | ")
}

export const describePaymentFlowState = (order = {}) => {
  const note = normalize(order?.statusNote)
  const customerConfirmed = hasPaymentFlowTag(note, PAYMENT_FLOW_TAGS.VN_PAY_CUSTOMER_CONFIRMED)
  const employeeConfirmed = hasPaymentFlowTag(note, PAYMENT_FLOW_TAGS.VN_PAY_EMPLOYEE_CONFIRMED)

  if (employeeConfirmed) {
    return {
      code: "EMPLOYEE_CONFIRMED",
      label: "Thanh toán đã được xác nhận",
      tone: "success"
    }
  }

  if (customerConfirmed) {
    return {
      code: "WAIT_EMPLOYEE",
      label: "Chờ nhân viên kiểm tra giao dịch",
      tone: "warning"
    }
  }

  return {
    code: "WAIT_CUSTOMER",
    label: "Chờ khách xác nhận thanh toán",
    tone: "neutral"
  }
}

export const isLikelyCounterPickupOrder = (order = {}) => {
  const normalizedAddress = normalize(order?.diaChiNhanHang).toLowerCase()
  if (!normalizedAddress) return true
  return normalizedAddress.includes("tai quay") || normalizedAddress.includes("tai cua hang")
}
