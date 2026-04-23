import { getAllHoaDon, getHoaDonById } from '@/services/hoaDonService'

const _NODE_BACKEND_URL = String(
  typeof import.meta !== 'undefined' ? (import.meta.env?.VITE_NODE_BACKEND_URL || '') : ''
).trim().replace(/\/$/, '') || 'http://localhost:3000'

/* ------------------------------------------------------------------ */
/*  Internal helpers                                                   */
/* ------------------------------------------------------------------ */

function normalizeStatusText(str) {
  return String(str || '')
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
}

function extractItems(source) {
  const candidates = [
    source?.items,
    source?.hoaDonChiTiets,
    source?.chiTietHoaDons,
    source?.chiTiets,
    source?.chiTietDonHang,
    source?.details,
    source?.lineItems,
    source?.data?.items,
    source?.data?.hoaDonChiTiets,
    source?.data?.chiTietHoaDons,
    source?.data?.chiTietDonHang,
    source?.data?.details,
    source?.data?.lineItems,
  ];

  for (const value of candidates) {
    if (Array.isArray(value) && value.length) return value;
  }

  return [];
}

function extractSpctId(item) {
  return Number(
    item?.spctId
    || item?.sanPhamChiTietId
    || item?.idSanPhamChiTiet
    || item?.chiTietSanPhamId
    || item?.idSpct
    || item?.spIdCt
    || item?.variantId
    || item?.idBienThe
    || item?.sanPhamChiTiet?.id
    || item?.variant?.id
    || 0
  )
}

function extractItemQty(item) {
  return Number(
    item?.soLuong
    || item?.quantity
    || item?.soLuongMua
    || item?.qty
    || item?.soLuongDat
    || item?.soLuongBan
    || 0
  )
}

/* ------------------------------------------------------------------ */
/*  Exported: shouldCountOrderForStock                                 */
/* ------------------------------------------------------------------ */

/**
 * Determine whether an order/invoice detail should be counted toward
 * sold stock.  Combines all field checks used across admin + customer.
 */
export function shouldCountOrderForStock(detail = {}) {
  const order = detail?.hoaDon || detail || {}
  const statusCode = String(order?.orderStatusCode || detail?.orderStatusCode || '').trim().toUpperCase()
  const fulfillmentCode = String(order?.fulfillmentStatusCode || detail?.fulfillmentStatusCode || '').trim().toUpperCase()
  const statusText = normalizeStatusText(
    order?.orderStatusName || order?.trangThai || order?.status || ''
  )
  const noteText = normalizeStatusText(order?.statusNote || detail?.statusNote || '')

  // Canceled → never count
  if (
    statusCode.includes('HUY') || statusText.includes('huy')
    || noteText.includes('huy') || statusText.includes('cancel')
  ) {
    return false
  }

  // Returned/failed deliveries should not keep reducing available stock.
  if (
    statusCode.includes('GIAO_THAT_BAI')
    || statusCode.includes('HOAN_VE')
    || statusCode.includes('TRA_HANG')
    || fulfillmentCode.includes('RETURN')
    || fulfillmentCode.includes('FAILED')
    || statusText.includes('that bai')
    || statusText.includes('hoan ve')
    || statusText.includes('tra hang')
    || noteText.includes('that bai')
    || noteText.includes('hoan ve')
    || noteText.includes('tra hang')
  ) {
    return false
  }

  // Count all remaining states as sold impact (pending/confirmed/shipping/completed...).
  return true
}

/* ------------------------------------------------------------------ */
/*  Exported: computeSoldBySpct                                        */
/* ------------------------------------------------------------------ */

/**
 * Fetch ALL invoices, then fetch detail for each (batched by 10),
 * and return Map<spctId, totalSoldQty>.
 */
export async function computeSoldBySpct() {
  try {
    const allRes = await getAllHoaDon()
    const invoices = Array.isArray(allRes?.data)
      ? allRes.data
      : allRes?.data?.content || []
    const orderIds = invoices
      .map((inv) => Number(inv?.id))
      .filter((id) => Number.isFinite(id) && id > 0)
    if (!orderIds.length) return new Map()

    const map = new Map()
    for (let i = 0; i < orderIds.length; i += 10) {
      const batch = orderIds.slice(i, i + 10)
      const results = await Promise.all(
        batch.map((id) => getHoaDonById(id).catch(() => null))
      )
      for (const detailRes of results) {
        const detail = detailRes?.data
        if (!detail || !shouldCountOrderForStock(detail)) continue
        for (const item of extractItems(detail)) {
          const spctId = extractSpctId(item)
          const qty = extractItemQty(item)
          if (spctId > 0 && qty > 0) {
            map.set(spctId, (map.get(spctId) || 0) + qty)
          }
        }
      }
    }
    return map
  } catch {
    return new Map()
  }
}

/**
 * Same as computeSoldBySpct but accepts a pre-loaded invoices array
 * (for pages that already fetched invoices, e.g., AdminHome).
 * Fetches detail only for invoices missing inline items. Batched by 10.
 */
export async function computeSoldBySpctFromInvoices(invoices = []) {
  try {
    const completedInvoices = invoices.filter((inv) => shouldCountOrderForStock(inv))
    const idsToFetch = completedInvoices
      .filter((inv) => !extractItems(inv).length)
      .map((inv) => Number(inv?.id))
      .filter((id) => Number.isFinite(id) && id > 0)

    const detailById = new Map()
    for (let i = 0; i < idsToFetch.length; i += 10) {
      const batch = idsToFetch.slice(i, i + 10)
      const results = await Promise.all(
        batch.map((id) => getHoaDonById(id).catch(() => null))
      )
      for (const res of results) {
        const data = res?.data
        if (data?.id) detailById.set(Number(data.id), data)
      }
    }

    const map = new Map()
    for (const inv of completedInvoices) {
      let items = extractItems(inv)
      if (!items.length) {
        const enriched = detailById.get(Number(inv?.id))
        if (enriched) items = extractItems(enriched)
      }
      for (const item of items) {
        const spctId = extractSpctId(item)
        const qty = extractItemQty(item)
        if (spctId > 0 && qty > 0) {
          map.set(spctId, (map.get(spctId) || 0) + qty)
        }
      }
    }
    return map
  } catch {
    return new Map()
  }
}

/* ------------------------------------------------------------------ */
/*  Exported: computeSoldBySpctDirect                                  */
/* ------------------------------------------------------------------ */

/**
 * Fetch sold-by-variant data from the Node.js backend (port 3000).
 * Unlike computeSoldBySpct(), this does NOT require Spring Security admin role.
 * Falls back to empty Map if Node.js is not running or the endpoint fails.
 */
export async function computeSoldBySpctDirect() {
  try {
    const res = await fetch(`${_NODE_BACKEND_URL}/api/stock/sold-by-variant`)
    if (!res.ok) return new Map()
    const data = await res.json()
    if (!Array.isArray(data)) return new Map()
    const map = new Map()
    for (const row of data) {
      const spctId = Number(row?.spctId)
      const sold = Number(row?.sold)
      if (spctId > 0 && sold > 0) {
        map.set(spctId, sold)
      }
    }
    return map
  } catch {
    return new Map()
  }
}

/* ------------------------------------------------------------------ */
/*  Exported: variant stock helpers                                    */
/* ------------------------------------------------------------------ */

/**
 * Base stock value from a variant object.  soLuong is the primary field
 * returned by the backend API.
 */
export function variantStockValue(variant) {
  return Number(variant?.soLuong ?? variant?.soLuongTon ?? variant?.tonKho ?? variant?.ton ?? 0)
}

function variantSoldValue(variant) {
  return Number(
    variant?.soLuongDaBan
    ?? variant?.daBan
    ?? variant?.sold
    ?? variant?.soldCount
    ?? variant?.soLuongBan
    ?? 0
  )
}

/**
 * Available stock = base stock − sold qty.
 * @param {Object} variant
 * @param {Map} soldBySpctMap  Map<spctId, totalSoldQty>
 */
export function variantAvailableStock(variant, soldBySpctMap = new Map()) {
  const baseStock = variantStockValue(variant)
  const spctId = Number(
    variant?.id || variant?.spctId || variant?.sanPhamChiTietId || variant?.chiTietSanPhamId || 0
  )
  const soldFromVariant = Math.max(0, variantSoldValue(variant))
  if (!Number.isFinite(spctId) || spctId <= 0) {
    return Math.max(0, baseStock - soldFromVariant)
  }

  const soldFromMap = Number(soldBySpctMap.get(spctId) || 0)
  const soldQty = Math.max(soldFromMap, soldFromVariant)
  return Math.max(0, baseStock - soldQty)
}
