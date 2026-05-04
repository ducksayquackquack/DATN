import axios from "axios"
import { resolveApiOrigin } from "../utils/apiOrigin"

const NODE_BACKEND = String(import.meta.env.VITE_NODE_BACKEND_URL || "").trim().replace(/\/$/, "")
const API_ORIGIN = resolveApiOrigin().replace(/\/$/, "")
const LOCAL_API_ORIGIN = "http://localhost:8080"
const LOCAL_HOST_PATTERN = /^(localhost|127(?:\.\d+){3}|0\.0\.0\.0|::1)$/i
const IS_LOCAL_BROWSER = (() => {
  if (typeof window === "undefined") return false
  return LOCAL_HOST_PATTERN.test(String(window.location?.hostname || "").trim())
})()

const CANDIDATE_BASES = [
  `${API_ORIGIN}/api/admin`,
  `${API_ORIGIN}/api`
]

if (IS_LOCAL_BROWSER) {
  CANDIDATE_BASES.push(`${LOCAL_API_ORIGIN}/api/admin`, `${LOCAL_API_ORIGIN}/api`)
}

const ENDPOINTS = {
  dotGiamGia: ["/dot-giam-gia", "/khuyen-mai"],
  chiTietDotGiamGia: ["/chi-tiet-dot-giam-gia", "/chi-tiet-khuyen-mai"],
  chiTietSanPham: ["/chi-tiet-san-pham", "/san-pham-chi-tiet", "/san-pham-chi-tiets", "/san-pham/chi-tiet"],
  sanPham: ["/san-pham"],
  mauSac: ["/mau-sac"],
  kichThuoc: ["/kich-thuoc"],
  thuongHieu: ["/thuong-hieu"],
  chatLieu: ["/chat-lieu"],
  loaiSan: ["/loai-san"],
  xuatXu: ["/xuat-xu"],
  anhChiTietSanPham: ["/anh-chi-tiet-san-pham"]
}

const endpointCache = new Map()

const unwrapList = (payload) => {
  const data = payload?.data ?? payload
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.data)) return data.data
  if (Array.isArray(data?.content)) return data.content
  if (Array.isArray(data?.data?.content)) return data.data.content
  if (Array.isArray(data?.data?.data)) return data.data.data
  if (Array.isArray(data?.data?.data?.content)) return data.data.data.content
  return []
}

const unwrapPageMeta = (payload) => {
  const data = payload?.data ?? payload ?? {}
  return {
    number: Number(data?.number ?? data?.pageNumber ?? data?.page ?? 0),
    size: Number(data?.size ?? data?.pageSize ?? 0),
    totalPages: Number(data?.totalPages ?? data?.pageCount ?? 0),
    totalElements: Number(data?.totalElements ?? data?.total ?? data?.count ?? 0)
  }
}

const fetchAllPages = async (url) => {
  // Try plain GET first to avoid 400 noise on endpoints that do not accept page/size params.
  let first = await safeRequest("get", url)
  if (!first) {
    first = await safeRequest("get", url, { params: { page: 0, size: 9999 } })
  }
  if (!first) return []

  const firstList = unwrapList(first)
  const meta = unwrapPageMeta(first)

  if (!Number.isFinite(meta.totalPages) || meta.totalPages <= 1) {
    return firstList
  }

  const startPage = Number.isFinite(meta.number) ? meta.number + 1 : 1
  const pageSize = Number.isFinite(meta.size) && meta.size > 0 ? meta.size : 9999
  const all = [...firstList]

  for (let page = startPage; page < meta.totalPages; page += 1) {
    const next = await safeRequest("get", url, { params: { page, size: pageSize } })
    if (!next) continue
    all.push(...unwrapList(next))
  }

  return all
}

const readId = (obj = {}) => Number(obj?.id ?? obj?.dotGiamGiaId ?? obj?.khuyenMaiId ?? 0)

const safeRequest = async (method, url, config = {}) => {
  try {
    return await axios({ method, url, __silentErrors: true, ...config })
  } catch (error) {
    const status = Number(error?.response?.status || 0)
    const isGet = String(method || "get").toLowerCase() === "get"
    if (isGet && (status === 400 || status === 404 || status === 405 || status >= 500)) return null
    throw error
  }
}

const probeEndpoint = async (url) => {
  const res = await safeRequest("get", url)
  return Boolean(res)
}

const pickEndpoint = async (key) => {
  if (endpointCache.has(key)) return endpointCache.get(key)

  const paths = ENDPOINTS[key] || []
  for (const base of CANDIDATE_BASES) {
    for (const path of paths) {
      const url = `${base}${path}`
      let ok = false
      try {
        ok = await probeEndpoint(url)
      } catch {
        ok = false
      }
      if (ok) {
        endpointCache.set(key, url)
        return url
      }
    }
  }

  const fallback = `${CANDIDATE_BASES[0]}${paths[0] || ""}`
  endpointCache.set(key, fallback)
  return fallback
}

const readName = (obj, ...keys) => {
  for (const key of keys) {
    if (obj?.[key] !== undefined && obj?.[key] !== null && String(obj[key]).trim() !== "") {
      return String(obj[key]).trim()
    }
  }
  return ""
}

const toNumber = (...values) => {
  for (const value of values) {
    const parsed = Number(value)
    if (Number.isFinite(parsed)) return parsed
  }
  return 0
}

export const normalizeDiscountData = (item = {}) => {
  const id = Number(item?.id ?? item?.dotGiamGiaId ?? item?.khuyenMaiId ?? 0)

  const ma = readName(item, "maDotGiamGia", "maKhuyenMai", "maDot", "code")
  const ten = readName(item, "tenDotGiamGia", "tenKhuyenMai", "tenDot", "name")

  const giaTriGiamGia = toNumber(item?.giaTriGiamGia, item?.giaTriGiam, item?.giaTri, item?.discountValue)
  const mucUuTien = toNumber(item?.mucUuTien, item?.priority, item?.doUuTien)

  const soLuongSanPhamApDung = Math.max(
    toNumber(
      item?.soLuongSanPhamApDung,
      item?.tongSanPhamApDung,
      item?.soSanPhamApDung,
      item?.appliedProductCount,
      Array.isArray(item?.idChiTietSanPhams) ? item.idChiTietSanPhams.length : 0
    ),
    0
  )

  return {
    ...item,
    id,
    maDotGiamGia: ma,
    maKhuyenMai: ma,
    tenDotGiamGia: ten,
    tenKhuyenMai: ten,
    giaTriGiamGia,
    giaTri: giaTriGiamGia,
    mucUuTien,
    ngayBatDau: item?.ngayBatDau ?? item?.startDate ?? "",
    ngayKetThuc: item?.ngayKetThuc ?? item?.endDate ?? "",
    doiTuongApDung: String(item?.doiTuongApDung ?? item?.phamViApDung ?? item?.applyTarget ?? "ALL").toUpperCase(),
    trangThai: toBoolean(item?.trangThai, true),
    soLuongSanPhamApDung
  }
}

const mapById = (rows = [], ...nameKeys) => {
  const map = new Map()
  rows.forEach((row) => {
    const id = Number(row?.id)
    if (!Number.isFinite(id)) return
    const label = readName(row, ...nameKeys)
    if (label) map.set(id, label)
  })
  return map
}

const normalizeImageUrl = (raw) => {
  if (!raw) return ""
  const value = String(raw).trim()
  if (!value) return ""
  if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("data:image/")) return value
  if (value.startsWith("/")) return `${API_ORIGIN}${value}`
  if (value.startsWith("uploads/")) return `${API_ORIGIN}/${value}`
  return `${API_ORIGIN}/uploads/${value}`
}

const toVariantList = (product = {}) => {
  if (Array.isArray(product?.sanPhamChiTiets)) return product.sanPhamChiTiets
  if (Array.isArray(product?.variants)) return product.variants
  if (Array.isArray(product?.chiTiets)) return product.chiTiets
  if (Array.isArray(product?.danhSachBienThe)) return product.danhSachBienThe
  return []
}

const toDateInput = (value) => {
  if (!value) return ""
  if (Array.isArray(value)) {
    const [y, m, d] = value
    if (y && m && d) return `${String(y).padStart(4, "0")}-${String(m).padStart(2, "0")}-${String(d).padStart(2, "0")}`
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ""
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const day = String(date.getDate()).padStart(2, "0")
  return `${year}-${month}-${day}`
}

const toApiDateTime = (value) => {
  const normalized = toDateInput(value)
  return normalized ? `${normalized}T00:00:00` : ""
}

const toBoolean = (value, fallback = false) => {
  if (typeof value === "boolean") return value
  if (typeof value === "number") return value === 1
  if (typeof value === "string") {
    const raw = value.trim().toLowerCase()
    const s = raw
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .replace(/đ/g, "d")

    if (s === "true" || s === "1" || s === "yes") return true
    if (s === "false" || s === "0" || s === "no") return false

    if (
      s.includes("active")
      || s.includes("hoat dong")
      || s.includes("dang dien ra")
      || s.includes("dang hoat dong")
    ) return true

    if (
      s.includes("inactive")
      || s.includes("ngung")
      || s.includes("tam dung")
      || s.includes("da ket thuc")
      || s.includes("stop")
      || s.includes("paused")
      || s.includes("off")
    ) return false
  }
  return fallback
}

const toTrangThaiLabel = (value, fallback = true) => {
  return toBoolean(value, fallback) ? "Hoạt động" : "Ngừng hoạt động"
}

const naturalCompare = (left, right) => String(left || "").localeCompare(String(right || ""), undefined, {
  numeric: true,
  sensitivity: "base"
})

const buildVariantStableKey = (row = {}) => {
  const id = Number(row?.id)
  if (Number.isFinite(id) && id > 0) return `id:${id}`

  const code = readName(row, "maChiTietSanPham", "maSanPhamChiTiet", "ma", "ma_chi_tiet_san_pham")
  if (code) return `ma:${code.toUpperCase()}`

  const productId = Number(row?.idSanPham ?? row?.id_san_pham ?? row?.sanPham?.id ?? 0)
  const colorId = Number(row?.idMauSac ?? row?.id_mau_sac ?? row?.mauSac?.id ?? 0)
  const sizeId = Number(row?.idKichThuoc ?? row?.id_kich_thuoc ?? row?.kichThuoc?.id ?? 0)
  return `pmk:${productId}:${colorId}:${sizeId}`
}

const sortVariantsStable = (rows = []) => {
  return [...rows].sort((a, b) => {
    const byProduct = naturalCompare(a?.maSanPham || a?.idSanPham, b?.maSanPham || b?.idSanPham)
    if (byProduct !== 0) return byProduct

    const byVariant = naturalCompare(a?.maChiTietSanPham || a?.id, b?.maChiTietSanPham || b?.id)
    if (byVariant !== 0) return byVariant

    const byColor = naturalCompare(a?.tenMauSac, b?.tenMauSac)
    if (byColor !== 0) return byColor

    return naturalCompare(a?.tenKichThuoc, b?.tenKichThuoc)
  })
}

const mapAssociatedProductsFromVariants = (variants = [], khuyenMaiId = 0) => {
  const kmId = Number(khuyenMaiId || 0)
  if (!Number.isFinite(kmId) || kmId <= 0) return []

  const byProduct = new Map()
  variants.forEach((v) => {
    const vKmId = Number(v?.idKhuyenMai ?? v?.id_khuyen_mai ?? 0)
    if (vKmId !== kmId) return

    const productId = Number(v?.idSanPham ?? v?.id_san_pham ?? 0)
    if (!Number.isFinite(productId) || productId <= 0) return
    if (byProduct.has(productId)) return

    byProduct.set(productId, {
      id: productId,
      maSanPham: v?.maSanPham || "",
      tenSanPham: v?.tenSanPham || "",
      idKhuyenMai: kmId
    })
  })

  return Array.from(byProduct.values())
}

const LOCAL_CAMPAIGN_PRODUCTS_KEY = "dirtywave:campaign-products"

const normalizeIdArray = (values = []) => [...new Set((Array.isArray(values) ? values : [])
  .map((id) => Number(id))
  .filter((id) => Number.isFinite(id) && id > 0))]

const normalizeLocalCampaignEntry = (entry) => {
  if (Array.isArray(entry)) {
    return {
      productIds: normalizeIdArray(entry),
      variantIds: []
    }
  }

  if (!entry || typeof entry !== "object") {
    return { productIds: [], variantIds: [] }
  }

  return {
    productIds: normalizeIdArray(entry?.productIds ?? entry?.products ?? entry?.idSanPhams),
    variantIds: normalizeIdArray(entry?.variantIds ?? entry?.variants ?? entry?.idChiTietSanPhams)
  }
}

const readLocalCampaignProductsStore = () => {
  if (typeof window === "undefined") return {}
  try {
    const raw = localStorage.getItem(LOCAL_CAMPAIGN_PRODUCTS_KEY)
    const parsed = raw ? JSON.parse(raw) : {}
    return parsed && typeof parsed === "object" ? parsed : {}
  } catch {
    return {}
  }
}

const writeLocalCampaignProductsStore = (store) => {
  if (typeof window === "undefined") return
  try {
    localStorage.setItem(LOCAL_CAMPAIGN_PRODUCTS_KEY, JSON.stringify(store || {}))
  } catch {
    // ignore localStorage errors
  }
}

const getLocalCampaignProductIds = (khuyenMaiId) => {
  const kmId = Number(khuyenMaiId || 0)
  if (!Number.isFinite(kmId) || kmId <= 0) return []
  const store = readLocalCampaignProductsStore()
  const normalized = normalizeLocalCampaignEntry(store[String(kmId)])
  return normalized.productIds
}

const getLocalCampaignVariantIds = (khuyenMaiId) => {
  const kmId = Number(khuyenMaiId || 0)
  if (!Number.isFinite(kmId) || kmId <= 0) return []
  const store = readLocalCampaignProductsStore()
  const normalized = normalizeLocalCampaignEntry(store[String(kmId)])
  return normalized.variantIds
}

const setLocalCampaignSelection = (khuyenMaiId, { productIds = [], variantIds = [] } = {}) => {
  const kmId = Number(khuyenMaiId || 0)
  if (!Number.isFinite(kmId) || kmId <= 0) return

  const store = readLocalCampaignProductsStore()
  store[String(kmId)] = {
    productIds: normalizeIdArray(productIds),
    variantIds: normalizeIdArray(variantIds)
  }
  writeLocalCampaignProductsStore(store)
}

const setLocalCampaignProductIds = (khuyenMaiId, productIds = []) => {
  setLocalCampaignSelection(khuyenMaiId, { productIds })
}

const normalizeProductIds = (values = []) => {
  return [...new Set((Array.isArray(values) ? values : [])
    .map((value) => Number(value))
    .filter((value) => Number.isFinite(value) && value > 0))]
}

const readSanPhamList = async () => {
  const sanPhamBase = await pickEndpoint("sanPham")
  const paged = await fetchAllPages(sanPhamBase)
  if (Array.isArray(paged) && paged.length) {
    return { sanPhamBase, products: paged }
  }

  const fallbackRes = await safeRequest("get", sanPhamBase, { params: { page: 0, size: 9999 } })
  return { sanPhamBase, products: unwrapList(fallbackRes) }
}

const updateProductCampaignViaSpring = async (sanPhamBase, product, nextCampaignId) => {
  const productId = Number(product?.id || 0)
  if (!Number.isFinite(productId) || productId <= 0) return false

  const normalizedCampaignId = Number(nextCampaignId || 0)
  const targetCampaign = Number.isFinite(normalizedCampaignId) && normalizedCampaignId > 0
    ? normalizedCampaignId
    : null

  const fullPayload = {
    ...(product && typeof product === "object" ? product : {}),
    id: productId,
    idKhuyenMai: targetCampaign
  }

  const payloads = [
    fullPayload,
    { idKhuyenMai: targetCampaign },
    { id: productId, idKhuyenMai: targetCampaign }
  ]

  let lastError = null
  for (const payload of payloads) {
    try {
      await axios.put(`${sanPhamBase}/${productId}`, payload)
      return true
    } catch (error) {
      lastError = error
      const status = Number(error?.response?.status || 0)
      if (status === 404 || status === 405) {
        try {
          await axios.patch(`${sanPhamBase}/${productId}`, payload)
          return true
        } catch (patchError) {
          lastError = patchError
        }
      }
    }
  }

  console.warn("[syncProductAssociationsFallback] Không thể cập nhật sản phẩm:", {
    productId,
    nextCampaignId: targetCampaign,
    message: lastError?.message || "unknown"
  })
  return false
}

const syncProductAssociationsFallback = async (khuyenMaiId, productIds = []) => {
  const kmId = Number(khuyenMaiId || 0)
  if (!Number.isFinite(kmId) || kmId <= 0) return false

  const desiredIds = new Set(normalizeProductIds(productIds))
  const { sanPhamBase, products } = await readSanPhamList()
  if (!Array.isArray(products) || !products.length) return false

  const targets = []
  for (const product of products) {
    const productId = Number(product?.id || 0)
    if (!Number.isFinite(productId) || productId <= 0) continue

    const currentCampaignId = Number(product?.idKhuyenMai ?? product?.id_khuyen_mai ?? 0)
    const shouldAssign = desiredIds.has(productId) && currentCampaignId !== kmId
    const shouldClear = !desiredIds.has(productId) && currentCampaignId === kmId

    if (shouldAssign) {
      targets.push({ product, nextCampaignId: kmId })
    } else if (shouldClear) {
      targets.push({ product, nextCampaignId: null })
    }
  }

  if (!targets.length) return true

  let successCount = 0
  for (const target of targets) {
    const ok = await updateProductCampaignViaSpring(sanPhamBase, target.product, target.nextCampaignId)
    if (ok) successCount += 1
  }

  return successCount === targets.length
}

export const discountService = {
  async getAll() {
    const base = await pickEndpoint("dotGiamGia")
    const res = await axios.get(base)
    return unwrapList(res).map(normalizeDiscountData)
  },

  async getOne(id) {
    const base = await pickEndpoint("dotGiamGia")
    const res = await axios.get(`${base}/${id}`)
    return normalizeDiscountData(res?.data ?? res)
  },

  async delete(id) {
    const base = await pickEndpoint("dotGiamGia")
    const res = await axios.delete(`${base}/${id}`)
    return res?.data ?? res
  },

  async update(id, payload) {
    const base = await pickEndpoint("dotGiamGia")
    const { idChiTietSanPhams, idSanPhams, ...discountData } = payload || {}
    const intendedVariantIds = normalizeIdArray(idChiTietSanPhams)
    const intendedProductIds = normalizeProductIds(idSanPhams)

    const boolStatus = toBoolean(discountData?.trangThai, true)
    const baseFields = {
      maKhuyenMai: readName(discountData, "maKhuyenMai", "maDotGiamGia"),
      tenKhuyenMai: readName(discountData, "tenKhuyenMai", "tenDotGiamGia"),
      giaTri: Number(discountData?.giaTri ?? discountData?.giaTriGiamGia ?? 0),
      donViGiam: "PERCENT",
      ngayBatDau: toApiDateTime(discountData?.ngayBatDau),
      ngayKetThuc: toApiDateTime(discountData?.ngayKetThuc),
      doiTuongApDung: String(discountData?.doiTuongApDung ?? discountData?.phamViApDung ?? "ALL").toUpperCase()
    }

    // Try boolean first (Spring Boot Boolean field), then string labels, then numeric.
    const payloadCandidates = [
      { id: Number(id), ...baseFields, trangThai: boolStatus },
      { id: Number(id), ...baseFields, trangThai: boolStatus ? 1 : 0 },
      { id: Number(id), ...baseFields, trangThai: toTrangThaiLabel(boolStatus) },
      { ...baseFields, trangThai: boolStatus },
      { ...baseFields, trangThai: toTrangThaiLabel(boolStatus) }
    ]

    let res = null
    let lastError = null
    for (const body of payloadCandidates) {
      try {
        res = await axios.put(`${base}/${id}`, body)
        break
      } catch (error) {
        lastError = error
        const status = Number(error?.response?.status || 0)
        if (status === 400) continue
        throw error
      }
    }

    if (!res) throw lastError || new Error("Khong cap nhat duoc dot giam gia")

    // Sync product associations via Node backend SQL endpoint
    let syncOk = false
    let resolvedProductIds = []
    const doSync = async (productIds) => {
      const normalizedProductIds = normalizeProductIds(productIds)
      resolvedProductIds = normalizedProductIds
      const syncBase = NODE_BACKEND || API_ORIGIN

      try {
        await axios.post(`${syncBase}/api/khuyen-mai-products/sync`, {
          khuyenMaiId: Number(id),
          sanPhamIds: normalizedProductIds
        })
        syncOk = true
        return
      } catch (syncErr) {
        console.warn('[updateDiscount] Node sync endpoint failed, fallback to Spring update:', syncErr?.message)
      }

      const fallbackOk = await syncProductAssociationsFallback(Number(id), normalizedProductIds)
      if (!fallbackOk) {
        throw new Error('SYNC_FALLBACK_FAILED')
      }
      syncOk = true
    }

    if (Array.isArray(idSanPhams) && idSanPhams.length > 0) {
      try {
        await doSync(idSanPhams)
      } catch (syncErr) {
        console.warn('[updateDiscount] Sync idSanPhams failed:', syncErr?.message)
        if (Array.isArray(idChiTietSanPhams) && idChiTietSanPhams.length > 0) {
          try {
            const syncResult = await this.syncProductAssociations(id, idChiTietSanPhams)
            resolvedProductIds = normalizeProductIds(syncResult?.productIds || [])
            syncOk = true
          } catch { /* */ }
        }
      }
    } else if (Array.isArray(idChiTietSanPhams) && idChiTietSanPhams.length > 0) {
      try {
        const syncResult = await this.syncProductAssociations(id, idChiTietSanPhams)
        resolvedProductIds = normalizeProductIds(syncResult?.productIds || [])
        syncOk = true
      } catch { /* */ }
    } else if ((Array.isArray(idChiTietSanPhams) && idChiTietSanPhams.length === 0) ||
               (Array.isArray(idSanPhams) && idSanPhams.length === 0)) {
      try {
        await doSync([])
        resolvedProductIds = []
        syncOk = true
      } catch { /* */ }
    } else {
      syncOk = true // no sync needed
    }

    if ((Array.isArray(idChiTietSanPhams) || Array.isArray(idSanPhams))) {
      setLocalCampaignSelection(Number(id), {
        productIds: resolvedProductIds.length > 0 ? resolvedProductIds : intendedProductIds,
        variantIds: intendedVariantIds
      })
    }

    const result = res?.data ?? res
    if (!syncOk) {
      result._syncFailed = true
    }
    return result
  },

  // Convert variant IDs to product IDs and call sync endpoint
  async syncProductAssociations(khuyenMaiId, variantIds) {
    const allVariants = await this.getAllProductDetails()
    const variantIdSet = new Set((Array.isArray(variantIds) ? variantIds : [])
      .map((v) => Number(v)).filter((v) => Number.isFinite(v) && v > 0))

    const productIds = [...new Set(
      allVariants
        .filter((v) => variantIdSet.has(Number(v?.id)))
        .map((v) => Number(v?.idSanPham))
        .filter((v) => Number.isFinite(v) && v > 0)
    )]

    if (variantIdSet.size > 0 && productIds.length === 0) {
      console.warn('[syncProductAssociations] Không tìm được product ID nào từ variant IDs:', [...variantIdSet])
    }

    const syncBase = NODE_BACKEND || API_ORIGIN
    try {
      await axios.post(`${syncBase}/api/khuyen-mai-products/sync`, {
        khuyenMaiId: Number(khuyenMaiId),
        sanPhamIds: productIds
      })
    } catch (syncErr) {
      console.warn('[syncProductAssociations] Node sync endpoint failed, fallback to Spring update:', syncErr?.message)
      const fallbackOk = await syncProductAssociationsFallback(Number(khuyenMaiId), productIds)
      if (!fallbackOk) throw syncErr
    }

    return { khuyenMaiId, productIds }
  },

  // Get products associated with a discount (from Node backend SQL)
  async getAssociatedProducts(khuyenMaiId) {
    const kmId = Number(khuyenMaiId || 0)
    if (!Number.isFinite(kmId) || kmId <= 0) return []

    try {
      const syncBase = NODE_BACKEND || API_ORIGIN
      const res = await axios.get(`${syncBase}/api/khuyen-mai-products/${kmId}`)
      const rows = Array.isArray(res?.data) ? res.data : []
      if (rows.length > 0) return rows
    } catch {
      // Fallback below for environments where Node backend is not running.
    }

    try {
      const allVariants = await this.getAllProductDetails()
      const mapped = mapAssociatedProductsFromVariants(allVariants, kmId)
      if (mapped.length > 0) return mapped
    } catch {
      // fallback below
    }

    const localProductIds = getLocalCampaignProductIds(kmId)
    if (localProductIds.length > 0) {
      return localProductIds.map((id) => ({ id, idSanPham: id, idKhuyenMai: kmId }))
    }

    const localVariantIds = getLocalCampaignVariantIds(kmId)
    if (localVariantIds.length > 0) {
      try {
        const allVariants = await this.getAllProductDetails()
        const variantSet = new Set(localVariantIds)
        const productIds = [...new Set(allVariants
          .filter((v) => variantSet.has(Number(v?.id)))
          .map((v) => Number(v?.idSanPham))
          .filter((id) => Number.isFinite(id) && id > 0))]

        if (productIds.length > 0) {
          return productIds.map((id) => ({ id, idSanPham: id, idKhuyenMai: kmId }))
        }
      } catch {
        // ignore and fallback to empty
      }
    }

    return []
  },

  async getDiscountDetails(idDotGiamGia) {
    // Return associated products from the Node backend SQL endpoint
    return this.getAssociatedProducts(idDotGiamGia)
  },

  async getAppliedProductCount(discountId) {
    const details = await this.getDiscountDetails(discountId)
    if (!Array.isArray(details)) return 0

    const uniqueIds = new Set(
      details
        .map((d) => Number(d?.idChiTietSanPham ?? d?.id_chi_tiet_san_pham ?? d?.chiTietSanPham?.id ?? 0))
        .filter((id) => Number.isFinite(id) && id > 0)
    )

    return uniqueIds.size
  },

  async getAllProductDetails() {
    const [
      ctspBase,
      sanPhamBase,
      mauSacBase,
      kichThuocBase,
      thuongHieuBase,
      chatLieuBase,
      loaiSanBase,
      xuatXuBase,
      anhBase
    ] = await Promise.all([
      pickEndpoint("chiTietSanPham"),
      pickEndpoint("sanPham"),
      pickEndpoint("mauSac"),
      pickEndpoint("kichThuoc"),
      pickEndpoint("thuongHieu"),
      pickEndpoint("chatLieu"),
      pickEndpoint("loaiSan"),
      pickEndpoint("xuatXu"),
      pickEndpoint("anhChiTietSanPham")
    ])

    const [ctspRes, spRes, mauSacRes, kichThuocRes, thuongHieuRes, chatLieuRes, loaiSanRes, xuatXuRes, anhRes] =
      await Promise.all([
        safeRequest("get", ctspBase, { params: { page: 0, size: 9999 } }),
        fetchAllPages(sanPhamBase),
        safeRequest("get", mauSacBase, { params: { page: 0, size: 9999 } }),
        safeRequest("get", kichThuocBase, { params: { page: 0, size: 9999 } }),
        safeRequest("get", thuongHieuBase, { params: { page: 0, size: 9999 } }),
        safeRequest("get", chatLieuBase, { params: { page: 0, size: 9999 } }),
        safeRequest("get", loaiSanBase, { params: { page: 0, size: 9999 } }),
        safeRequest("get", xuatXuBase, { params: { page: 0, size: 9999 } }),
        fetchAllPages(anhBase)
      ])

    let ctsp = unwrapList(ctspRes)
    const sanPham = Array.isArray(spRes) ? spRes : unwrapList(spRes)
    const mauSac = unwrapList(mauSacRes)
    const kichThuoc = unwrapList(kichThuocRes)
    const thuongHieu = unwrapList(thuongHieuRes)
    const chatLieu = unwrapList(chatLieuRes)
    const loaiSan = unwrapList(loaiSanRes)
    const xuatXu = unwrapList(xuatXuRes)
    const anhList = Array.isArray(anhRes) ? anhRes : unwrapList(anhRes)

    const sanPhamMap = new Map()
    sanPham.forEach((item) => sanPhamMap.set(Number(item?.id), item))

    const mauSacMap = mapById(mauSac, "tenMauSac", "tenMau", "name")
    const kichThuocMap = mapById(kichThuoc, "tenKichThuoc", "name")
    const thuongHieuMap = mapById(thuongHieu, "tenThuongHieu", "tenHang", "name")
    const chatLieuMap = mapById(chatLieu, "tenChatLieu", "name")
    const loaiSanMap = mapById(loaiSan, "tenLoaiSan", "tenLoai", "name")
    const xuatXuMap = mapById(xuatXu, "tenXuatXu", "tenXuatSu", "name")

    const imageByCtspId = new Map()
    anhList.forEach((image) => {
      const ctspId = Number(image?.idChiTietSanPham ?? image?.id_chi_tiet_san_pham)
      if (!Number.isFinite(ctspId)) return

      const current = imageByCtspId.get(ctspId)
      const isRepresentative = toBoolean(image?.laAnhDaiDien ?? image?.anhDaiDien, false)
      if (!current || isRepresentative) {
        const rawPath = readName(image, "duongDanAnh", "urlAnh", "url", "image")
        imageByCtspId.set(ctspId, normalizeImageUrl(rawPath))
      }
    })

    // Always derive variants from /san-pham payload and merge with ctsp endpoint.
    // This prevents truncated datasets when chi-tiet endpoint returns only first page.
    const derived = []
    if (sanPham.length) {
      sanPham.forEach((parent) => {
        const parentId = Number(parent?.id ?? parent?.sanPhamId ?? 0)
        toVariantList(parent).forEach((variant) => {
          derived.push({
            ...variant,
            idSanPham: Number(variant?.idSanPham ?? variant?.sanPhamId ?? parentId),
            __parent: parent
          })
        })
      })
    }

    if (!ctsp.length && derived.length) {
      ctsp = derived
    } else if (ctsp.length && derived.length) {
      const keyOf = (row = {}) => {
        const id = Number(row?.id)
        if (Number.isFinite(id) && id > 0) return `id:${id}`
        const ma = readName(row, "maChiTietSanPham", "maSanPhamChiTiet", "ma", "ma_chi_tiet_san_pham")
        if (ma) return `ma:${ma.toUpperCase()}`
        const p = Number(row?.idSanPham ?? row?.sanPhamId ?? row?.sanPham?.id)
        const m = Number(row?.idMauSac ?? row?.id_mau_sac ?? row?.mauSac?.id)
        const k = Number(row?.idKichThuoc ?? row?.id_kich_thuoc ?? row?.kichThuoc?.id)
        if (Number.isFinite(p) && Number.isFinite(m) && Number.isFinite(k)) return `pmk:${p}-${m}-${k}`
        return ""
      }

      const map = new Map()
      derived.forEach((row) => {
        const key = keyOf(row)
        if (key) map.set(key, row)
      })

      ctsp.forEach((row) => {
        const key = keyOf(row)
        if (!key) return
        const prev = map.get(key) || {}
        map.set(key, {
          ...prev,
          ...row,
          __parent: row?.__parent || prev?.__parent
        })
      })

      ctsp = Array.from(map.values())
    }

    const normalizedRows = ctsp.map((item) => {
      const id = Number(item?.id)
      const idSanPham = Number(item?.idSanPham ?? item?.id_san_pham ?? item?.sanPham?.id)
      const parent = item?.__parent || sanPhamMap.get(idSanPham) || {}

      const idThuongHieu = Number(parent?.idThuongHieu ?? parent?.id_thuong_hieu)
      const idChatLieu = Number(parent?.idChatLieu ?? parent?.id_chat_lieu)
      const idXuatXu = Number(parent?.idXuatXu ?? parent?.id_xuat_xu)

      const giaBan = Number(item?.giaBan ?? item?.gia_ban ?? item?.giaNiemYet ?? item?.gia_niem_yet ?? 0)
      const variantImage = imageByCtspId.get(id)
        || normalizeImageUrl(readName(item, "anh", "urlAnh", "duongDanAnh", "anhDaiDien", "hinhAnh", "image", "imageUrl", "thumbnail"))
        || ""
      const parentImage = normalizeImageUrl(readName(parent, "anh", "hinhAnh", "anhDaiDien", "urlAnh", "imageUrl", "thumbnail"))
        || ""

      return {
        ...item,
        id,
        idSanPham,
        idKhuyenMai: Number(parent?.idKhuyenMai ?? parent?.id_khuyen_mai ?? item?.idKhuyenMai ?? item?.id_khuyen_mai ?? 0) || null,
        maSanPham: readName(parent, "maSanPham", "ma_san_pham") || `SP-${idSanPham || "N/A"}`,
        tenSanPham: readName(parent, "tenSanPham", "ten_san_pham") || readName(item, "tenSanPham", "ten_san_pham") || "Sản phẩm",
        maChiTietSanPham: readName(item, "maChiTietSanPham", "maSanPhamChiTiet", "ma", "ma_chi_tiet_san_pham") || `CTSP-${id || "N/A"}`,
        tenMauSac:
          readName(item?.mauSac, "tenMauSac", "tenMau", "name") ||
          mauSacMap.get(Number(item?.idMauSac ?? item?.id_mau_sac ?? item?.mauSac?.id)) ||
          "Không rõ",
        tenKichThuoc:
          readName(item?.kichThuoc, "tenKichThuoc", "name") ||
          kichThuocMap.get(Number(item?.idKichThuoc ?? item?.id_kich_thuoc ?? item?.kichThuoc?.id)) ||
          "Không rõ",
        tenLoaiSan:
          readName(item?.loaiSan, "tenLoaiSan", "tenLoai", "name") ||
          readName(item?.loai, "tenLoaiSan", "tenLoai", "name") ||
          loaiSanMap.get(Number(item?.idLoaiSan ?? item?.id_loai_san ?? item?.loaiSan?.id ?? item?.loai?.id)) ||
          "Không rõ",
        tenThuongHieu:
          readName(parent?.thuongHieu, "tenThuongHieu", "tenHang", "name") ||
          readName(parent?.hang, "tenThuongHieu", "tenHang", "name") ||
          readName(item?.hang, "tenThuongHieu", "tenHang", "name") ||
          thuongHieuMap.get(idThuongHieu) ||
          "Không rõ",
        tenChatLieu: readName(parent?.chatLieu, "tenChatLieu", "name") || chatLieuMap.get(idChatLieu) || "Không rõ",
        tenXuatXu:
          readName(parent?.xuatXu, "tenXuatXu", "tenXuatSu", "name") ||
          readName(parent?.xuatSu, "tenXuatXu", "tenXuatSu", "name") ||
          readName(item?.xuatXu, "tenXuatXu", "tenXuatSu", "name") ||
          readName(item?.xuatSu, "tenXuatXu", "tenXuatSu", "name") ||
          xuatXuMap.get(idXuatXu) ||
          "Không rõ",
        soLuong: Number(item?.soLuong ?? item?.so_luong ?? 0),
        giaNiemYet: giaBan,
        giaBan,
        anhBienThe: variantImage,
        anhSanPham: parentImage,
        anh: variantImage || parentImage || ""
      }
    })

    const deduped = new Map()
    normalizedRows.forEach((row) => {
      const key = buildVariantStableKey(row)
      const prev = deduped.get(key)
      if (!prev) {
        deduped.set(key, row)
        return
      }

      deduped.set(key, {
        ...prev,
        ...row,
        anhBienThe: row?.anhBienThe || prev?.anhBienThe || "",
        anhSanPham: row?.anhSanPham || prev?.anhSanPham || "",
        anh: row?.anh || prev?.anh || "",
        idKhuyenMai: row?.idKhuyenMai ?? prev?.idKhuyenMai ?? null,
      })
    })

    return sortVariantsStable(Array.from(deduped.values()))
  },

  async createDiscountComposite(payload) {
    const base = await pickEndpoint("dotGiamGia")

    const { idChiTietSanPhams, ...discountData } = payload || {}

    const normalizedPayload = {
      maKhuyenMai: readName(discountData, "maKhuyenMai", "maDotGiamGia"),
      tenKhuyenMai: readName(discountData, "tenKhuyenMai", "tenDotGiamGia"),
      giaTri: Number(discountData?.giaTri ?? discountData?.giaTriGiamGia ?? 0),
      donViGiam: "PERCENT",
      ngayBatDau: toApiDateTime(discountData?.ngayBatDau),
      ngayKetThuc: toApiDateTime(discountData?.ngayKetThuc),
      doiTuongApDung: String(discountData?.doiTuongApDung ?? discountData?.phamViApDung ?? "ALL").toUpperCase(),
      trangThai: toTrangThaiLabel(discountData?.trangThai, true)
    }

    const createRes = await axios.post(base, normalizedPayload)
    const created = createRes?.data ?? createRes
    const discountId = readId(created)

    if (!Number.isFinite(discountId) || discountId <= 0) {
      throw new Error("Khong tao duoc dot giam gia")
    }

    const ctspIds = normalizeIdArray(idChiTietSanPhams)

    let resolvedProductIds = []

    if (ctspIds.length) {
      try {
        const syncResult = await this.syncProductAssociations(discountId, ctspIds)
        resolvedProductIds = normalizeProductIds(syncResult?.productIds || [])
      } catch (syncErr) {
        console.warn('[createDiscountComposite] Đồng bộ sản phẩm thất bại (không chặn tạo mới):', syncErr)
        created._syncFailed = true
      }
    }

    setLocalCampaignSelection(discountId, {
      productIds: resolvedProductIds,
      variantIds: ctspIds
    })

    return created
  },

  getLocalAssociatedVariantIds(khuyenMaiId) {
    return getLocalCampaignVariantIds(khuyenMaiId)
  }
}

export default discountService
