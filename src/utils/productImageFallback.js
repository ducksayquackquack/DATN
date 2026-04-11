import img1 from "@/assets/img/Jackets/bomber/bomber-da-lon.jpg?url";
import img2 from "@/assets/img/Jackets/bomber/bomber-dang-lung.jpg?url";
import img3 from "@/assets/img/Jackets/bomber/bomber-gia-da.jpg?url";
import img4 from "@/assets/img/Jackets/bomber/bomber-nhe-cotton.jpg?url";
import img5 from "@/assets/img/Jackets/hoodie/hoodie-dang-hop.jpg?url";
import img6 from "@/assets/img/Jackets/hoodie/hoodie-in-hinh.jpg?url";
import img7 from "@/assets/img/Jackets/hoodie/hoodie-keo-khoa.jpg?url";
import img8 from "@/assets/img/Jackets/coach/coach-cach-nhiet.jpg?url";
import img9 from "@/assets/img/Jackets/coach/coach-da-asos.jpg?url";
import img10 from "@/assets/img/Jackets/coach/coach-gia-da.jpg?url";
import img11 from "@/assets/img/Jackets/coach/coach-long-cuu.jpg?url";
import img12 from "@/assets/img/Jackets/bomber/bomber-astronaut/bomber-astronaut-black.PNG?url";
import img12b from "@/assets/img/Jackets/bomber/bomber-astronaut/bomber-astronaut-blue.PNG?url";
import img13 from "@/assets/img/Jackets/bomber/bomber-embroidered-fuzzy/bomer-embroidered-black.PNG?url";
import img13b from "@/assets/img/Jackets/bomber/bomber-embroidered-fuzzy/bomer-embroidered-green.PNG?url";
import img13c from "@/assets/img/Jackets/bomber/bomber-embroidered-fuzzy/bomer-embroidered-brown.PNG?url";
import img13d from "@/assets/img/Jackets/bomber/bomber-embroidered-fuzzy/bomer-embroidered-red.PNG?url";
import img13e from "@/assets/img/Jackets/bomber/bomber-embroidered-fuzzy/bomer-embroidered-white.PNG?url";
import img14 from "@/assets/img/Jackets/bomber/bomber-windbreaker/bomer-windbreaker-black.PNG?url";
import img14b from "@/assets/img/Jackets/bomber/bomber-windbreaker/bomer-windbreaker-blue.PNG?url";
import img14c from "@/assets/img/Jackets/bomber/bomber-windbreaker/bomer-windbreaker-green.PNG?url";
import img15 from "@/assets/img/Jackets/coach/coach-leopard/coach-leopard.PNG?url";
import img16 from "@/assets/img/Jackets/coach/coach-longsleeve/coach-longsleeve-black.PNG?url";
import img16b from "@/assets/img/Jackets/coach/coach-longsleeve/coach-longsleeve-red.PNG?url";
import img16c from "@/assets/img/Jackets/coach/coach-longsleeve/coach-longsleeve-white.PNG?url";
import img17 from "@/assets/img/Jackets/coach/coach-tiger-stripe/coach-tiger-stripe.PNG?url";
import img18 from "@/assets/img/Jackets/hoodie/hoodie-camo/hoodie-camo-black.PNG?url";
import img18b from "@/assets/img/Jackets/hoodie/hoodie-camo/hoodie-camo-white.PNG?url";
import img19 from "@/assets/img/Jackets/hoodie/hoodie-zip-boxy/hoodie-zip-boxy-blue.PNG?url";
import img19b from "@/assets/img/Jackets/hoodie/hoodie-zip-boxy/hoodie-zip-boxy-white.PNG?url";
import img20 from "@/assets/img/Jackets/hoodie/hoodie-zip-silk/hoodie-zip-silk-black.PNG?url";
import img20b from "@/assets/img/Jackets/hoodie/hoodie-zip-silk/hoodie-zip-silk-gray.PNG?url";
import img20c from "@/assets/img/Jackets/hoodie/hoodie-zip-silk/hoodie-zip-silk-red.PNG?url";

const fallbackImages = [
  img1, img2, img3, img4, img5,
  img6, img7, img8, img9, img10,
  img11, img12, img13, img14, img15,
  img16, img17, img18, img19, img20,
];

const byCode = {
  SP001: img1,
  SP002: img2,
  SP003: img3,
  SP004: img4,
  SP005: img5,
  SP006: img6,
  SP007: img7,
  SP008: img8,
  SP009: img9,
  SP010: img10,
  SP011: img11,
  SP012: img12,
  SP013: img13,
  SP014: img14,
  SP015: img15,
  SP016: img16,
  SP017: img17,
  SP018: img18,
  SP019: img19,
  SP020: img20,
};

const normalizeColor = (value = "") =>
  String(value || "")
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .toLowerCase()
    .replace(/\s+/g, " ")
    .trim();

const variantColorByCode = {
  SP002: {
    do: img2,
    red: img2,
  },
  SP003: {
    "xanh duong": img3,
    xanh: img3,
    blue: img3,
  },
  SP009: {
    kem: img9,
    cream: img9,
  },
  SP010: {
    nau: img10,
    brown: img10,
  },
  SP012: {
    den: img12,
    "xanh duong": img12b,
    xanh: img12b,
    blue: img12b,
    black: img12,
  },
  SP013: {
    den: img13,
    black: img13,
    "xanh la": img13b,
    xanh: img13b,
    green: img13b,
    nau: img13c,
    brown: img13c,
    do: img13d,
    red: img13d,
    trang: img13e,
    white: img13e,
  },
  SP014: {
    den: img14,
    black: img14,
    "xanh duong": img14b,
    xanh: img14b,
    blue: img14b,
    "xanh la": img14c,
    green: img14c,
  },
  SP016: {
    den: img16,
    black: img16,
    do: img16b,
    red: img16b,
    trang: img16c,
    white: img16c,
  },
  SP018: {
    den: img18,
    black: img18,
    trang: img18b,
    white: img18b,
  },
  SP019: {
    "xanh duong": img19,
    xanh: img19,
    blue: img19,
    trang: img19b,
    white: img19b,
  },
  SP020: {
    den: img20,
    black: img20,
    xam: img20b,
    ghi: img20b,
    gray: img20b,
    grey: img20b,
    do: img20c,
    red: img20c,
  },
};

const variantOrderFallback = {
  // SP002 intentionally excluded: the "B" suffix in SPCT002B means Size L, not Color 2.
  // Color for SP002 must always be resolved by tenMauSac, not by variant letter suffix.
  SP003: [img3],
  SP009: [img9],
  SP010: [img10],
  SP013: [img13, img13b, img13c, img13d, img13e],
  SP014: [img14, img14b, img14c],
  SP016: [img16, img16b, img16c],
  SP018: [img18, img18b],
  SP019: [img19, img19b],
  SP020: [img20, img20b, img20c],
};

const normalize = (value = "") =>
  String(value || "")
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .toLowerCase();

const fallbackByName = [
  { keywords: ["bomber", "da", "lon"], image: img1 },
  { keywords: ["bomber", "dang", "lung"], image: img2 },
  { keywords: ["bomber", "gia", "da"], image: img3 },
  { keywords: ["bomber", "cotton"], image: img4 },
  { keywords: ["hoodie", "dang", "hop"], image: img5 },
  { keywords: ["hoodie", "in", "hinh"], image: img6 },
  { keywords: ["hoodie", "keo", "khoa"], image: img7 },
  { keywords: ["coach", "cach", "nhiet"], image: img8 },
  { keywords: ["coach", "da"], image: img9 },
  { keywords: ["astronaut"], image: img12 },
  { keywords: ["embroidered"], image: img13 },
  { keywords: ["windbreaker"], image: img14 },
  { keywords: ["leopard"], image: img15 },
  { keywords: ["longsleeve"], image: img16 },
  { keywords: ["tiger", "stripe"], image: img17 },
  { keywords: ["camo"], image: img18 },
  { keywords: ["zip", "boxy"], image: img19 },
  { keywords: ["zip", "silk"], image: img20 },
];

export const fallbackImageForProduct = ({ id, maSanPham, tenSanPham } = {}) => {
  const code = String(maSanPham || "").trim().toUpperCase();
  if (code && byCode[code]) return byCode[code];

  const nameNorm = normalize(tenSanPham);
  if (nameNorm) {
    const found = fallbackByName.find((item) => item.keywords.every((k) => nameNorm.includes(k)));
    if (found?.image) return found.image;
  }

  const n = Number(id);
  if (Number.isFinite(n) && n > 0) return fallbackImages[(n - 1) % fallbackImages.length];

  const digits = Number(String(code).replace(/\D+/g, ""));
  if (Number.isFinite(digits) && digits > 0) return fallbackImages[(digits - 1) % fallbackImages.length];

  return fallbackImages[0] || "";
};

export const fallbackImageForVariant = ({
  id,
  maSanPham,
  tenSanPham,
  tenMauSac,
  maChiTietSanPham,
} = {}) => {
  const code = String(maSanPham || "").trim().toUpperCase();
  const color = normalizeColor(tenMauSac);

  if (code && color && variantColorByCode[code]) {
    const byExact = variantColorByCode[code][color];
    if (byExact) return byExact;

    // Be tolerant with color naming differences from backend labels.
    const colorEntries = Object.entries(variantColorByCode[code]);
    const byLoose = colorEntries.find(([alias]) => color.includes(alias) || alias.includes(color));
    if (byLoose?.[1]) return byLoose[1];
  }

  const varCode = String(maChiTietSanPham || "").trim().toUpperCase();
  if (code && varCode && variantOrderFallback[code]?.length) {
    const suffix = varCode.replace(/.*?(\d+)([A-Z]?)$/, "$2");
    // A -> 0, B -> 1, C -> 2 ... to match variant letter ordering.
    const index = suffix ? Math.max(0, suffix.charCodeAt(0) - 65) : 0;
    const list = variantOrderFallback[code];
    if (list[index]) return list[index];
    if (list[0]) return list[0];
  }

  return fallbackImageForProduct({ id, maSanPham, tenSanPham });
};
