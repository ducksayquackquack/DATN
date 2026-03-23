const STORAGE_KEY = "dirtywave:product-image-overrides:v1"

function readStore() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return {}
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === "object" ? parsed : {}
  } catch {
    return {}
  }
}

function writeStore(store) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(store))
  } catch {
    // Ignore storage write failures to avoid blocking the UI.
  }
}

function normalizeImageList(images = []) {
  return [...new Set(
    (Array.isArray(images) ? images : [images])
      .map((item) => String(item || "").trim())
      .filter(Boolean)
  )]
}

function normalizeColorImageEntries(entries = []) {
  const list = Array.isArray(entries) ? entries : []
  return list
    .map((entry, index) => ({
      colorId: Number(entry?.colorId),
      image: String(entry?.image || '').trim(),
      order: Number.isFinite(Number(entry?.order)) ? Number(entry.order) : index
    }))
    .filter((entry) => Number.isFinite(entry.colorId) && entry.colorId > 0 && entry.image)
    .sort((left, right) => left.order - right.order)
}

function normalizeStoreEntry(entry) {
  if (Array.isArray(entry)) {
    return {
      images: normalizeImageList(entry),
      colorImages: []
    }
  }

  if (entry && typeof entry === 'object') {
    return {
      images: normalizeImageList(entry.images || entry.gallery || entry.listAnh || entry),
      colorImages: normalizeColorImageEntries(entry.colorImages || entry.mauSacHinhAnhs || entry.anhTheoMauSac)
    }
  }

  return {
    images: [],
    colorImages: []
  }
}

function buildLookupKeys(productLike) {
  const source = productLike && typeof productLike === "object"
    ? productLike
    : { id: productLike }

  const numericId = Number(source?.id)
  const code = String(source?.maSanPham || source?.ma || source?.code || "").trim()
  const keys = []

  if (Number.isFinite(numericId) && numericId > 0) {
    keys.push(`id:${numericId}`)
  }

  if (code) {
    keys.push(`code:${code.toUpperCase()}`)
  }

  return keys
}

export function getProductImageOverride(productLike) {
  const store = readStore()
  for (const key of buildLookupKeys(productLike)) {
    const entry = normalizeStoreEntry(store[key])
    if (entry.images.length) return entry.images
  }
  return []
}

export function getProductImageConfig(productLike) {
  const store = readStore()
  for (const key of buildLookupKeys(productLike)) {
    const entry = normalizeStoreEntry(store[key])
    if (entry.images.length || entry.colorImages.length) return entry
  }

  return {
    images: [],
    colorImages: []
  }
}

export function setProductImageOverride(productLike, images) {
  setProductImageConfig(productLike, { images })
}

export function setProductImageConfig(productLike, config = {}) {
  const normalizedImages = normalizeImageList(config?.images || [])
  const normalizedColorImages = normalizeColorImageEntries(config?.colorImages || [])
  const keys = buildLookupKeys(productLike)
  if (!keys.length) return

  const store = readStore()
  for (const key of keys) {
    if (normalizedImages.length || normalizedColorImages.length) {
      store[key] = {
        images: normalizedImages,
        colorImages: normalizedColorImages
      }
    } else {
      delete store[key]
    }
  }
  writeStore(store)
}

export function clearProductImageOverride(productLike) {
  setProductImageOverride(productLike, [])
}
