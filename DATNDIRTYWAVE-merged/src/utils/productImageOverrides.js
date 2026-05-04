import img14c from "@/assets/img/Jackets/bomber/bomber-windbreaker/bomer-windbreaker-green.PNG?url"
import img20c from "@/assets/img/Jackets/hoodie/hoodie-zip-silk/hoodie-zip-silk-red.PNG?url"

const STORAGE_KEY = "dirtywave:product-image-overrides:v1"
const SP022_PURPLE_IMAGE = "https://dirtywave.vercel.app/uploads/products/sp022/1.webp"
const SP022_PINK_IMAGE = "https://dirtywave.vercel.app/uploads/products/sp022/2.webp"

const BUILTIN_OVERRIDES = {
  "code:SP022": {
    images: [SP022_PURPLE_IMAGE, SP022_PINK_IMAGE],
    colorImages: [
      { colorId: 6, image: SP022_PURPLE_IMAGE, order: 0 },
      { colorId: 7, image: SP022_PINK_IMAGE, order: 1 },
    ],
  },
  "code:SP023": {
    images: [img14c],
    colorImages: [
      { colorId: 11, image: img14c, order: 0 },
    ],
  },
  "code:SP025": {
    images: [img20c],
    colorImages: [
      { colorId: 7, image: img20c, order: 0 },
      { colorId: 6, image: img20c, order: 1 },
    ],
  },
  "code:SP026": {
    images: [img14c],
    colorImages: [{ colorId: 11, image: img14c, order: 0 }],
  },
}

const LEGACY_SP023_BAD_IMAGE_TOKENS = ["hoodie-zip-boxy", "hoodie-zip-silk"]
const LEGACY_SP024_BAD_IMAGE_TOKENS = ["hoodie-zip-silk", "hoodie", "ao-khoac-hoodie"]

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
  const list = Array.isArray(entries)
    ? entries
    : (entries && typeof entries === 'object'
      ? Object.entries(entries).map(([key, value], index) => {
          if (value && typeof value === 'object') {
            return {
              ...value,
              colorId: value.colorId ?? value.mauSacId ?? Number(key),
              order: value.order ?? value.thuTu ?? index
            }
          }

          return {
            colorId: Number(key),
            image: value,
            order: index
          }
        })
      : [])
  return list
    .map((entry, index) => ({
      colorId: Number(entry?.colorId || entry?.mauSacId || entry?.idMauSac || entry?.id_mau_sac || entry?.mau?.id),
      image: String(entry?.image || entry?.previewUrl || entry?.url || entry?.path || entry?.anh || '').trim(),
      order: Number.isFinite(Number(entry?.order))
        ? Number(entry.order)
        : (Number.isFinite(Number(entry?.thuTu)) ? Number(entry.thuTu) : index)
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

  if (code) {
    keys.push(`code:${code.toUpperCase()}`)
    return keys
  }

  // Only fall back to id when product code is not available.
  if (Number.isFinite(numericId) && numericId > 0) {
    keys.push(`id:${numericId}`)
  }

  return keys
}

function resolveEntryForKey(store, key) {
  // SP022 had stale local overrides in some browsers; force canonical mapping.
  if (key === "code:SP022") {
    return normalizeStoreEntry(BUILTIN_OVERRIDES[key])
  }

  if (key === "code:SP023") {
    const localEntry = normalizeStoreEntry(store[key])
    const imageCandidates = [
      ...(Array.isArray(localEntry.images) ? localEntry.images : []),
      ...(Array.isArray(localEntry.colorImages) ? localEntry.colorImages.map((item) => item.image) : [])
    ]
      .map((value) => String(value || "").toLowerCase())
      .filter(Boolean)

    const hasLegacyBadImage = imageCandidates.some((src) => {
      return LEGACY_SP023_BAD_IMAGE_TOKENS.some((token) => src.includes(token))
    })

    if (hasLegacyBadImage) {
      return normalizeStoreEntry(BUILTIN_OVERRIDES[key])
    }
  }

  if (key === "code:SP024") {
    const localEntry = normalizeStoreEntry(store[key])
    const imageCandidates = [
      ...(Array.isArray(localEntry.images) ? localEntry.images : []),
      ...(Array.isArray(localEntry.colorImages) ? localEntry.colorImages.map((item) => item.image) : [])
    ]
      .map((value) => String(value || "").toLowerCase())
      .filter(Boolean)

    const hasLegacyBadImage = imageCandidates.some((src) => {
      return LEGACY_SP024_BAD_IMAGE_TOKENS.some((token) => src.includes(token))
    })

    if (hasLegacyBadImage) {
      return {
        images: [],
        colorImages: []
      }
    }
  }

  return normalizeStoreEntry(store[key] ?? BUILTIN_OVERRIDES[key])
}

export function getProductImageOverride(productLike) {
  const store = readStore()
  for (const key of buildLookupKeys(productLike)) {
    const entry = resolveEntryForKey(store, key)
    if (entry.images.length) return entry.images
  }
  return []
}

export function getProductImageConfig(productLike) {
  const store = readStore()
  for (const key of buildLookupKeys(productLike)) {
    const entry = resolveEntryForKey(store, key)
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

  const source = productLike && typeof productLike === "object"
    ? productLike
    : { id: productLike }
  const numericId = Number(source?.id)
  const code = String(source?.maSanPham || source?.ma || source?.code || "").trim()

  if (code.toUpperCase() === "SP022") {
    delete store["code:SP022"]
  }

  if (code && Number.isFinite(numericId) && numericId > 0) {
    delete store[`id:${numericId}`]
  }

  writeStore(store)
}

export function clearProductImageOverride(productLike) {
  setProductImageOverride(productLike, [])
}
