import img14c from "@/assets/img/Jackets/bomber/bomber-windbreaker/bomer-windbreaker-green.PNG?url"
import img19 from "@/assets/img/Jackets/hoodie/hoodie-zip-boxy/hoodie-zip-boxy-blue.PNG?url"
import img19b from "@/assets/img/Jackets/hoodie/hoodie-zip-boxy/hoodie-zip-boxy-white.PNG?url"
import img20 from "@/assets/img/Jackets/hoodie/hoodie-zip-silk/hoodie-zip-silk-black.PNG?url"
import img20b from "@/assets/img/Jackets/hoodie/hoodie-zip-silk/hoodie-zip-silk-gray.PNG?url"
import img20c from "@/assets/img/Jackets/hoodie/hoodie-zip-silk/hoodie-zip-silk-red.PNG?url"

const STORAGE_KEY = "dirtywave:product-image-overrides:v1"
const SP022_PURPLE_IMAGE = "/uploads/products/sp022/1.webp"
const SP022_PINK_IMAGE = "/uploads/products/sp022/2.webp"

const BUILTIN_OVERRIDES = {
  "code:SP022": {
    images: [SP022_PURPLE_IMAGE, SP022_PINK_IMAGE],
    colorImages: [
      { colorId: 6, image: SP022_PURPLE_IMAGE, order: 0 },
      { colorId: 7, image: SP022_PINK_IMAGE, order: 1 },
    ],
  },
  "id:23": {
    images: [img19],
    colorImages: [
      { colorId: 2, image: img19, order: 0 },
      { colorId: 3, image: img19b, order: 1 },
      { colorId: 1, image: img20, order: 2 },
    ],
  },
  "code:SP023": {
    images: [img19],
    colorImages: [
      { colorId: 2, image: img19, order: 0 },
      { colorId: 3, image: img19b, order: 1 },
      { colorId: 1, image: img20, order: 2 },
    ],
  },
  "id:24": {
    images: [img20b],
    colorImages: [{ colorId: 8, image: img20b, order: 0 }],
  },
  "code:SP024": {
    images: [img20b],
    colorImages: [{ colorId: 8, image: img20b, order: 0 }],
  },
  "id:25": {
    images: [img20c],
    colorImages: [
      { colorId: 7, image: img20c, order: 0 },
      { colorId: 6, image: img20c, order: 1 },
    ],
  },
  "code:SP025": {
    images: [img20c],
    colorImages: [
      { colorId: 7, image: img20c, order: 0 },
      { colorId: 6, image: img20c, order: 1 },
    ],
  },
  "id:26": {
    images: [img14c],
    colorImages: [{ colorId: 11, image: img14c, order: 0 }],
  },
  "code:SP026": {
    images: [img14c],
    colorImages: [{ colorId: 11, image: img14c, order: 0 }],
  },
}

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
  }

  // Prefer stable product code over DB id to avoid collisions when ids differ across environments.
  if (Number.isFinite(numericId) && numericId > 0) {
    keys.push(`id:${numericId}`)
  }

  return keys
}

export function getProductImageOverride(productLike) {
  const store = readStore()
  for (const key of buildLookupKeys(productLike)) {
    const entry = normalizeStoreEntry(store[key] ?? BUILTIN_OVERRIDES[key])
    if (entry.images.length) return entry.images
  }
  return []
}

export function getProductImageConfig(productLike) {
  const store = readStore()
  for (const key of buildLookupKeys(productLike)) {
    const entry = normalizeStoreEntry(store[key] ?? BUILTIN_OVERRIDES[key])
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
