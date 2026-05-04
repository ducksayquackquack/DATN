import axios from "axios";
import { resolveApiOrigin } from "../utils/apiOrigin";

const NODE_BACKEND = String(import.meta.env.VITE_NODE_BACKEND_URL || "").trim().replace(/\/$/, "");
const SAME_ORIGIN_API_BASE = `${resolveApiOrigin()}/api`.replace(/\/$/, "");
const SPRING_API_BASE = SAME_ORIGIN_API_BASE;
const SPRING_PRODUCTS_API = `${SPRING_API_BASE}/san-pham`;
const SPRING_CAMPAIGNS_API = `${SPRING_API_BASE}/khuyen-mai`;
const SAME_ORIGIN_PRODUCTS_API = `${SAME_ORIGIN_API_BASE}/san-pham`;
const SAME_ORIGIN_CAMPAIGNS_API = `${SAME_ORIGIN_API_BASE}/khuyen-mai`;
const LOCAL_CAMPAIGN_PRODUCTS_KEY = "dirtywave:campaign-products";

let cacheAt = 0;
let cacheProductMap = new Map(); // productId -> { giaTri, ngayBatDau, ngayKetThuc, tenKhuyenMai, ... }
let pendingPromise = null;

const CACHE_TTL_MS = 30 * 1000;

const unwrapList = (payload) => {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.content)) return payload.content;
  if (Array.isArray(payload?.data)) return payload.data;
  if (Array.isArray(payload?.items)) return payload.items;
  if (Array.isArray(payload?.results)) return payload.results;
  return [];
};

const clampPercent = (value) => {
  const percent = Number(value);
  if (!Number.isFinite(percent)) return 0;
  return Math.max(0, Math.min(100, percent));
};

const normalizeCampaignInfo = (row) => {
  const percent = clampPercent(
    row?.giaTri
    ?? row?.phanTramGiam
    ?? row?.phanTram
    ?? row?.tyLeGiam
    ?? row?.giaTriGiamGia
    ?? 0
  );
  return {
    idKhuyenMai: Number(row?.id ?? row?.idKhuyenMai ?? row?.khuyenMaiId ?? 0) || null,
    giaTri: percent,
    tenKhuyenMai: row?.tenKhuyenMai ?? row?.ten ?? "",
    maKhuyenMai: row?.maKhuyenMai ?? row?.ma ?? "",
    ngayBatDau: row?.ngayBatDau,
    ngayKetThuc: row?.ngayKetThuc,
    trangThaiKM: row?.trangThaiKM ?? row?.trangThai ?? row?.isActive ?? row?.active,
  };
};

const toDateSafe = (value) => {
  if (!value) return null;
  if (Array.isArray(value)) {
    const [y, m, d] = value;
    return new Date(Number(y), Number(m || 1) - 1, Number(d || 1));
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
};

const normalizeStatusText = (value) => {
  return String(value ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase()
    .trim();
};

const isTruthyStatus = (value) => {
  if (value === true || value === 1) return true;
  const text = normalizeStatusText(value);
  if (!text) return false;
  return ["1", "true", "active", "hoat dong", "dang dien ra", "hieu luc"].includes(text);
};

const isCampaignActiveNow = (row, now = new Date()) => {
  const start = toDateSafe(row?.ngayBatDau);
  const end = toDateSafe(row?.ngayKetThuc);
  if (!start || !end) return false;
  const s = new Date(start); s.setHours(0, 0, 0, 0);
  const e = new Date(end); e.setHours(23, 59, 59, 999);
  const statusRaw = row?.trangThaiKM ?? row?.trangThai ?? row?.isActive ?? row?.active ?? "";
  const statusText = normalizeStatusText(statusRaw);
  const statusOk = isTruthyStatus(statusRaw)
    || statusText.includes("hoat dong")
    || statusText.includes("dang dien ra")
    || statusText.includes("active");
  return statusOk && now >= s && now <= e;
};

const readLocalCampaignProductsStore = () => {
  if (typeof window === "undefined") return {};
  try {
    const raw = localStorage.getItem(LOCAL_CAMPAIGN_PRODUCTS_KEY);
    const parsed = raw ? JSON.parse(raw) : {};
    return parsed && typeof parsed === "object" ? parsed : {};
  } catch {
    return {};
  }
};

const normalizeIdArray = (values) => {
  return [...new Set((Array.isArray(values) ? values : [])
    .map((id) => Number(id))
    .filter((id) => Number.isFinite(id) && id > 0))]
}

const extractProductIdsFromEntry = (entry) => {
  if (Array.isArray(entry)) return normalizeIdArray(entry)
  if (!entry || typeof entry !== "object") return []
  return normalizeIdArray(entry?.productIds ?? entry?.products ?? entry?.idSanPhams)
}

const extractVariantIdsFromEntry = (entry) => {
  if (!entry || Array.isArray(entry) || typeof entry !== "object") return []
  return normalizeIdArray(entry?.variantIds ?? entry?.variants ?? entry?.idChiTietSanPhams)
}

const getLocalCampaignSelection = (campaignId) => {
  const kmId = Number(campaignId || 0)
  if (!Number.isFinite(kmId) || kmId <= 0) return { productIds: [], variantIds: [] }

  const store = readLocalCampaignProductsStore()
  const entry = store[String(kmId)]
  return {
    productIds: extractProductIdsFromEntry(entry),
    variantIds: extractVariantIdsFromEntry(entry)
  }
}

const getLocalCampaignProductsMap = () => {
  const store = readLocalCampaignProductsStore();
  const map = new Map();
  for (const [campaignIdRaw, productIdsRaw] of Object.entries(store || {})) {
    const campaignId = Number(campaignIdRaw);
    if (!Number.isFinite(campaignId) || campaignId <= 0) continue;
    const productIds = extractProductIdsFromEntry(productIdsRaw)

    if (productIds.length > 0) map.set(campaignId, productIds);
  }
  return map;
};

async function buildProductDiscountMap(force = false) {
  const now = Date.now();
  if (!force && cacheProductMap.size && now - cacheAt < CACHE_TTL_MS) {
    return cacheProductMap;
  }
  if (!force && pendingPromise) return pendingPromise;

  pendingPromise = (async () => {
    const nextMap = new Map();
    const nowDate = new Date();

    const nodeRes = NODE_BACKEND
      ? await axios.get(`${NODE_BACKEND}/api/khuyen-mai-products`).catch(() => null)
      : null;
    const nodeRows = Array.isArray(nodeRes?.data) ? nodeRes.data : [];
    for (const row of nodeRows) {
      if (!isCampaignActiveNow(row, nowDate)) continue;
      const productId = Number(row?.id);
      if (!Number.isFinite(productId) || productId <= 0) continue;
      const info = normalizeCampaignInfo(row);
      if (info.giaTri <= 0) continue;
      nextMap.set(productId, info);
    }

    if (!nextMap.size) {
      let campaignRes = await axios.get(SPRING_CAMPAIGNS_API).catch(() => null);
      let productsRes = await axios.get(SPRING_PRODUCTS_API, { params: { page: 0, size: 2000 } }).catch(() => null);
      if (!campaignRes || !productsRes) {
        campaignRes = campaignRes || await axios.get(SAME_ORIGIN_CAMPAIGNS_API).catch(() => null);
        productsRes = productsRes || await axios.get(SAME_ORIGIN_PRODUCTS_API, { params: { page: 0, size: 2000 } }).catch(() => null);
      }

      const campaigns = unwrapList(campaignRes?.data);
      const products = unwrapList(productsRes?.data);
      const activeCampaignMap = new Map();

      for (const row of campaigns) {
        const id = Number(row?.id ?? row?.idKhuyenMai ?? row?.khuyenMaiId ?? 0);
        if (!Number.isFinite(id) || id <= 0) continue;
        if (!isCampaignActiveNow(row, nowDate)) continue;
        const info = normalizeCampaignInfo(row);
        if (info.giaTri <= 0) continue;
        activeCampaignMap.set(id, info);
      }

      for (const product of products) {
        const productId = Number(product?.id ?? product?.idSanPham ?? product?.sanPhamId ?? 0);
        if (!Number.isFinite(productId) || productId <= 0) continue;

        const productCampaignId = Number(product?.idKhuyenMai ?? product?.id_khuyen_mai ?? 0);
        if (productCampaignId > 0 && activeCampaignMap.has(productCampaignId)) {
          nextMap.set(productId, activeCampaignMap.get(productCampaignId));
          continue;
        }

        const variants = Array.isArray(product?.sanPhamChiTiets) ? product.sanPhamChiTiets : [];
        const variantCampaignId = variants
          .map((v) => Number(v?.idKhuyenMai ?? v?.id_khuyen_mai ?? 0))
          .find((id) => Number.isFinite(id) && id > 0);
        if (variantCampaignId > 0 && activeCampaignMap.has(variantCampaignId)) {
          nextMap.set(productId, activeCampaignMap.get(variantCampaignId));
        }
      }

      // Final fallback: local client-side campaign-product mappings.
      if (typeof window !== "undefined") {
        const localMap = getLocalCampaignProductsMap();
        for (const [campaignId, productIds] of localMap.entries()) {
          const info = activeCampaignMap.get(Number(campaignId));
          if (!info) continue;
          for (const productId of productIds) {
            if (!nextMap.has(productId)) {
              nextMap.set(productId, info);
            }
          }
        }
      }
    }

    cacheProductMap = nextMap;
    cacheAt = Date.now();
    pendingPromise = null;
    return nextMap;
  })().catch(() => { pendingPromise = null; return cacheProductMap; });

  return pendingPromise;
}

export async function getProductCampaignInfo(productId) {
  const id = Number(productId || 0);
  if (!Number.isFinite(id) || id <= 0) return null;
  const map = await buildProductDiscountMap(false);
  return map.get(id) || null;
}

export async function getCampaignDiscountPercentByVariant(variantId, productId) {
  const pId = Number(productId || 0);
  if (!Number.isFinite(pId) || pId <= 0) return 0;
  const map = await buildProductDiscountMap(false);
  const info = map.get(pId);
  return info ? Math.max(0, Math.min(100, info.giaTri)) : 0;
}

export async function applyCampaignPriceToVariants(variants = [], productId = 0) {
  const rows = Array.isArray(variants) ? variants : [];
  if (!rows.length) return [];
  const map = await buildProductDiscountMap(false);

  return rows.map((variant) => {
    const pId = Number(productId || variant?.idSanPham || variant?.sanPham?.id || 0);
    const info = pId > 0 ? map.get(pId) : null;
    const mappedPercent = info ? clampPercent(info.giaTri) : 0;
    const campaignId = Number(info?.idKhuyenMai || 0)
    const variantId = Number(variant?.id || variant?.idChiTietSanPham || variant?.spctId || 0)
    const localSelection = campaignId > 0 ? getLocalCampaignSelection(campaignId) : { variantIds: [] }
    const hasVariantConstraint = Array.isArray(localSelection?.variantIds) && localSelection.variantIds.length > 0
    const isVariantSelected = hasVariantConstraint
      ? localSelection.variantIds.includes(variantId)
      : true

    const rawPrice = Number(variant?.giaBan || variant?.price || 0);
    const listedBase = Number(variant?.giaBanGoc || variant?.giaNiemYet || 0);
    const basePrice = listedBase > 0 ? Math.max(listedBase, rawPrice) : rawPrice;

    const variantPercent = clampPercent(
      variant?.dotGiamGiaPhanTram
      ?? variant?.phanTramGiamGia
      ?? variant?.giamGiaPhanTram
      ?? 0
    );
    const variantFinal = Number(variant?.giaBanSauDotGiamGia ?? variant?.giaSauGiam ?? 0);

    const percent = hasVariantConstraint
      ? (isVariantSelected ? mappedPercent : 0)
      : (mappedPercent > 0 ? mappedPercent : variantPercent);
    let finalPrice = rawPrice;
    if (hasVariantConstraint && !isVariantSelected && basePrice > 0) {
      finalPrice = basePrice;
    } else if (percent > 0 && basePrice > 0) {
      finalPrice = Math.max(0, Math.round(basePrice * (1 - percent / 100)));
    } else if (variantFinal > 0 && basePrice > 0 && variantFinal <= basePrice) {
      finalPrice = variantFinal;
    }

    const derivedPercent = percent > 0
      ? percent
      : (basePrice > 0 && finalPrice < basePrice
        ? clampPercent(Math.round((1 - finalPrice / basePrice) * 100))
        : 0);

    return {
      ...variant,
      giaBanGoc: basePrice,
      giaBanSauDotGiamGia: finalPrice,
      giaBan: finalPrice,
      giaSauGiam: finalPrice,
      dotGiamGiaPhanTram: derivedPercent,
      campaignInfo: info,
    };
  });
}

export async function refreshCampaignPricingCache() {
  await buildProductDiscountMap(true);
}

export async function getActiveCampaignMap() {
  return buildProductDiscountMap(false);
}
