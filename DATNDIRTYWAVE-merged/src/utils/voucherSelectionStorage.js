const CHECKOUT_ITEM_VOUCHER_SELECTIONS_KEY = "checkoutItemVoucherSelections"
const CHECKOUT_PENDING_ITEM_VOUCHERS_KEY = "checkoutPendingItemVoucherSelections"

const safeParseObject = (raw) => {
  try {
    const parsed = raw ? JSON.parse(raw) : {}
    return parsed && typeof parsed === "object" && !Array.isArray(parsed) ? parsed : {}
  } catch {
    return {}
  }
}

const normalizeVoucherCode = (value) => String(value || "").trim().toUpperCase()

const parseLineKey = (key) => {
  const raw = String(key || "")
  const [idPart = "", color = "", size = "", voucherPart = ""] = raw.split("__")
  return {
    id: String(idPart || "").trim(),
    color: String(color || "").trim(),
    size: String(size || "").trim(),
    voucherCode: normalizeVoucherCode(voucherPart),
  }
}

const buildLineKey = (id, color = "", size = "", voucherCode = "") => {
  const base = String(id || "").trim()
  if (!base) return ""
  const normalizedCode = normalizeVoucherCode(voucherCode)
  return `${base}__${String(color || "").trim()}__${String(size || "").trim()}__${normalizedCode}`
}

const extractVoucherCode = (payload) => {
  if (!payload) return ""
  if (typeof payload === "string") return normalizeVoucherCode(payload)
  return normalizeVoucherCode(
    payload?.maPhieuGiamGia
      || payload?.voucherCode
      || payload?.maVoucher
      || payload?.code
      || payload?.voucher?.maPhieuGiamGia
      || payload?.voucher?.voucherCode
      || payload?.voucher?.maVoucher
      || payload?.voucher?.code
      || payload?.target?.voucherCode
      || ""
  )
}

const ensureVoucherCodeOnPayload = (payload, voucherCode) => {
  const normalizedCode = normalizeVoucherCode(voucherCode)
  if (!normalizedCode) return null

  if (!payload || typeof payload !== "object") {
    return { maPhieuGiamGia: normalizedCode, voucherCode: normalizedCode }
  }

  const next = { ...payload }
  if (next.voucher && typeof next.voucher === "object") {
    next.voucher = {
      ...next.voucher,
      maPhieuGiamGia: normalizedCode,
      voucherCode: normalizedCode,
    }
  } else {
    next.maPhieuGiamGia = normalizedCode
    next.voucherCode = normalizedCode
  }
  return next
}

const sanitizeSelectionMap = (mapValue) => {
  const source = mapValue && typeof mapValue === "object" ? mapValue : {}
  const next = {}

  for (const [rawKey, payload] of Object.entries(source)) {
    const parsedKey = parseLineKey(rawKey)
    if (!parsedKey.id) continue

    const codeFromPayload = extractVoucherCode(payload)
    const finalCode = parsedKey.voucherCode || codeFromPayload
    if (!finalCode) continue

    const normalizedKey = buildLineKey(parsedKey.id, parsedKey.color, parsedKey.size, finalCode)
    if (!normalizedKey) continue

    const normalizedPayload = ensureVoucherCodeOnPayload(payload, finalCode)
    if (!normalizedPayload) continue
    next[normalizedKey] = normalizedPayload
  }

  return next
}

const sanitizePendingMap = (mapValue) => {
  const source = mapValue && typeof mapValue === "object" ? mapValue : {}
  const next = {}

  for (const [rawKey, payload] of Object.entries(source)) {
    const parsedKey = parseLineKey(rawKey)
    const sourcePayload = payload && typeof payload === "object" ? payload : {}
    const target = sourcePayload?.target && typeof sourcePayload.target === "object" ? sourcePayload.target : {}

    const targetId = String(target?.productId || parsedKey.id || "").trim()
    if (!targetId) continue

    const targetColor = String(target?.color || parsedKey.color || "").trim()
    const targetSize = String(target?.size || parsedKey.size || "").trim()
    const codeFromPayload = extractVoucherCode(sourcePayload)
    const finalCode = normalizeVoucherCode(target?.voucherCode || parsedKey.voucherCode || codeFromPayload)
    if (!finalCode) continue

    const normalizedKey = buildLineKey(targetId, targetColor, targetSize, finalCode)
    if (!normalizedKey) continue

    const normalizedVoucher = ensureVoucherCodeOnPayload(sourcePayload?.voucher || sourcePayload, finalCode)
    if (!normalizedVoucher) continue

    next[normalizedKey] = {
      ...sourcePayload,
      voucher: normalizedVoucher,
      target: {
        ...target,
        productId: Number(targetId) || targetId,
        color: targetColor,
        size: targetSize,
        voucherCode: finalCode,
      },
    }
  }

  return next
}

export const cleanLegacyVoucherStorage = () => {
  try {
    const selectionRaw = localStorage.getItem(CHECKOUT_ITEM_VOUCHER_SELECTIONS_KEY)
    const pendingRaw = localStorage.getItem(CHECKOUT_PENDING_ITEM_VOUCHERS_KEY)

    const selectionMap = safeParseObject(selectionRaw)
    const pendingMap = safeParseObject(pendingRaw)

    const cleanedSelections = sanitizeSelectionMap(selectionMap)
    const cleanedPending = sanitizePendingMap(pendingMap)

    const nextSelectionRaw = JSON.stringify(cleanedSelections)
    const nextPendingRaw = JSON.stringify(cleanedPending)

    if (selectionRaw !== nextSelectionRaw) {
      localStorage.setItem(CHECKOUT_ITEM_VOUCHER_SELECTIONS_KEY, nextSelectionRaw)
    }
    if (pendingRaw !== nextPendingRaw) {
      localStorage.setItem(CHECKOUT_PENDING_ITEM_VOUCHERS_KEY, nextPendingRaw)
    }
  } catch {
    // Ignore storage errors.
  }
}
