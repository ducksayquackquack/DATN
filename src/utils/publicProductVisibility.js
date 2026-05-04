const CURATED_PRODUCT_CODE_LIMIT = 20

const IMAGE_EXTENSIONS = /\.(png|jpe?g|gif|webp|svg|bmp|avif)$/i

const normalizeCatalogProductCode = (value = "") => {
  const raw = String(value || "").trim().toUpperCase()
  if (!raw) return ""

  const spMatch = raw.match(/^SP0*(\d{1,3})$/i)
  if (spMatch?.[1]) {
    const number = Number(spMatch[1])
    if (Number.isFinite(number) && number >= 1) return `SP${String(number).padStart(3, "0")}`
  }

  const legacyMatch = raw.match(/^ATID070-0*(\d{1,3})$/i)
  if (legacyMatch?.[1]) {
    const number = Number(legacyMatch[1])
    if (Number.isFinite(number) && number >= 1) return `SP${String(number).padStart(3, "0")}`
  }

  return raw
}

const isAbsoluteUrl = (value = "") => /^https?:\/\//i.test(value) || /^data:image\//i.test(value)

const isImageLikeString = (value = "") => {
  const raw = String(value || "").trim()
  if (!raw) return false
  if (isAbsoluteUrl(raw)) return true

  const normalized = raw.replace(/\\/g, "/").split(/[?#]/)[0]
  if (normalized.startsWith("/uploads/") || normalized.startsWith("uploads/")) return true
  return IMAGE_EXTENSIONS.test(normalized)
}

const collectImageCandidates = (entry, bucket = []) => {
  if (!entry) return bucket

  if (typeof entry === "string") {
    if (isImageLikeString(entry)) bucket.push(entry)
    return bucket
  }

  if (Array.isArray(entry)) {
    entry.forEach((child) => collectImageCandidates(child, bucket))
    return bucket
  }

  if (typeof entry === "object") {
    const keys = [
      "anh",
      "hinhAnh",
      "image",
      "imageUrl",
      "url",
      "duongDan",
      "duongDanAnh",
      "link",
      "path",
      "images",
      "listAnh",
      "anhChinh",
      "sanPhamChiTiets",
    ]

    for (const key of keys) {
      collectImageCandidates(entry[key], bucket)
    }
  }

  return bucket
}

const isUploadedImageCandidate = (value = "") => {
  const raw = String(value || "").trim()
  if (!raw) return false

  const normalized = raw.replace(/\\/g, "/")
  if (normalized.startsWith("/uploads/") || normalized.startsWith("uploads/")) return true

  try {
    const url = new URL(raw)
    return url.pathname.toLowerCase().includes("/uploads/")
  } catch {
    return normalized.toLowerCase().includes("/uploads/")
  }
}

export const hasRealPublicProductImage = (product) => {
  const candidates = collectImageCandidates(product)
  return candidates.some((candidate) => isUploadedImageCandidate(candidate))
}

export const shouldDisplayPublicProduct = (product) => {
  const normalizedCode = normalizeCatalogProductCode(product?.maSanPham || product?.ma || "")
  const codeMatch = normalizedCode.match(/^SP(\d{3})$/)
  const codeNumber = Number(codeMatch?.[1] || 0)

  if (Number.isFinite(codeNumber) && codeNumber >= 1 && codeNumber <= CURATED_PRODUCT_CODE_LIMIT) {
    return true
  }

  return hasRealPublicProductImage(product)
}
