import { computed, onMounted, onUnmounted, ref } from "vue"
import { getAllHoaDon } from "../services/hoaDonService"
import { getKhachHangByTaiKhoanId } from "../services/KhachHangService"
import { hasPaymentFlowTag, PAYMENT_FLOW_TAGS } from "../utils/paymentWorkflow"

const STORAGE_KEY = "notifications:seen"

const readSeenMap = () => {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || "{}")
  } catch {
    return {}
  }
}

const writeSeenMap = (value) => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(value || {}))
}

const isSeen = (scope, id) => {
  const map = readSeenMap()
  return Boolean(map?.[scope]?.[id])
}

const markSeen = (scope, ids = []) => {
  const map = readSeenMap()
  const current = { ...(map?.[scope] || {}) }
  for (const id of ids) current[id] = true
  map[scope] = current
  writeSeenMap(map)
}

const normalizeList = (response) => {
  if (Array.isArray(response?.data)) return response.data
  if (Array.isArray(response?.data?.content)) return response.data.content
  return []
}

const normalizeStatusCode = (value) => String(value || "").trim().toUpperCase()

const normalizeStatusText = (value) => String(value || "").trim().toLowerCase()

const toCustomerStatusLabel = (item) => {
  const code = normalizeStatusCode(item?.orderStatusCode)
  const text = normalizeStatusText(item?.orderStatusName || item?.trangThai)

  if (code === "CHO_XAC_NHAN" || text.includes("chờ xác nhận") || text.includes("cho xac nhan")) return "Chờ xác nhận"
  if (code === "CHO_LAY_HANG" || text.includes("chờ xử lý") || text.includes("cho xu ly")) return "Chờ xử lý"
  if (code === "DANG_GIAO" || text.includes("đang giao") || text.includes("dang giao")) return "Đang giao"
  if (code === "DA_GIAO" || code === "HOAN_THANH" || text.includes("đã giao") || text.includes("da giao") || text.includes("hoàn thành") || text.includes("hoan thanh")) return "Hoàn thành"
  if (code === "HUY" || code === "DA_HUY" || text.includes("hủy") || text.includes("huy")) return "Đã hủy"
  if (code === "GIAO_THAT_BAI" || text.includes("thất bại") || text.includes("that bai")) return "Giao thất bại"

  return item?.orderStatusName || item?.trangThai || "Đơn hàng đã cập nhật"
}

const summarizeCustomerStatus = (item) => {
  const status = toCustomerStatusLabel(item)
  const orderCode = item?.maHoaDon || `#${item?.id}`
  if (status === "Chờ xác nhận") return `Đơn ${orderCode} vừa được tạo và đang chờ xác nhận.`
  if (status === "Chờ xử lý") return `Đơn ${orderCode} đang được chuẩn bị.`
  if (status === "Đang giao") return `Đơn ${orderCode} đang trên đường giao đến bạn.`
  if (status === "Hoàn thành") return `Đơn ${orderCode} đã hoàn thành.`
  if (status === "Đã hủy") return `Đơn ${orderCode} đã bị hủy.`
  if (status === "Giao thất bại") return `Đơn ${orderCode} giao không thành công.`
  return `Đơn ${orderCode} có cập nhật mới.`
}

const buildCustomerOrderNotificationId = (item) => {
  const code = normalizeStatusCode(item?.orderStatusCode || item?.orderStatusName || item?.trangThai)
  const note = String(item?.statusNote || "").trim().toLowerCase().slice(0, 120)
  return `customer-order-${item?.id}-${code}-${note}`
}

const resolveCurrentCustomerId = async () => {
  const userId = Number(localStorage.getItem("userId") || 0)
  if (!userId) return null

  try {
    const response = await getKhachHangByTaiKhoanId(userId)
    return Number(response?.data?.id || 0) || null
  } catch {
    return null
  }
}

const getScopeFromPath = (path = "") => {
  if (String(path).startsWith("/admin/")) return "admin"
  if (String(path).startsWith("/employee/")) return "employee"
  return "customer"
}

const buildCustomerNotifications = async () => {
  const list = []

  try {
    const customerId = await resolveCurrentCustomerId()
    if (customerId) {
      const response = await getAllHoaDon()
      const rows = normalizeList(response)
      const customerRows = rows
        .filter((item) => Number(item?.khachHangId || item?.khachHang?.id) === Number(customerId))
        .sort((a, b) => String(b?.ngayTao || "").localeCompare(String(a?.ngayTao || "")))
        .slice(0, 20)

      for (const item of customerRows) {
        const id = buildCustomerOrderNotificationId(item)
        list.push({
          id,
          createdAt: item?.ngayTao || new Date().toISOString(),
          title: `Đơn hàng ${toCustomerStatusLabel(item)}`,
          description: summarizeCustomerStatus(item),
          link: "/customer/profile?tab=orders",
          read: isSeen("customer", id)
        })
      }
    }
  } catch {
    // Keep local fallback notifications below.
  }

  try {
    const latest = JSON.parse(localStorage.getItem("vnpay:lastCustomerConfirm") || "null")
    if (latest?.invoiceId) {
      const id = `customer-vnpay-${latest.invoiceId}`
      list.push({
        id,
        createdAt: latest.at || new Date().toISOString(),
        title: "Thanh toán VNPay đã được ghi nhận",
        description: latest.invoiceCode
          ? `Yêu cầu xác nhận đã gửi cho hóa đơn ${latest.invoiceCode}.`
          : "Yêu cầu xác nhận đã gửi thành công.",
        link: "/customer/profile?tab=orders",
        read: isSeen("customer", id)
      })
    }
  } catch {
    // ignore malformed payload
  }

  return list.sort((a, b) => String(b?.createdAt || "").localeCompare(String(a?.createdAt || "")))
}

const buildStaffNotifications = (rows, scope) => {
  const vnpayAwaiting = rows
    .filter((item) => {
      const note = item?.statusNote || ""
      const hasCustomerConfirm = hasPaymentFlowTag(note, PAYMENT_FLOW_TAGS.VN_PAY_CUSTOMER_CONFIRMED)
      const hasEmployeeConfirm = hasPaymentFlowTag(note, PAYMENT_FLOW_TAGS.VN_PAY_EMPLOYEE_CONFIRMED)
      return hasCustomerConfirm && !hasEmployeeConfirm
    })
    .map((item) => {
      const id = `invoice-${item.id}`
      return {
        id,
        createdAt: item?.ngayTao || new Date().toISOString(),
        title: "Thông báo thanh toán VNPay",
        description: `Đơn ${item?.maHoaDon || `#${item?.id}`} đang chờ nhân viên xác nhận tiền vào tài khoản shop.`,
        link: `${scope === "admin" ? "/admin" : "/employee"}/hoa-don/detail/${item.id}`,
        read: isSeen(scope, id)
      }
    })

  const orderQueue = rows
    .filter((item) => {
      const code = normalizeStatusCode(item?.orderStatusCode)
      return code === "CHO_XAC_NHAN" || code === "CHO_LAY_HANG" || code === "GIAO_THAT_BAI"
    })
    .map((item) => {
      const code = normalizeStatusCode(item?.orderStatusCode)
      const id = `invoice-queue-${item.id}-${code}`
      const orderCode = item?.maHoaDon || `#${item?.id}`
      const statusLabel = toCustomerStatusLabel(item)
      return {
        id,
        createdAt: item?.ngayTao || new Date().toISOString(),
        title: "Đơn hàng cần xử lý",
        description: `Đơn ${orderCode} hiện ở trạng thái ${statusLabel}.`,
        link: `${scope === "admin" ? "/admin" : "/employee"}/hoa-don/detail/${item.id}`,
        read: isSeen(scope, id)
      }
    })

  return [...vnpayAwaiting, ...orderQueue]
    .sort((a, b) => String(b?.createdAt || "").localeCompare(String(a?.createdAt || "")))
    .slice(0, 30)
}

export function useNotifications(scope = "customer") {
  const notifications = ref([])
  const loading = ref(false)

  const unreadCount = computed(() => notifications.value.filter((item) => !item.read).length)

  const refresh = async () => {
    if (scope === "customer") {
      notifications.value = await buildCustomerNotifications()
      return
    }

    loading.value = true
    try {
      const response = await getAllHoaDon()
      const rows = normalizeList(response)
      notifications.value = buildStaffNotifications(rows, scope)
    } catch {
      notifications.value = []
    } finally {
      loading.value = false
    }
  }

  const markAllAsRead = () => {
    const ids = notifications.value.map((item) => item.id)
    markSeen(scope, ids)
    notifications.value = notifications.value.map((item) => ({ ...item, read: true }))
  }

  const markOneAsRead = (id) => {
    if (!id) return
    markSeen(scope, [id])
    notifications.value = notifications.value.map((item) => {
      if (item.id !== id) return item
      return { ...item, read: true }
    })
  }

  const handleStorage = () => {
    refresh()
  }

  onMounted(() => {
    refresh()
    window.addEventListener("storage", handleStorage)
    window.addEventListener("focus", refresh)
  })

  onUnmounted(() => {
    window.removeEventListener("storage", handleStorage)
    window.removeEventListener("focus", refresh)
  })

  return {
    notifications,
    loading,
    unreadCount,
    refresh,
    markAllAsRead,
    markOneAsRead
  }
}

export { getScopeFromPath }
