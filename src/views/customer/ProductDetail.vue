<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue"
import { useRoute, useRouter } from "vue-router"
import {
  ChevronLeft,
  ChevronRight,
  Copy,
  Eye,
  Menu,
  Search,
  ShieldCheck,
  ShoppingCart,
  Ticket,
  Truck,
  Undo2,
  Wallet,
  X,
  Ruler
} from "lucide-vue-next"
import { getAllSanPham } from "../../services/sanPhamService"
import taiKhoanService from "../../services/taiKhoanService"
import { getKhachHangByTaiKhoanId } from "../../services/KhachHangService"
import { calculateVoucherDiscount, getActiveVouchers, getAllVouchers, isVoucherApplicable } from "../../services/khuyenMaiService"
import { getVietnameseNameByEmail } from "../../utils/vietnameseNames"
import { useToast } from "../../composables/useToast"
import {
  AUTH_CONTEXT_CHANGED_EVENT,
  resolveAccountByRole
} from "../../utils/authContext"
import {
  readCartObject,
  writeCartObject,
  writeCheckoutCartArray
} from "../../utils/cartStorage"
import { resolveApiOrigin } from "../../utils/apiOrigin"
import { getProductImageOverride } from "../../utils/productImageOverrides"
import SiteNav from "../../components/SiteNav.vue"
import logo from "../../assets/img/logo/new logo.png?url"
import img1 from "../../assets/img/Jackets/Áo bomber da lộn DirtyWave.jpg?url"
import img2 from "../../assets/img/Jackets/Áo bomber dáng lửng.jpg?url"
import img3 from "../../assets/img/Jackets/Áo bomber giả da DirtyWave.jpg?url"
import img4 from "../../assets/img/Jackets/Áo bomber nhẹ vải cotton DirtyWave.jpg?url"
import img5 from "../../assets/img/Jackets/Áo hoodie kéo khoá dáng hộp DirtyWave.jpg?url"
import img6 from "../../assets/img/Jackets/Áo hoodie kéo khoá in hình DirtyWave.jpg?url"
import img7 from "../../assets/img/Jackets/Áo hoodie kéo khoá Jacket DirtyWave.jpg?url"
import img8 from "../../assets/img/Jackets/Áo khoác coach cách nhiệt vải Timberland.jpg?url"
import img9 from "../../assets/img/Jackets/Áo khoac coach da ASOS DirtyWave.jpg?url"
import img10 from "../../assets/img/Jackets/Áo khoác coach giả da DirtyWave.jpg?url"
import img11 from "../../assets/img/Jackets/Áo khoác coach lông cừu DirtyWave.jpg?url"

const route = useRoute()
const router = useRouter()
const toast = useToast()
const BACKEND_ORIGIN = resolveApiOrigin().replace(/\/$/, "")
const fallbackImages = [img1, img2, img3, img4, img5, img6, img7, img8, img9, img10, img11]

const PRODUCTS = [
  {
    id: 1,
    name: "Áo Bomber Da Lộn DirtyWave",
    category: "Bomber",
    price: 649000,
    originalPrice: 799000,
    sku: "ATID070-01",
    badge: "Còn hàng",
    badgeTone: "red",
    images: [img1, img3, img4, img2],
    colors: [{ name: "Đen than", hex: "#1f1b1b" }, { name: "Nâu cacao", hex: "#6b412c" }, { name: "Rêu", hex: "#4a5845" }],
    sizes: ["S", "M", "L", "XL"],
    material: "Da lộn xử lý mềm bề mặt",
    fit: "Bomber regular fit",
    bullets: [
      "Chất liệu mềm tay, bề mặt lì và giữ phom tốt.",
      "Bo cổ, bo tay và bo gấu dệt chắc để outfit gọn hơn.",
      "Phối đẹp với denim tối màu, quần straight hoặc cargo."
    ],
    description: {
      intro: "DirtyWave tập trung vào phom gọn, dứt khoát và cảm giác mặc thật. Thiết kế này ưu tiên độ sắc của outfit nhưng vẫn đủ dễ mặc hằng ngày.",
      material: "Bề mặt da lộn mềm, lì, cho cảm giác chạm tay tốt và lên ảnh có chiều sâu. Kết cấu vừa đủ dày để giữ form ổn định khi mặc riêng hoặc layer.",
      design: "Khoá kéo kim loại tông tối, vai xuôi vừa phải và tỉ lệ thân áo cân bằng giúp tổng thể trông cao, gọn và rõ nét hơn.",
      fit: "Regular fit theo tinh thần streetwear tinh gọn: vừa đủ rộng để thoải mái nhưng không làm outfit bị nặng nề."
    }
  },
  {
    id: 2,
    name: "Áo Bomber Dáng Lửng",
    category: "Bomber",
    price: 399000,
    originalPrice: 0,
    sku: "ATID070-02",
    badge: "Còn hàng",
    badgeTone: "red",
    images: [img2, img1, img4, img3],
    colors: [{ name: "Kem khói", hex: "#d4cbbf" }, { name: "Đen", hex: "#1b1b1b" }],
    sizes: ["S", "M", "L"],
    material: "Cotton pha đứng phom",
    fit: "Cropped bomber fit",
    bullets: [
      "Phom lửng giúp tổng thể gọn và cao hơn.",
      "Bề mặt vải ít nhăn, dễ phối với quần cạp cao.",
      "Phù hợp cả outfit đi làm sáng tạo lẫn đi chơi."
    ],
    description: {
      intro: "Thiết kế bomber lửng dành cho người muốn outfit sắc, gọn và hiện đại hơn so với các phom truyền thống.",
      material: "Chất vải dày vừa, mặt vải sạch và ít nhăn, giữ được cảm giác chỉn chu trong quá trình sử dụng.",
      design: "Chiều dài thân áo được cắt ngắn hợp lý để giữ tỉ lệ outfit cân đối. Vai xuôi nhẹ giúp phần thân trên mềm hơn khi phối đồ.",
      fit: "Phom cropped nhưng không quá ôm, vẫn giữ đủ độ thoải mái để mặc nhiều giờ liên tục."
    }
  },
  {
    id: 3,
    name: "Áo Bomber Giả Da DirtyWave",
    category: "Bomber",
    price: 329000,
    originalPrice: 389000,
    sku: "ATID070-03",
    badge: "Bán chạy",
    badgeTone: "dark",
    images: [img3, img1, img10, img4],
    colors: [{ name: "Đen bóng mờ", hex: "#171717" }, { name: "Nâu espresso", hex: "#4d3329" }],
    sizes: ["M", "L", "XL"],
    material: "Giả da mềm dễ chăm sóc",
    fit: "Regular fit",
    bullets: ["Nhẹ, dễ mặc hơn các mẫu da cứng.", "Giữ thần thái mạnh nhưng không quá nặng.", "Hợp với sneaker, boots và quần suông."],
    description: {
      intro: "Một lựa chọn dễ tiếp cận hơn cho outfit da, tập trung vào khả năng mặc thường xuyên và độ linh hoạt.",
      material: "Chất giả da mềm, bề mặt mịn và ít bắt bụi, giúp việc chăm sóc đơn giản hơn.",
      design: "Cấu trúc cổ, thân và tay giữ tinh thần bomber rõ ràng nhưng được xử lý gọn để không bị thô.",
      fit: "Regular fit vừa đủ thoải mái, phù hợp nhiều dáng người và nhiều kiểu phối đồ."
    }
  },
  {
    id: 4,
    name: "Áo Bomber Cotton DirtyWave",
    category: "Bomber",
    price: 459000,
    originalPrice: 0,
    sku: "ATID070-04",
    badge: "Mới",
    badgeTone: "red",
    images: [img4, img1, img2, img7],
    colors: [{ name: "Xám khói", hex: "#757575" }, { name: "Đen", hex: "#101010" }],
    sizes: ["S", "M", "L", "XL"],
    material: "Cotton đanh bề mặt",
    fit: "Relaxed fit",
    bullets: ["Nhẹ, thoáng và dễ mặc hằng ngày.", "Tinh thần tối giản, ít chi tiết thừa.", "Phù hợp thời tiết chuyển mùa."],
    description: {
      intro: "Bomber cotton cho người cần một lớp áo ngoài đơn giản nhưng vẫn gọn và có điểm nhấn về chất liệu.",
      material: "Cotton có độ đứng vừa phải, bề mặt sạch và ít xù sau quá trình sử dụng thông thường.",
      design: "Thiết kế tối giản, kiểm soát tốt tỉ lệ ở phần thân và tay áo để luôn giữ cảm giác gọn.",
      fit: "Relaxed fit dễ phối với áo thun, sweater mỏng hoặc hoodie nhẹ."
    }
  },
  {
    id: 5,
    name: "Hoodie Zip Dáng Hộp DirtyWave",
    category: "Hoodie",
    price: 499000,
    originalPrice: 599000,
    sku: "ATID070-05",
    badge: "Còn hàng",
    badgeTone: "red",
    images: [img5, img6, img7, img4],
    colors: [{ name: "Đen mực", hex: "#0c0c0c" }, { name: "Ghi xám", hex: "#919191" }, { name: "Kem tro", hex: "#d7cec1" }],
    sizes: ["M", "L", "XL"],
    material: "Nỉ dày boxy",
    fit: "Boxy fit",
    bullets: ["Dáng hộp rõ, hợp layering.", "Khoá kéo lớn tạo điểm nhấn mạnh.", "Mũ hai lớp giữ dáng tốt."],
    description: {
      intro: "Một mẫu hoodie zip đóng vai trò layer chính trong outfit, hướng đến cảm giác dày, nặng tay và giàu khối.",
      material: "Vải nỉ dày, mặt ngoài lì và phần trong êm, giữ được form đẹp khi kéo khoá hoặc mở hờ.",
      design: "Đầu kéo lớn, vai hạ và tỉ lệ boxy tạo nên tinh thần streetwear rõ nét.",
      fit: "Boxy fit dành cho set đồ cần nhiều lớp hoặc silhouette rõ ràng."
    }
  },
  {
    id: 6,
    name: "Hoodie Zip In Hình DirtyWave",
    category: "Hoodie",
    price: 699000,
    originalPrice: 0,
    sku: "ATID070-06",
    badge: "Mới",
    badgeTone: "red",
    images: [img6, img5, img7, img4],
    colors: [{ name: "Đen graphic", hex: "#131313" }, { name: "Tro sáng", hex: "#b6b6b2" }],
    sizes: ["M", "L", "XL"],
    material: "Nỉ compact in kỹ thuật số",
    fit: "Relaxed zip fit",
    bullets: ["Mặt in sắc, độ bền cao.", "Thân áo dày vừa, hợp mặc độc lập.", "Dễ phối với denim tối và quần suông."],
    description: {
      intro: "Mẫu hoodie tập trung vào đồ hoạ mặt trước nhưng vẫn giữ tinh thần mặc dễ của DirtyWave.",
      material: "Vải nỉ compact cho cảm giác chắc tay, bề mặt mịn và lên form ổn định.",
      design: "Đồ hoạ được xử lý gọn, không làm tổng thể quá nặng nhưng vẫn đủ tạo điểm nhìn.",
      fit: "Relaxed fit thoải mái, phù hợp layering nhẹ và mặc hằng ngày."
    }
  },
  {
    id: 7,
    name: "Hoodie Zip Jacket DirtyWave",
    category: "Hoodie",
    price: 559000,
    originalPrice: 0,
    sku: "ATID070-07",
    badge: "Bán chạy",
    badgeTone: "dark",
    images: [img7, img5, img6, img4],
    colors: [{ name: "Than đậm", hex: "#171b22" }, { name: "Xám băng", hex: "#9ba0a6" }],
    sizes: ["M", "L", "XL"],
    material: "Nỉ dệt dày giữ phom",
    fit: "Regular zip fit",
    bullets: ["Khoá kéo chắc tay, vận hành mượt.", "Dáng cân bằng giữa boxy và regular.", "Phù hợp set đồ streetwear gọn."],
    description: {
      intro: "Thiết kế zip hoodie thiên về sự cân bằng, dễ mặc và hợp nhiều hoàn cảnh sử dụng hơn.",
      material: "Chất nỉ dày vừa, phần mặt vải sạch và giữ được sự ổn định sau nhiều lần mặc.",
      design: "Thân áo, mũ và tay áo được cân tỉ lệ để tổng thể luôn gọn thay vì bị nặng phần trên.",
      fit: "Regular fit hơi rộng nhẹ, đủ thoải mái nhưng không làm outfit bị phồng."
    }
  },
  {
    id: 8,
    name: "Áo Khoác Coach Cách Nhiệt Timberland",
    category: "Coach",
    price: 799000,
    originalPrice: 899000,
    sku: "ATID070-08",
    badge: "Drop mới",
    badgeTone: "red",
    images: [img8, img9, img10, img11],
    colors: [{ name: "Nâu đất", hex: "#5b4838" }, { name: "Đen đá", hex: "#181818" }],
    sizes: ["M", "L", "XL"],
    material: "Vải chống gió có lớp lót nhẹ",
    fit: "Coach fit",
    bullets: ["Giữ nhiệt nhẹ cho thời tiết chuyển mùa.", "Cổ áo sắc, phần thân gọn.", "Dễ phối cùng tee và denim."],
    description: {
      intro: "Một mẫu coach jacket phục vụ các set đồ cần độ sắc và tính ứng dụng cao.",
      material: "Lớp ngoài chống gió nhẹ, trong có lót mỏng giúp mặc ổn hơn vào buổi tối hoặc phòng lạnh.",
      design: "Cấu trúc coach cổ điển được tinh giản, giữ cảm giác sạch và hiện đại hơn.",
      fit: "Coach fit với độ rộng vừa phải để mặc thoải mái cùng nhiều lớp cơ bản."
    }
  },
  {
    id: 9,
    name: "Áo Khoác Coach Da ASOS DirtyWave",
    category: "Coach",
    price: 899000,
    originalPrice: 0,
    sku: "ATID070-09",
    badge: "Premium",
    badgeTone: "dark",
    images: [img9, img10, img8, img11],
    colors: [{ name: "Nâu rượu", hex: "#5a3527" }, { name: "Đen trầm", hex: "#111111" }],
    sizes: ["L", "XL"],
    material: "Bề mặt da xử lý lì",
    fit: "Structured coach fit",
    bullets: ["Bề mặt da lì, lên ảnh tốt.", "Giữ được thần thái coach jacket nhưng sang hơn.", "Hợp boots, loafer và denim đậm."],
    description: {
      intro: "Thiết kế coach da dành cho outfit cần độ chín và độ sâu chất liệu rõ rệt.",
      material: "Bề mặt da xử lý lì, hạn chế bóng và cho cảm giác cao cấp hơn khi nhìn gần.",
      design: "Thân áo sạch chi tiết, tập trung vào phom vai và độ rơi của bề mặt da.",
      fit: "Structured fit đứng phom rõ, phù hợp người thích silhouette sắc nét."
    }
  },
  {
    id: 10,
    name: "Áo Khoác Coach Giả Da DirtyWave",
    category: "Coach",
    price: 699000,
    originalPrice: 0,
    sku: "ATID070-10",
    badge: "Xu hướng",
    badgeTone: "red",
    images: [img10, img9, img8, img11],
    colors: [{ name: "Đen than", hex: "#151515" }, { name: "Nâu chocolate", hex: "#55352a" }],
    sizes: ["M", "L", "XL"],
    material: "Giả da mềm ít nhăn",
    fit: "Relaxed coach fit",
    bullets: ["Nhẹ hơn da thật, dễ mặc lâu.", "Phù hợp outfit đi làm sáng tạo.", "Giữ tinh thần mạnh nhưng không quá nặng."],
    description: {
      intro: "Coach giả da là lựa chọn dễ tiếp cận khi cần outfit có chất mạnh mà vẫn linh hoạt.",
      material: "Giả da mềm, ít nhăn và dễ bảo quản hơn trong điều kiện sử dụng hằng ngày.",
      design: "Đường cắt gọn, giữ tinh thần coach với cảm giác hiện đại và dễ phối hơn.",
      fit: "Relaxed fit cho phép mặc cùng hoodie mỏng hoặc tee dày mà vẫn thoải mái."
    }
  },
  {
    id: 11,
    name: "Áo Khoác Coach Lông Cừu DirtyWave",
    category: "Coach",
    price: 399000,
    originalPrice: 559000,
    sku: "ATID070-11",
    badge: "Giá tốt",
    badgeTone: "red",
    images: [img11, img8, img9, img10],
    colors: [{ name: "Kem cát", hex: "#d6cebf" }, { name: "Nâu đậm", hex: "#5a4336" }],
    sizes: ["S", "M", "L"],
    material: "Lông cừu nhân tạo mềm",
    fit: "Warm relaxed fit",
    bullets: ["Giữ ấm ổn trong ngày lạnh nhẹ.", "Bề mặt nổi khối tạo điểm nhìn mạnh.", "Phối đẹp với quần đen và boot."],
    description: {
      intro: "Một mẫu coach jacket thiên về chất liệu và cảm giác ấm, dành cho những set đồ có texture rõ.",
      material: "Bề mặt lông cừu nhân tạo mềm, tạo chiều sâu thị giác và cảm giác mặc êm hơn.",
      design: "Cấu trúc tổng thể tối giản để phần chất liệu trở thành điểm nhấn chính của outfit.",
      fit: "Relaxed fit ấm áp, dễ layer với áo len hoặc sweatshirt mỏng."
    }
  }
]

const profileOpen = ref(false)
const mobileOpen = ref(false)
const voucherDrawerOpen = ref(false)
const selectedVoucherInDetail = ref(null)
const searchQuery = ref("")
const activeImage = ref("")
const quantity = ref(1)
const selectedColor = ref("")
const selectedSize = ref("")
const activeTab = ref("description")
const vouchers = ref([])
const loadingVouchers = ref(false)
const backendProducts = ref([])
const userAvatar = ref("")
const userDisplayName = ref("Khách hàng")
const userRoleLabel = ref("Khách hàng")
const cartVersion = ref(0)
const CART_UPDATED_EVENT = "dirtywave:cart-updated"
const quickPreviewProduct = ref(null)
const quickPreviewColor = ref("")
const quickPreviewSize = ref("")
const quickPreviewQty = ref(1)

const getAvatarStorageKey = (accountId) => `avatar:${accountId}`

const normalizeRole = (role) => String(role || "").trim().toUpperCase().replace(/^ROLE_/, "")

const toRoleLabel = (role) => {
  const normalized = normalizeRole(role)
  if (normalized === "ADMIN") return "Quản trị viên"
  if (normalized === "EMPLOYEE") return "Nhân viên"
  return "Khách hàng"
}

const toDisplayNameFromEmail = (email = "") => {
  const localPart = String(email || "").split("@")[0].replace(/[._-]+/g, " ").trim()
  if (!localPart) return "Khách hàng"
  return localPart.split(/\s+/).map((word) => word.charAt(0).toUpperCase() + word.slice(1)).join(" ")
}

const VND = (value) => new Intl.NumberFormat("vi-VN").format(Number(value || 0)) + "₫"

const normalizeKeyword = (value = "") => String(value)
  .normalize("NFD")
  .replace(/[\u0300-\u036f]/g, "")
  .toLowerCase()
  .trim()

const resolveProductId = (item) => {
  const id = Number(item?.id ?? item?.idSanPham ?? item?.sanPhamId ?? item?.productId)
  return Number.isFinite(id) && id > 0 ? id : 0
}

const isAbsoluteUrl = (value = "") => /^https?:\/\//i.test(value) || /^data:image\//i.test(value)

const isImageString = (value = "") => {
  const raw = String(value || "").trim()
  if (!raw) return false
  if (isAbsoluteUrl(raw)) return true

  const normalized = raw.replace(/\\/g, "/").split(/[?#]/)[0]
  if (normalized.startsWith("/uploads/") || normalized.startsWith("uploads/")) return true
  return /\.(png|jpe?g|gif|webp|svg|bmp|avif)$/i.test(normalized)
}

const toImageUrl = (value) => {
  if (!value) return ""
  const raw = String(value).trim()
  if (!raw) return ""
  if (isAbsoluteUrl(raw)) return raw

  const normalized = raw.replace(/\\/g, "/")
  const uploadsMatch = normalized.match(/(?:^|\/)(uploads\/.*)$/i)
  if (uploadsMatch?.[1]) return `${BACKEND_ORIGIN}/${uploadsMatch[1]}`
  if (normalized.startsWith("/uploads/")) return `${BACKEND_ORIGIN}${normalized}`
  if (normalized.startsWith("uploads/")) return `${BACKEND_ORIGIN}/${normalized}`
  if (normalized.startsWith("assets/") || normalized.startsWith("img/")) return `/${normalized}`
  if (normalized.startsWith("/")) return normalized
  return normalized
}

const pickImageValues = (entry) => {
  if (!entry) return []
  if (typeof entry === "string") {
    if (!isImageString(entry)) return []
    const parsed = toImageUrl(entry)
    return parsed ? [parsed] : []
  }
  if (Array.isArray(entry)) {
    return entry.flatMap((item) => pickImageValues(item)).filter(Boolean)
  }
  if (typeof entry === "object") {
    const bucket = []
    const directKeys = ["anh", "hinhAnh", "image", "imageUrl", "duongDanAnh", "images", "listAnh", "anhChinh", "thumbnail", "src"]
    for (const key of directKeys) {
      bucket.push(...pickImageValues(entry[key]))
    }
    return bucket
  }
  return []
}

const fallbackImageFor = (id, code = "") => {
  const numericId = Number(id)
  if (Number.isFinite(numericId) && numericId > 0) {
    return fallbackImages[(numericId - 1) % fallbackImages.length]
  }

  const codeNumber = Number(String(code || "").replace(/\D+/g, ""))
  if (Number.isFinite(codeNumber) && codeNumber > 0) {
    return fallbackImages[(codeNumber - 1) % fallbackImages.length]
  }

  return fallbackImages[0]
}

const normalizeBackendProduct = (item) => {
  const variants = Array.isArray(item?.sanPhamChiTiets) ? item.sanPhamChiTiets : []
  const id = resolveProductId(item)
  const code = String(item?.maSanPham || item?.ma || "")
  const category = String(item?.danhMuc?.tenDanhMuc || item?.loai?.tenLoai || "Thời trang nam")
  const variantPrices = variants.map((variant) => Number(variant?.giaBan || 0)).filter((n) => n > 0)
  const variantOriginalPrices = variants.map((variant) => Number(variant?.giaNhap || 0)).filter((n) => n > 0)
  const overrideImages = getProductImageOverride({ id, maSanPham: code })

  const images = overrideImages.length ? overrideImages : [...new Set(pickImageValues([
    item?.anh,
    item?.hinhAnh,
    item?.images,
    item?.image,
    item?.listAnh,
    item?.anhChinh,
    variants
  ]))]

  const colors = [...new Set(
    variants
      .map((variant) => String(variant?.mauSac?.tenMau || "").trim())
      .filter(Boolean)
  )].map((name) => ({ name, hex: colorHexByName(name) }))

  const sizes = [...new Set(
    variants
      .map((variant) => String(variant?.kichThuoc?.tenKichThuoc || "").trim())
      .filter(Boolean)
  )]

  const price = variantPrices.length
    ? Math.min(...variantPrices)
    : Number(item?.giaBan || item?.gia || 0)

  const originalPrice = variantOriginalPrices.length
    ? Math.max(...variantOriginalPrices)
    : Number(item?.giaGoc || item?.giaNiemYet || 0)

  const descriptionText = String(item?.moTa || "").trim()

  return {
    id,
    raw: item,
    name: String(item?.tenSanPham || item?.name || "Sản phẩm"),
    category,
    price,
    originalPrice,
    sku: code,
    badge: String(item?.trangThai || "").toLowerCase().includes("ngung") ? "Hết hàng" : "Còn hàng",
    badgeTone: String(item?.trangThai || "").toLowerCase().includes("ngung") ? "dark" : "red",
    images: images.length ? images : [fallbackImageFor(id, code)],
    colors,
    sizes,
    material: "Chất liệu theo biến thể",
    fit: String(item?.loai?.tenLoai || "Form tiêu chuẩn"),
    bullets: [
      "Sản phẩm được đồng bộ trực tiếp từ dữ liệu hệ thống.",
      "Màu sắc và kích thước hiển thị theo biến thể hiện có.",
      "Giá bán được lấy theo biến thể thấp nhất đang khả dụng."
    ],
    description: {
      intro: descriptionText || "Sản phẩm đang được cập nhật mô tả chi tiết.",
      material: "Thông tin chất liệu được xác định theo từng biến thể của sản phẩm.",
      design: "Thiết kế và thông số chi tiết phụ thuộc dữ liệu quản trị đã nhập.",
      fit: "Form hiển thị theo loại sản phẩm trong hệ thống."
    }
  }
}

const EMPTY_PRODUCT = {
  id: 0,
  raw: null,
  name: "Không tìm thấy sản phẩm",
  category: "Sản phẩm",
  price: 0,
  originalPrice: 0,
  sku: "",
  badge: "Không khả dụng",
  badgeTone: "dark",
  images: [fallbackImages[0]],
  colors: [],
  sizes: [],
  material: "Đang cập nhật",
  fit: "Đang cập nhật",
  bullets: [],
  description: {
    intro: "Sản phẩm này không tồn tại hoặc đã bị ẩn khỏi hệ thống.",
    material: "",
    design: "",
    fit: ""
  }
}

const productCatalog = computed(() => {
  return backendProducts.value
    .map(normalizeBackendProduct)
    .filter((item) => Number(item.id) > 0)
})

const currentProduct = computed(() => {
  const routeId = Number(route.params.id)
  if (!Number.isFinite(routeId) || routeId <= 0) return EMPTY_PRODUCT
  return productCatalog.value.find((item) => Number(item.id) === routeId) || EMPTY_PRODUCT
})

const createPlaceholderImage = (index) => {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="900" height="1200" viewBox="0 0 900 1200"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#f8fafc"/><stop offset="100%" stop-color="#e5e7eb"/></linearGradient></defs><rect width="900" height="1200" fill="url(#g)"/><rect x="190" y="380" width="520" height="440" rx="16" fill="none" stroke="#cbd5e1" stroke-width="10" stroke-dasharray="20 16"/><circle cx="450" cy="600" r="60" fill="#e5e7eb"/><rect x="330" y="710" width="240" height="26" rx="13" fill="#e5e7eb"/><rect x="360" y="748" width="180" height="22" rx="11" fill="#e5e7eb"/></svg>`
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`
}

const displayImages = computed(() => {
  const baseImages = Array.isArray(currentProduct.value.images)
    ? currentProduct.value.images.filter(Boolean)
    : []

  if (baseImages.length) return baseImages
  return [createPlaceholderImage(0)]
})

const activeImageIndex = computed(() => {
  const idx = displayImages.value.findIndex((image) => image === activeImage.value)
  return idx >= 0 ? idx : 0
})

const selectGalleryImage = (image) => {
  activeImage.value = image
}

const showPrevImage = () => {
  if (!displayImages.value.length) return
  const nextIndex = (activeImageIndex.value - 1 + displayImages.value.length) % displayImages.value.length
  activeImage.value = displayImages.value[nextIndex]
}

const showNextImage = () => {
  if (!displayImages.value.length) return
  const nextIndex = (activeImageIndex.value + 1) % displayImages.value.length
  activeImage.value = displayImages.value[nextIndex]
}

const matchedBackendProduct = computed(() => {
  return currentProduct.value?.raw || null
})

const displayedProductCode = computed(() => {
  return matchedBackendProduct.value?.maSanPham || currentProduct.value.sku
})

const backendVariants = computed(() => {
  const rows = Array.isArray(matchedBackendProduct.value?.sanPhamChiTiets)
    ? matchedBackendProduct.value.sanPhamChiTiets
    : []

  return rows.map((variant, index) => ({
    id: variant?.id,
    colorName: String(variant?.mauSac?.tenMau || "").trim(),
    sizeName: String(variant?.kichThuoc?.tenKichThuoc || "").trim(),
    price: Number(variant?.giaBan || 0),
    sortIndex: index
  }))
})

const colorHexByName = (name) => {
  const normalized = normalizeKeyword(name)
  if (normalized.includes("den")) return "#1a1a1a"
  if (normalized.includes("trang") || normalized.includes("kem")) return "#ded7ca"
  if (normalized.includes("nau")) return "#6b412c"
  if (normalized.includes("hong")) return "#d684a1"
  if (normalized.includes("xam") || normalized.includes("ghi")) return "#7f858f"
  if (normalized.includes("xanh")) return "#2f4f75"
  return "#9ca3af"
}

const effectiveColors = computed(() => {
  if (backendVariants.value.length) {
    const colorMap = new Map()
    for (const variant of backendVariants.value) {
      if (!variant.colorName || colorMap.has(variant.colorName)) continue
      colorMap.set(variant.colorName, { name: variant.colorName, hex: colorHexByName(variant.colorName) })
    }
    if (colorMap.size) return [...colorMap.values()]
  }

  return (currentProduct.value.colors || []).map((color) => ({
    name: color?.name || "",
    hex: color?.hex || colorHexByName(color?.name)
  }))
})

const effectiveSizes = computed(() => {
  if (backendVariants.value.length) {
    const options = backendVariants.value
      .filter((variant) => !selectedColor.value || variant.colorName === selectedColor.value)
      .map((variant) => variant.sizeName)
      .filter(Boolean)

    const unique = [...new Set(options)]
    if (unique.length) return unique
  }

  return currentProduct.value.sizes || []
})

const selectedBackendVariant = computed(() => {
  if (!backendVariants.value.length) return null
  const exact = backendVariants.value.find((variant) => {
    return variant.colorName === selectedColor.value && variant.sizeName === selectedSize.value
  })
  if (exact) return exact

  return backendVariants.value.find((variant) => variant.colorName === selectedColor.value)
    || backendVariants.value.find((variant) => variant.sizeName === selectedSize.value)
    || backendVariants.value[0]
})

const effectivePrice = computed(() => {
  const backendPrice = Number(selectedBackendVariant.value?.price || 0)
  if (backendPrice > 0) return backendPrice
  return Number(currentProduct.value.price || 0)
})

const effectiveOriginalPrice = computed(() => {
  const oldPrice = Number(currentProduct.value.originalPrice || 0)
  const nowPrice = Number(effectivePrice.value || 0)
  if (oldPrice > nowPrice) return oldPrice
  return 0
})

const relatedProducts = computed(() => {
  const currentId = currentProduct.value.id
  const source = productCatalog.value
  const sameCategory = source.filter((item) => item.id !== currentId && item.category === currentProduct.value.category)
  const otherProducts = source.filter((item) => item.id !== currentId && item.category !== currentProduct.value.category)
  return [...sameCategory, ...otherProducts].slice(0, 5)
})

const productDiscount = computed(() => {
  const oldPrice = Number(effectiveOriginalPrice.value || 0)
  const currentPrice = Number(effectivePrice.value || 0)
  if (!oldPrice || oldPrice <= currentPrice) return 0
  return Math.round(((oldPrice - currentPrice) / oldPrice) * 100)
})

const userInitials = computed(() => {
  const words = String(userDisplayName.value || "KH").trim().split(/\s+/).filter(Boolean)
  if (words.length >= 2) return `${words[0][0]}${words[words.length - 1][0]}`.toUpperCase()
  return String(userDisplayName.value || "KH").slice(0, 2).toUpperCase()
})

const cartCount = computed(() => {
  cartVersion.value
  const stored = readCartObject()
  return Object.values(stored).reduce((sum, value) => sum + Number(value || 0), 0)
})

const voucherBaseAmount = computed(() => Number(effectivePrice.value || 0) * Number(quantity.value || 1))

const applicableVouchers = computed(() => {
  return vouchers.value.filter((voucher) => isVoucherApplicable(voucher, voucherBaseAmount.value, null))
})

const promoLines = computed(() => {
  return vouchers.value.map((voucher) => {
    const val = Number(voucher.giaTriGiamGia || 0)
    const discountLabel = voucher.hinhThucGiam
      ? `giảm ${val}%`
      : `giảm ${val >= 1000 ? Math.round(val / 1000) + 'K' : val + 'đ'}`
    return { code: voucher.maPhieuGiamGia, discountLabel, min: VND(voucher.hoaDonToiThieu || 0) }
  })
})

// Show all loaded vouchers – backend filters by active; no extra client-side blocking
const voucherChips = computed(() => vouchers.value)

const voucherPreview = computed(() => voucherChips.value)

const voucherChipLabel = (voucher) => {
  return String(voucher?.maPhieuGiamGia || "")
}

const syncProductState = () => {
  activeImage.value = displayImages.value[0] || ""
  selectedColor.value = effectiveColors.value[0]?.name || ""
  selectedSize.value = effectiveSizes.value[0] || ""
  quantity.value = 1
}

const loadBackendProducts = async () => {
  try {
    const response = await getAllSanPham()
    backendProducts.value = Array.isArray(response?.data) ? response.data : []
  } catch {
    backendProducts.value = []
  }
}

const loadCurrentUser = async () => {
  const userEmail = String(localStorage.getItem("userEmail") || "").trim().toLowerCase()
  userAvatar.value = ""
  userRoleLabel.value = "Khách hàng"
  if (userEmail) userDisplayName.value = toDisplayNameFromEmail(userEmail)

  try {
    const account = await resolveAccountByRole({
      service: taiKhoanService,
      expectedRoles: ['CUSTOMER', 'KHACH_HANG', 'KHACHHANG', 'USER'],
      allowFallback: false
    })
    if (!account) return

    userRoleLabel.value = toRoleLabel(account?.vaiTro)
    if (account?.email) userDisplayName.value = toDisplayNameFromEmail(account.email)

    try {
      const customerRes = await getKhachHangByTaiKhoanId(account.id)
      if (customerRes?.data?.tenKhachHang) {
        userDisplayName.value = String(customerRes.data.tenKhachHang)
      }
    } catch {
      const mappedName = getVietnameseNameByEmail(account?.email)
      if (mappedName) userDisplayName.value = mappedName
    }

    const localAvatar = account?.id ? localStorage.getItem(getAvatarStorageKey(account.id)) : ""
    userAvatar.value = localAvatar || String(account?.avatar || "")
  } catch {
    userAvatar.value = ""
  }
}

const loadVouchers = async () => {
  loadingVouchers.value = true
  try {
    const normalizeVoucher = (voucher) => ({
      ...voucher,
      id: voucher?.id ?? voucher?.phieuGiamGiaId ?? voucher?.maPhieuGiamGia ?? voucher?.maPhieu,
      maPhieuGiamGia: String(voucher?.maPhieuGiamGia || voucher?.maPhieu || voucher?.maKhuyenMai || voucher?.code || "").trim(),
      tenPhieuGiamGia: String(voucher?.tenPhieuGiamGia || voucher?.tenKhuyenMai || voucher?.tenPhieu || "").trim(),
      giaTriGiamGia: Number(voucher?.giaTriGiamGia ?? voucher?.giaTriGiam ?? 0),
      hoaDonToiThieu: Number(voucher?.hoaDonToiThieu ?? voucher?.donToiThieu ?? voucher?.dieuKienToiThieu ?? 0),
      soTienGiamToiDa: Number(voucher?.soTienGiamToiDa ?? 0)
    })

    let payload = []
    try {
      const response = await getActiveVouchers()
      const activeData = response?.data
      payload = Array.isArray(activeData) ? activeData : (Array.isArray(activeData?.content) ? activeData.content : [])
      if (!payload.length) {
        const fallbackResponse = await getAllVouchers()
        const fallbackData = fallbackResponse?.data
        payload = Array.isArray(fallbackData) ? fallbackData : (Array.isArray(fallbackData?.content) ? fallbackData.content : [])
      }
    } catch {
      const response = await getAllVouchers()
      const allData = response?.data
      payload = Array.isArray(allData) ? allData : (Array.isArray(allData?.content) ? allData.content : [])
    }
    vouchers.value = payload.map(normalizeVoucher).filter((voucher) => voucher.maPhieuGiamGia)
  } catch {
    vouchers.value = []
  } finally {
    loadingVouchers.value = false
  }
}

const selectVoucher = (voucher) => {
  if (!isVoucherApplicable(voucher, voucherBaseAmount.value, null)) {
    const needed = Number(voucher.hoaDonToiThieu || 0) - voucherBaseAmount.value
    if (needed > 0) {
      toast.error(`Cần thêm ${VND(needed)} để áp dụng voucher này`)
    } else {
      toast.error('Voucher không khả dụng')
    }
    return
  }
  selectedVoucherInDetail.value = voucher
  try { localStorage.setItem('checkoutSelectedVoucher', JSON.stringify(voucher)) } catch {}
  toast.success(`Đã áp dụng ${voucher.maPhieuGiamGia} – tiết kiệm ${VND(calculateVoucherDiscount(voucher, voucherBaseAmount.value))}`)
  closeVoucherDrawer()
}

const validateSelection = () => {
  if (!selectedColor.value) {
    toast.error("Vui lòng chọn màu sắc")
    return false
  }
  if (!selectedSize.value) {
    toast.error("Vui lòng chọn kích thước")
    return false
  }

  const availableStock = Number(selectedBackendVariant.value?.soLuong || 0)
  if (availableStock > 0 && Number(quantity.value || 0) > availableStock) {
    toast.error(`Số lượng vượt tồn kho. Còn lại: ${availableStock}`)
    return false
  }

  if (Number(quantity.value || 0) <= 0) {
    toast.error("Số lượng phải lớn hơn 0")
    return false
  }

  return true
}

const refreshCartCount = () => {
  cartVersion.value += 1
}

const notifyCartUpdated = () => {
  window.dispatchEvent(new Event(CART_UPDATED_EVENT))
}

const addToCart = () => {
  if (!validateSelection()) return
  const storedCart = readCartObject()
  const key = String(currentProduct.value.id)
  storedCart[key] = Number(storedCart[key] || 0) + quantity.value
  writeCartObject(storedCart)
  refreshCartCount()
  notifyCartUpdated()
  toast.success(`Đã thêm ${quantity.value} sản phẩm vào giỏ hàng`)
}

const buyNow = () => {
  if (!validateSelection()) return
  if (selectedVoucherInDetail.value) {
    try { localStorage.setItem('checkoutSelectedVoucher', JSON.stringify(selectedVoucherInDetail.value)) } catch {}
  } else {
    localStorage.removeItem('checkoutSelectedVoucher')
  }
  writeCheckoutCartArray([{
    id: currentProduct.value.id,
    name: currentProduct.value.name,
    price: effectivePrice.value,
    quantity: quantity.value,
    size: selectedSize.value,
    color: selectedColor.value,
    spctId: selectedBackendVariant.value?.id || null,
    image: activeImage.value || currentProduct.value.images[0]
  }])
  router.push("/checkout")
}

const handleSearch = () => {
  if (!searchQuery.value.trim()) {
    router.push("/trang-chu")
    return
  }
  router.push({ path: "/trang-chu", query: { q: searchQuery.value.trim() } })
}

const closeVoucherDrawer = () => {
  voucherDrawerOpen.value = false
  document.body.style.overflow = ""
}

const openVoucherDrawer = () => {
  voucherDrawerOpen.value = true
  document.body.style.overflow = "hidden"
}

const openProfilePage = () => {
  profileOpen.value = false
  router.push("/customer/profile")
}

const openOrdersPage = () => {
  profileOpen.value = false
  router.push({ path: "/customer/profile", query: { tab: "orders" } })
}

const toggleMobileMenu = () => {
  mobileOpen.value = !mobileOpen.value
}

const toggleProfileMenu = (event) => {
  event.stopPropagation()
  profileOpen.value = !profileOpen.value
}

const browseCategory = (category) => {
  mobileOpen.value = false
  router.push({ path: "/trang-chu", query: category ? { category } : {} })
}

const openHomeAnchor = (hash = "") => {
  mobileOpen.value = false
  router.push(hash ? { path: "/trang-chu", hash: `#${hash}` } : "/trang-chu")
}

const goHome = () => router.push("/trang-chu")
const openCart = () => router.push("/gio-hang")
const goToProductDetail = (id) => {
  closeQuickPreview()
  router.push(`/product/${id}`)
}

const openQuickPreview = (product) => {
  quickPreviewProduct.value = product
  quickPreviewColor.value = product?.colors?.[0]?.name || ""
  quickPreviewSize.value = product?.sizes?.[0] || ""
  quickPreviewQty.value = 1
}

const closeQuickPreview = () => {
  quickPreviewProduct.value = null
}

const getQuickPreviewCode = (product) => {
  if (!product) return ""
  const byId = backendProducts.value.find(item => Number(item?.id) === Number(product.id))
  if (byId?.maSanPham) return byId.maSanPham
  const localName = normalizeKeyword(product.name)
  const byName = backendProducts.value.find(item => normalizeKeyword(item?.tenSanPham) === localName)
  return byName?.maSanPham || product.sku || String(product.id)
}

const quickPreviewAddToCart = () => {
  if (!quickPreviewProduct.value?.id) return
  const storedCart = readCartObject()
  const key = String(quickPreviewProduct.value.id)
  storedCart[key] = Number(storedCart[key] || 0) + quickPreviewQty.value
  writeCartObject(storedCart)
  refreshCartCount()
  notifyCartUpdated()
  toast.success(`Đã thêm ${quickPreviewQty.value} sản phẩm vào giỏ hàng`)
  closeQuickPreview()
}

const logout = () => {
  profileOpen.value = false
  localStorage.removeItem("role")
  localStorage.removeItem("userId")
  localStorage.removeItem("userEmail")
  router.push("/auth/customer-login")
}

const handleDocumentClick = (event) => {
  const target = event.target
  if (!(target instanceof Element)) return
  if (!target.closest(".pd-profile")) {
    profileOpen.value = false
  }
}

watch(() => route.params.id, () => {
  profileOpen.value = false
  mobileOpen.value = false
  closeVoucherDrawer()
  closeQuickPreview()
  window.scrollTo({ top: 0, behavior: "auto" })
})

watch(
  () => currentProduct.value.id,
  () => {
    syncProductState()
  },
  { immediate: true }
)

watch(selectedColor, () => {
  if (!effectiveSizes.value.includes(selectedSize.value)) {
    selectedSize.value = effectiveSizes.value[0] || ""
  }
})

watch(effectiveColors, (colors) => {
  if (!colors.some((color) => color.name === selectedColor.value)) {
    selectedColor.value = colors[0]?.name || ""
  }
})

onMounted(async () => {
  await Promise.all([loadCurrentUser(), loadVouchers(), loadBackendProducts()])
  syncProductState()
  document.addEventListener("click", handleDocumentClick)
  window.addEventListener("storage", refreshCartCount)
  window.addEventListener(AUTH_CONTEXT_CHANGED_EVENT, loadCurrentUser)
})

onUnmounted(() => {
  document.removeEventListener("click", handleDocumentClick)
  window.removeEventListener("storage", refreshCartCount)
  window.removeEventListener(AUTH_CONTEXT_CHANGED_EVENT, loadCurrentUser)
  document.body.style.overflow = ""
})
</script>

<template>
  <div class="product-detail-page">
    <SiteNav :cart-count="cartCount" />

    <main class="pd-shell">
      <div class="pd-breadcrumb">
        <button type="button" @click="goHome">Trang chủ</button>
        <ChevronRight :size="14" />
        <strong>{{ currentProduct.name }}</strong>
      </div>

      <section class="pd-main">
        <div class="pd-gallery" :class="{ 'pd-gallery--single': displayImages.length <= 1 }">
          <div v-if="displayImages.length > 1" class="pd-gallery__thumbs">
            <button
              v-for="(image, index) in displayImages"
              :key="`${image}-${index}`"
              type="button"
              class="pd-gallery__thumb"
              :class="{ 'is-active': image === activeImage }"
              @click="selectGalleryImage(image)"
            >
              <img :src="image" :alt="currentProduct.name" />
            </button>
          </div>

          <div class="pd-gallery__stage">
            <img :src="activeImage || displayImages[0]" :alt="currentProduct.name" />
            <button v-if="displayImages.length > 1" type="button" class="pd-gallery__arrow pd-gallery__arrow--left" aria-label="Ảnh trước" @click="showPrevImage">
              <ChevronLeft :size="18" />
            </button>
            <button v-if="displayImages.length > 1" type="button" class="pd-gallery__arrow pd-gallery__arrow--right" aria-label="Ảnh tiếp theo" @click="showNextImage">
              <ChevronRight :size="18" />
            </button>
          </div>
        </div>

        <div class="pd-info">
          <div class="pd-info__panel">
            <div class="pd-info__topline">
              <h1>{{ currentProduct.name }}</h1>
              <span class="pd-stock" :class="`is-${currentProduct.badgeTone}`">{{ currentProduct.badge }}</span>
            </div>

            <p class="pd-sku">Mã sản phẩm: {{ displayedProductCode }}</p>

            <div class="pd-price-row">
              <strong>{{ VND(effectivePrice) }}</strong>
              <s v-if="effectiveOriginalPrice">{{ VND(effectiveOriginalPrice) }}</s>
              <span v-if="productDiscount" class="pd-discount">-{{ productDiscount }}%</span>
            </div>

            <section class="pd-promo-box">
              <div class="pd-promo-box__title">
                <Ticket :size="15" />
                <strong>Ưu đãi online</strong>
              </div>
              <ul>
                <li v-for="line in promoLines" :key="line.code">
                  Nhập mã <b>{{ line.code }}</b> {{ line.discountLabel }} đơn từ {{ line.min }}
                </li>
                <li v-if="!promoLines.length">Hiện chưa có voucher khả dụng theo dữ liệu khuyến mãi hiện tại.</li>
                <li>Freeship đơn từ 299K</li>
              </ul>
            </section>

            <div class="pd-voucher-row">
              <span class="pd-label">Mã giảm giá</span>
              <div class="pd-voucher-row__chips">
                <button
                  v-for="voucher in voucherPreview"
                  :key="voucher.id || voucher.maPhieuGiamGia"
                  type="button"
                  class="pd-voucher-chip"
                  :class="{ 'is-selected': selectedVoucherInDetail?.maPhieuGiamGia === voucher.maPhieuGiamGia }"
                  @click="openVoucherDrawer"
                >
                  {{ voucherChipLabel(voucher) }}
                </button>
              </div>
              <small v-if="selectedVoucherInDetail" class="pd-selected-voucher-hint">
                ✓ {{ selectedVoucherInDetail.maPhieuGiamGia }} đã được áp dụng
              </small>
            </div>

            <div class="pd-option-row">
              <span class="pd-label">Màu sắc: <b>{{ selectedColor }}</b></span>
              <div class="pd-colors">
                <button
                  v-for="color in effectiveColors"
                  :key="color.name"
                  type="button"
                  class="pd-color"
                  :class="{ 'is-active': selectedColor === color.name }"
                  :title="color.name"
                  @click="selectedColor = color.name"
                >
                  <span :style="{ background: color.hex }"></span>
                </button>
              </div>
            </div>

            <div class="pd-option-row pd-option-row--size">
              <div class="pd-size-head">
                <span class="pd-label">Kích thước</span>
                <button type="button" class="pd-size-guide">
                  <Ruler :size="14" />
                  Hướng dẫn chọn size
                </button>
              </div>
              <div class="pd-sizes">
                <button
                  v-for="size in effectiveSizes"
                  :key="size"
                  type="button"
                  class="pd-size"
                  :class="{ 'is-active': selectedSize === size }"
                  @click="selectedSize = size"
                >
                  {{ size }}
                </button>
              </div>
            </div>

            <div class="pd-buy-row">
              <div class="pd-qty">
                <button type="button" @click="quantity = Math.max(1, quantity - 1)">-</button>
                <strong>{{ quantity }}</strong>
                <button type="button" @click="quantity += 1">+</button>
              </div>
              <button type="button" class="pd-cart-button" @click="addToCart">THÊM VÀO GIỎ</button>
            </div>

            <button type="button" class="pd-buy-now" @click="buyNow">MUA NGAY</button>

            <div class="pd-service-grid">
              <article>
                <Truck :size="16" />
                <span>Freeship đơn từ 299K</span>
              </article>
              <article>
                <ShieldCheck :size="16" />
                <span>Cam kết chính hãng</span>
              </article>
              <article>
                <Wallet :size="16" />
                <span>Thanh toán COD</span>
              </article>
              <article>
                <Undo2 :size="16" />
                <span>Đổi hàng trong 15 ngày</span>
              </article>
            </div>
          </div>
        </div>
      </section>

      <section class="pd-tabs">
        <div class="pd-tabs__head">
          <button type="button" :class="{ active: activeTab === 'description' }" @click="activeTab = 'description'">Mô tả</button>
          <button type="button" :class="{ active: activeTab === 'shipping' }" @click="activeTab = 'shipping'">Chính sách giao hàng</button>
          <button type="button" :class="{ active: activeTab === 'return' }" @click="activeTab = 'return'">Chính sách đổi hàng</button>
        </div>

        <div v-if="activeTab === 'description'" class="pd-tabs__panel">
          <h2>{{ currentProduct.name }}</h2>
          <ul class="pd-bullets">
            <li><strong>Chất liệu:</strong> {{ currentProduct.material }}</li>
            <li><strong>Form:</strong> {{ currentProduct.fit }}</li>
          </ul>
          <p>{{ currentProduct.description.intro }}</p>
          <h3>Chất liệu</h3>
          <p>{{ currentProduct.description.material }}</p>
          <h3>Kỹ thuật thiết kế</h3>
          <p>{{ currentProduct.description.design }}</p>
          <h3>Form dáng</h3>
          <p>{{ currentProduct.description.fit }}</p>
        </div>

        <div v-else-if="activeTab === 'shipping'" class="pd-tabs__panel">
          <h2>Chính sách giao hàng</h2>
          <p>DirtyWave giao hàng toàn quốc. Đơn từ 299.000₫ được áp dụng ưu đãi phí vận chuyển theo từng thời điểm, và đơn từ 1.000.000₫ được miễn phí ship tại trang checkout hiện tại.</p>
          <p>Thời gian giao dự kiến từ 2 đến 5 ngày làm việc tuỳ khu vực nhận hàng.</p>
        </div>

        <div v-else class="pd-tabs__panel">
          <h2>Chính sách đổi hàng</h2>
          <p>Hỗ trợ đổi size trong vòng 15 ngày với sản phẩm còn nguyên tình trạng sử dụng, tem nhãn đầy đủ và không có dấu hiệu giặt tẩy.</p>
          <p>Bạn có thể theo dõi và quản lý đơn đổi trong trung tâm tài khoản.</p>
        </div>
      </section>

      <section class="pd-related">
        <h2>Sản phẩm cùng loại</h2>
        <div class="pd-related__grid">
          <article
            v-for="item in relatedProducts"
            :key="item.id"
            class="pd-related__card"
            @click="goToProductDetail(item.id)"
          >
            <div class="pd-related__image">
              <img :src="item.images[0]" :alt="item.name" />
              <span>{{ item.badge }}</span>
              <div class="pd-related__actions">
                <button type="button" class="pd-related__action" @click.stop="openQuickPreview(item)">
                  <Eye :size="16" />
                </button>
                <button type="button" class="pd-related__action" @click.stop="goToProductDetail(item.id)">
                  <ShoppingCart :size="16" />
                </button>
              </div>
            </div>
            <div class="pd-related__body">
              <small>{{ item.category }}</small>
              <p>{{ item.name }}</p>
              <strong>{{ VND(item.price) }}</strong>
              <s v-if="item.originalPrice">{{ VND(item.originalPrice) }}</s>
              <div class="pd-related__dots">
                <span v-for="color in item.colors.slice(0, 3)" :key="color.name" :style="{ background: color.hex }"></span>
              </div>
            </div>
          </article>
        </div>
      </section>
    </main>

    <div v-if="quickPreviewProduct" class="pd-quick-view" @click.self="closeQuickPreview">
      <article class="pd-quick-view__card">
        <button type="button" class="pd-quick-view__close" @click="closeQuickPreview">
          <X :size="16" />
        </button>
        <div class="pd-quick-view__image">
          <img :src="quickPreviewProduct.images[0]" :alt="quickPreviewProduct.name" />
        </div>
        <div class="pd-quick-view__body">
          <h3>{{ quickPreviewProduct.name }}</h3>
          <small class="pd-quick-view__code">Mã sản phẩm: {{ getQuickPreviewCode(quickPreviewProduct) }}</small>
          <strong class="pd-quick-view__price">{{ VND(quickPreviewProduct.price) }}</strong>

          <div class="pd-quick-view__option-row">
            <span class="pd-quick-view__label">Màu sắc: <b>{{ quickPreviewColor }}</b></span>
            <div class="pd-quick-view__swatches">
              <button
                v-for="color in quickPreviewProduct.colors"
                :key="color.name"
                type="button"
                class="pd-quick-view__swatch"
                :class="{ 'is-active': quickPreviewColor === color.name }"
                :style="{ background: color.hex }"
                :title="color.name"
                @click="quickPreviewColor = color.name"
              ></button>
            </div>
          </div>

          <div class="pd-quick-view__option-row">
            <span class="pd-quick-view__label">Kích thước: <b>{{ quickPreviewSize }}</b></span>
            <div class="pd-quick-view__sizes">
              <button
                v-for="size in quickPreviewProduct.sizes"
                :key="size"
                type="button"
                class="pd-quick-view__size"
                :class="{ 'is-active': quickPreviewSize === size }"
                @click="quickPreviewSize = size"
              >{{ size }}</button>
            </div>
          </div>

          <section class="pd-quick-view__promo">
            <div class="pd-quick-view__promo-title">
              <Ticket :size="13" />
              <b>ƯU ĐÃI ONLINE</b>
            </div>
            <ul>
              <li v-for="line in promoLines.slice(0, 3)" :key="line.code">
                  Nhập mã <b>{{ line.code }}</b> {{ line.discountLabel }} đơn từ {{ line.min }}
              </li>
            </ul>
          </section>

          <div class="pd-quick-view__buy-row">
            <div class="pd-quick-view__qty">
              <button type="button" @click="quickPreviewQty = Math.max(1, quickPreviewQty - 1)">−</button>
              <strong>{{ quickPreviewQty }}</strong>
              <button type="button" @click="quickPreviewQty += 1">+</button>
            </div>
            <button type="button" class="pd-quick-view__cart-btn" @click="quickPreviewAddToCart">THÊM VÀO GIỎ</button>
          </div>

          <button type="button" class="pd-quick-view__detail-link" @click="goToProductDetail(quickPreviewProduct.id)">
            Xem chi tiết »
          </button>
        </div>
      </article>
    </div>

    <transition name="pd-drawer">
      <div v-if="voucherDrawerOpen" class="pd-drawer-overlay" @click.self="closeVoucherDrawer">
        <aside class="pd-drawer">
          <div class="pd-drawer__head">
            <div>
              <small>Voucher giảm giá - áp dụng online</small>
              <h3>Kho voucher DirtyWave</h3>
            </div>
            <button type="button" class="pd-drawer__close" @click="closeVoucherDrawer">
              <X :size="18" />
            </button>
          </div>

          <div class="pd-drawer__list">
            <article
              v-for="voucher in voucherChips"
              :key="voucher.id || voucher.maPhieuGiamGia"
              class="pd-drawer__voucher"
              :class="{ 'is-selected': selectedVoucherInDetail?.maPhieuGiamGia === voucher.maPhieuGiamGia }"
            >
              <div class="pd-drawer__voucher-info">
                <span class="pd-drawer__label">{{ voucher.tenPhieuGiamGia || 'Voucher' }}</span>
                <strong class="pd-drawer__amount">{{ VND(calculateVoucherDiscount(voucher, voucherBaseAmount)) }}</strong>
                <p class="pd-drawer__min">Đơn từ {{ VND(voucher.hoaDonToiThieu || 0) }}</p>
                <p class="pd-drawer__code">Mã: <b>{{ voucher.maPhieuGiamGia }}</b></p>
              </div>
              <button
                type="button"
                class="pd-apply-btn"
                :class="{ 'is-applied': selectedVoucherInDetail?.maPhieuGiamGia === voucher.maPhieuGiamGia }"
                @click="selectVoucher(voucher)"
              >
                {{ selectedVoucherInDetail?.maPhieuGiamGia === voucher.maPhieuGiamGia ? '✓ Đã chọn' : 'Áp dụng' }}
              </button>
            </article>
            <p v-if="!voucherChips.length" class="pd-drawer__empty">
              Hiện không có phiếu giảm giá đang hoạt động.
            </p>
          </div>
        </aside>
      </div>
    </transition>
  </div>
</template>

<style>
.product-detail-page {
  --pd-red: #c5162d;
  --pd-red-dark: #8f1121;
  --pd-red-soft: #fff1f2;
  --pd-red-line: #f4c6cd;
  --pd-ink: #151515;
  --pd-muted: #6f6a6d;
  --pd-line: #e8d8db;
  --pd-bg: #fbf7f8;
  --pd-shadow: 0 18px 40px rgba(143, 17, 33, 0.08);
  min-height: 100vh;
  background: radial-gradient(circle at top right, rgba(197, 22, 45, 0.08), transparent 24%), linear-gradient(180deg, #fffdfd 0%, #f8f1f2 100%);
  color: var(--pd-ink);
}

.product-detail-page,
.product-detail-page * {
  box-sizing: border-box;
}

.product-detail-page button {
  appearance: none;
  -webkit-appearance: none;
}

.product-detail-page img {
  display: block;
  max-width: 100%;
}

.product-detail-page a {
  color: inherit;
  text-decoration: none;
}

.pd-topbar {
  position: relative;
  overflow: hidden;
  border-bottom: 1px solid rgba(255, 255, 255, 0.18);
  background: linear-gradient(135deg, var(--pd-red) 0%, var(--pd-red-dark) 100%);
}

.pd-site-header__inner,
.pd-shell {
  width: min(1260px, calc(100% - 36px));
  margin: 0 auto;
}

.pd-topbar__inner {
  width: 100%;
  padding: 10px 18px;
}

.pd-marquee {
  overflow: hidden;
  white-space: nowrap;
}

.pd-marquee__track {
  display: inline-block;
  padding-left: 100%;
  color: white;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.04em;
  animation: pd-marquee 24s linear infinite;
}

@keyframes pd-marquee {
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(-100%);
  }
}

.pd-site-header {
  position: sticky;
  top: 0;
  z-index: 30;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid rgba(143, 17, 33, 0.08);
  backdrop-filter: blur(14px);
  box-shadow: 0 12px 28px rgba(21, 21, 21, 0.08);
}

.pd-site-header__inner {
  padding: 12px 0 10px;
}

.pd-nav-shell {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 24px;
}

.pd-brand {
  border: 0;
  padding: 0;
  background: transparent;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.pd-brand__logo {
  width: 54px;
  height: 54px;
  border-radius: 14px;
  overflow: hidden;
  background: #111827;
  box-shadow: var(--pd-shadow);
  border-bottom: 3px solid var(--pd-red);
}

.pd-brand__logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scale(1.65);
  transform-origin: center;
  margin-top: -8px;
}

.pd-brand__text {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.05;
}

.pd-brand__text strong {
  font-size: 17px;
  letter-spacing: 0.03em;
}

.pd-brand__text small {
  color: var(--pd-muted);
  font-size: 12px;
}

.pd-menu {
  display: flex;
  align-items: center;
  gap: 24px;
  min-width: 0;
}

.pd-menu > a,
.pd-dropdown > a,
.profile-dropdown button,
.pd-breadcrumb button,
.pd-size-guide,
.pd-voucher-chip,
.pd-tabs__head button,
.pd-drawer__close,
.pd-copy,
.pd-store-note,
.pd-icon-button,
.pd-gallery__thumb,
.pd-color,
.pd-size,
.pd-cart-button,
.pd-buy-now,
.pd-mobile-menu__panel button,
.pd-pill-button {
  border: 0;
  background: transparent;
  cursor: pointer;
  font: inherit;
}

.pd-menu > a,
.pd-dropdown > a {
  white-space: nowrap;
  padding: 10px 8px;
  border-radius: 12px;
  color: var(--pd-ink);
  font-size: 14px;
  font-weight: 600;
}

.pd-menu > a:hover,
.pd-dropdown > a:hover {
  background: rgba(197, 22, 45, 0.06);
}

.pd-dropdown {
  position: relative;
}

.pd-dropdown:hover .pd-dropdown-panel {
  display: block;
}

.pd-dropdown-panel {
  position: absolute;
  top: calc(100% + 10px);
  left: 0;
  display: none;
  width: min(720px, 90vw);
  padding: 16px;
  border: 1px solid rgba(143, 17, 33, 0.12);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.99);
  box-shadow: 0 20px 50px rgba(21, 21, 21, 0.12);
}

.pd-dropdown-grid {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 14px;
}

.pd-panel-block {
  border: 1px solid rgba(143, 17, 33, 0.09);
  border-radius: 16px;
  padding: 16px;
  background: linear-gradient(180deg, #fffefe 0%, #fff9fa 100%);
}

.pd-panel-block h4 {
  margin: 0 0 10px;
  color: var(--pd-muted);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.pd-panel-block p {
  margin: 8px 0 0;
  color: var(--pd-muted);
  font-size: 13px;
  line-height: 1.6;
}

.pd-panel-links {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.pd-panel-links a {
  padding: 10px 12px;
  border: 1px solid rgba(143, 17, 33, 0.09);
  border-radius: 12px;
  background: white;
  font-size: 14px;
  font-weight: 600;
}

.pd-panel-links a:hover {
  border-color: rgba(197, 22, 45, 0.3);
  color: var(--pd-red-dark);
}

.pd-panel-block--cta {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 16px;
  background: linear-gradient(135deg, rgba(197, 22, 45, 0.96) 0%, rgba(143, 17, 33, 0.98) 100%);
}

.pd-panel-block--cta h4,
.pd-panel-block--cta p {
  color: rgba(255, 255, 255, 0.9);
}

.pd-panel-block--soft {
  background: linear-gradient(135deg, #ffe6ea 0%, #ffd6dd 100%);
}

.pd-panel-block--soft h4,
.pd-panel-block--soft p {
  color: var(--pd-red-dark);
}

.pd-pill-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  align-self: flex-start;
  min-height: 40px;
  padding: 0 16px;
  border-radius: 999px;
  background: white;
  color: var(--pd-red-dark);
  font-size: 13px;
  font-weight: 700;
}

.pd-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pd-search {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 320px;
  padding: 10px 14px;
  border: 1px solid var(--pd-line);
  background: white;
  border-radius: 999px;
  box-shadow: inset 0 0 0 1px rgba(197, 22, 45, 0.02);
}

.pd-search svg {
  color: #7b7276;
}

.pd-search input {
  width: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  font: inherit;
  color: var(--pd-ink);
}

.pd-search input::placeholder {
  color: #9b9094;
}

.pd-icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 42px;
  height: 42px;
  border: 1px solid var(--pd-line);
  border-radius: 12px;
  background: white;
  color: var(--pd-ink);
  box-shadow: var(--pd-shadow);
}

.pd-icon-button:hover {
  border-color: rgba(197, 22, 45, 0.28);
}

.pd-hamburger {
  display: none;
}

.pd-cart-icon {
  position: relative;
}

.cart-count {
  position: absolute;
  top: -7px;
  right: -7px;
  display: inline-grid;
  place-items: center;
  min-width: 19px;
  height: 19px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--pd-red);
  color: white;
  font-size: 11px;
  font-weight: 700;
}

.profile-wrapper {
  position: relative;
}

.user-account-btn {
  border: 1px solid var(--pd-line);
  background: #fff;
  border-radius: 12px;
  min-height: 42px;
  padding: 6px 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: var(--pd-shadow);
}

.profile-avatar-chip {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ffe4e8 0%, #ffd3da 100%);
  color: var(--pd-red-dark);
  font-size: 12px;
  font-weight: 700;
  overflow: hidden;
}

.profile-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-identity {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.15;
}

.profile-name {
  font-size: 13px;
  font-weight: 700;
  color: var(--pd-ink);
}

.profile-role {
  font-size: 11px;
  color: var(--pd-muted);
}

.profile-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 190px;
  padding: 8px;
  border: 1px solid var(--pd-line);
  border-radius: 14px;
  background: white;
  box-shadow: 0 18px 40px rgba(21, 21, 21, 0.14);
}

.dropdown-item {
  display: block;
  width: 100%;
  padding: 12px 14px;
  text-align: left;
  border-radius: 8px;
  color: var(--pd-ink);
  font-size: 14px;
  font-weight: 500;
}

.dropdown-item:hover {
  background: #f8f0f2;
}

.dropdown-item.danger {
  color: var(--pd-red);
}

.dropdown-item.danger:hover {
  background: #fee7eb;
}

.pd-mobile-menu {
  display: none;
  padding-top: 12px;
}

.pd-mobile-menu__panel {
  display: grid;
  gap: 8px;
  padding: 14px;
  border: 1px solid rgba(143, 17, 33, 0.1);
  border-radius: 18px;
  background: white;
  box-shadow: var(--pd-shadow);
}

.pd-mobile-menu__panel button {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  min-height: 42px;
  padding: 0 14px;
  border-radius: 12px;
  background: #fdf4f5;
  color: var(--pd-red-dark);
  font-weight: 700;
}

.pd-shell {
  padding: 18px 0 56px;
}

.pd-breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 18px;
  font-size: 12px;
  color: #6b7280;
}

.pd-breadcrumb strong {
  color: #111827;
}

.pd-main {
  display: grid;
  grid-template-columns: minmax(0, 520px) minmax(340px, 420px);
  justify-content: center;
  gap: 28px;
  align-items: start;
}

.pd-gallery {
  display: grid;
  grid-template-columns: 82px minmax(0, 1fr);
  gap: 14px;
}

.pd-gallery--single {
  grid-template-columns: minmax(0, 1fr);
}

.pd-gallery__thumbs {
  display: grid;
  gap: 10px;
  align-content: start;
}

.pd-gallery__thumb {
  padding: 0;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  overflow: hidden;
}

.pd-gallery__thumb.is-active {
  border-color: var(--pd-red);
  box-shadow: 0 0 0 1px rgba(197, 22, 45, 0.22);
}

.pd-gallery__thumb img {
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
}

.pd-gallery__stage {
  position: relative;
  overflow: hidden;
  border: 1px solid #eee4e6;
  background: linear-gradient(180deg, #eef1f1 0%, #e7e2de 100%);
  aspect-ratio: 3 / 4;
}

.pd-gallery__stage img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.pd-gallery__arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid #d1d5db;
  background: rgba(255, 255, 255, 0.9);
  color: #111827;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.pd-gallery__arrow--left { left: 10px; }

.pd-gallery__arrow--right { right: 10px; }

.pd-info {
  padding-top: 2px;
}

.pd-info__panel {
  display: grid;
  gap: 12px;
  padding: 4px 0;
}

.pd-info__topline {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.pd-info__topline h1 {
  margin: 0;
  font-size: 24px;
  line-height: 1.18;
}

.pd-stock {
  display: inline-flex;
  align-items: center;
  padding: 4px 9px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
}

.pd-stock.is-red {
  background: #fee7eb;
  color: var(--pd-red-dark);
}

.pd-stock.is-dark {
  background: #fee7eb;
  color: var(--pd-red-dark);
}

.pd-sku {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
}

.pd-price-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.pd-price-row strong {
  font-size: 44px;
  color: var(--pd-red-dark);
  line-height: 1;
}

.pd-price-row s {
  color: #9ca3af;
  font-size: 19px;
}

.pd-discount {
  display: inline-flex;
  padding: 5px 9px;
  border-radius: 999px;
  background: #3a0e14;
  color: white;
  font-size: 12px;
  font-weight: 700;
}

.pd-promo-box {
  padding: 14px 16px;
  border: 1px dashed #ef6a78;
  border-radius: 6px;
  background: #fff8f8;
}

.pd-promo-box__title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--pd-red);
  font-size: 12px;
  text-transform: uppercase;
}

.pd-promo-box ul {
  margin: 10px 0 0;
  padding-left: 16px;
  color: #374151;
  font-size: 13px;
  line-height: 1.8;
}

.pd-voucher-row,
.pd-option-row {
  margin-top: 0;
}

.pd-voucher-row__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.pd-label {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  color: #374151;
}

.pd-voucher-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 30px;
  min-width: 86px;
  padding: 0 11px;
  border: 1px solid #ef4444;
  border-radius: 4px;
  color: #b91c1c;
  background: #ffffff;
  font-size: 11px;
  font-weight: 700;
  clip-path: polygon(0 0, 100% 0, 100% 44%, 98.5% 50%, 100% 56%, 100% 100%, 0 100%, 0 56%, 1.5% 50%, 0 44%);
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}

.pd-voucher-chip.is-selected {
  background: var(--pd-red);
  border-color: var(--pd-red);
  color: white;
}

.pd-selected-voucher-hint {
  display: block;
  margin-top: 6px;
  color: #059669;
  font-size: 11px;
  font-weight: 600;
}

.pd-colors,
.pd-sizes {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pd-color {
  width: 28px;
  height: 28px;
  padding: 3px;
  border: 1px solid #d6ccd0;
  border-radius: 50%;
  background: white;
}

.pd-color span {
  display: block;
  width: 100%;
  height: 100%;
  border-radius: 50%;
}

.pd-color.is-active {
  border-color: var(--pd-red);
  box-shadow: 0 0 0 1px var(--pd-red);
}

.pd-size-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.pd-size-guide {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--pd-muted);
  font-size: 12px;
}

.pd-size {
  min-width: 40px;
  height: 38px;
  padding: 0 12px;
  border: 1px solid #dbd2d5;
  background: white;
  font-size: 12px;
}

.pd-size.is-active {
  border-color: var(--pd-red);
  background: #fff4f5;
  color: var(--pd-red);
}

.pd-buy-row {
  display: grid;
  grid-template-columns: 98px minmax(0, 1fr);
  gap: 12px;
  align-items: stretch;
  margin-top: 4px;
}

.pd-qty {
  display: grid;
  grid-template-columns: 30px 1fr 30px;
  align-items: center;
  border: 1px solid rgba(197, 22, 45, 0.34);
  border-radius: 4px;
  height: 46px;
  background: white;
}

.pd-qty button,
.pd-qty strong {
  display: grid;
  place-items: center;
}

.pd-qty button {
  border: 0;
  background: transparent;
  color: var(--pd-red-dark);
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
}

.pd-qty strong {
  font-size: 14px;
  color: #111827;
}

.pd-cart-button,
.pd-buy-now {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.03em;
}

.pd-cart-button {
  border: 1px solid rgba(197, 22, 45, 0.5);
  border-radius: 4px;
  background: white;
  color: var(--pd-red-dark);
}

.pd-buy-now {
  width: 100%;
  margin-top: 0;
  border-radius: 4px;
  background: linear-gradient(135deg, var(--pd-red), var(--pd-red-dark));
  color: white;
  box-shadow: var(--pd-shadow);
}

.pd-service-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  padding-top: 2px;
}

.pd-service-grid article {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12px;
  color: #5d5558;
}

.pd-tabs {
  margin-top: 28px;
}

.pd-tabs__head {
  display: flex;
  gap: 4px;
  border-bottom: 1px solid #ddd2d6;
}

.pd-tabs__head button {
  padding: 10px 14px;
  border: 1px solid #ddd2d6;
  border-bottom: 0;
  background: #fcf7f8;
  font-size: 12px;
  color: #374151;
}

.pd-tabs__head button.active {
  background: white;
  color: var(--pd-red);
}

.pd-tabs__panel {
  padding: 20px 0 0;
}

.pd-tabs__panel h2 {
  margin: 0 0 10px;
  font-size: 22px;
}

.pd-tabs__panel h3 {
  margin: 18px 0 8px;
  font-size: 17px;
}

.pd-tabs__panel p {
  margin: 0;
  line-height: 1.75;
  color: #374151;
}

.pd-bullets {
  margin: 0 0 16px;
  padding-left: 18px;
  line-height: 1.8;
}

.pd-related {
  margin-top: 36px;
}

.pd-related h2 {
  margin: 0 0 18px;
  font-size: 22px;
}

.pd-related__grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.pd-related__card {
  position: relative;
  padding: 0;
  overflow: hidden;
  border: 1px solid rgba(143, 17, 33, 0.1);
  border-radius: 18px;
  background: white;
  text-align: left;
  box-shadow: var(--pd-shadow);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.pd-related__card:hover {
  transform: translateY(-3px);
  box-shadow: 0 22px 44px rgba(143, 17, 33, 0.12);
}

.pd-related__image {
  position: relative;
  background: linear-gradient(180deg, #f8f5f5 0%, #f0eded 100%);
  aspect-ratio: 0.9;
}

.pd-related__image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 35%;
}

.pd-related__image span {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 4px 7px;
  border-radius: 999px;
  background: rgba(143, 17, 33, 0.94);
  color: white;
  font-size: 9px;
  font-weight: 700;
}

.pd-related__body {
  display: grid;
  gap: 6px;
  padding: 12px 12px 14px;
}

.pd-related__actions {
  position: absolute;
  left: 50%;
  bottom: 10px;
  transform: translate(-50%, 8px);
  display: flex;
  gap: 8px;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.pd-related__card:hover .pd-related__actions {
  opacity: 1;
  transform: translate(-50%, 0);
  pointer-events: auto;
}

.pd-related__action {
  width: 34px;
  height: 34px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.96);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #111827;
}

.pd-quick-view {
  position: fixed;
  inset: 0;
  z-index: 70;
  display: grid;
  place-items: center;
  padding: 18px;
  background: rgba(17, 24, 39, 0.55);
}

.pd-quick-view__card {
  width: min(820px, 100%);
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 320px);
  gap: 16px;
  border-radius: 16px;
  background: white;
  padding: 16px;
  position: relative;
}

.pd-quick-view__close {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 32px;
  height: 32px;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  background: white;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.pd-quick-view__image {
  border-radius: 12px;
  overflow: hidden;
  background: linear-gradient(180deg, #f8f5f5 0%, #f0eded 100%);
}

.pd-quick-view__image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 35%;
}

.pd-quick-view__body {
  display: grid;
  align-content: start;
  gap: 10px;
}

.pd-quick-view__body h3 {
  margin: 0;
  font-size: 22px;
}

.pd-quick-view__body small {
  color: #6b7280;
}

.pd-quick-view__body strong {
  color: var(--pd-red-dark);
  font-size: 28px;
}

.pd-quick-view__btn {
  min-height: 40px;
  border: 1px solid rgba(197, 22, 45, 0.4);
  border-radius: 8px;
  padding: 0 14px;
  background: #fff6f7;
  color: var(--pd-red-dark);
  font-weight: 700;
}

.pd-quick-view__code {
  color: #6b7280;
  font-size: 13px;
}

.pd-quick-view__price {
  color: var(--pd-red-dark) !important;
  font-size: 32px !important;
}

.pd-quick-view__option-row {
  display: grid;
  gap: 8px;
}

.pd-quick-view__label {
  font-size: 13px;
  color: #374151;
}

.pd-quick-view__swatches {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pd-quick-view__swatch {
  width: 26px;
  height: 26px;
  border-radius: 999px;
  border: 2px solid #e5e7eb;
  cursor: pointer;
  background: transparent;
}

.pd-quick-view__swatch.is-active {
  border-color: var(--pd-red);
  box-shadow: 0 0 0 1px var(--pd-red);
}

.pd-quick-view__sizes {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pd-quick-view__size {
  min-width: 38px;
  height: 34px;
  padding: 0 10px;
  border: 1px solid #dbd2d5;
  border-radius: 4px;
  background: white;
  font-size: 12px;
  cursor: pointer;
}

.pd-quick-view__size.is-active {
  border-color: var(--pd-red);
  background: #fff4f5;
  color: var(--pd-red);
}

.pd-quick-view__promo {
  padding: 10px 12px;
  border: 1px dashed #ef6a78;
  border-radius: 6px;
  background: #fff8f8;
}

.pd-quick-view__promo-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--pd-red);
  font-size: 11px;
  text-transform: uppercase;
}

.pd-quick-view__promo ul {
  margin: 6px 0 0;
  padding-left: 14px;
  font-size: 12px;
  line-height: 1.8;
  color: #374151;
}

.pd-quick-view__buy-row {
  display: grid;
  grid-template-columns: 90px minmax(0, 1fr);
  gap: 10px;
  align-items: stretch;
}

.pd-quick-view__qty {
  height: 40px;
  border: 1px solid rgba(197, 22, 45, 0.34);
  border-radius: 4px;
  background: white;
  display: grid;
  grid-template-columns: 28px 1fr 28px;
  align-items: center;
}

.pd-quick-view__qty button {
  border: 0;
  background: transparent;
  color: var(--pd-red-dark);
  font-size: 16px;
  cursor: pointer;
  display: grid;
  place-items: center;
}

.pd-quick-view__qty strong {
  text-align: center;
  font-size: 13px;
  color: #111827;
}

.pd-quick-view__cart-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  border: 1px solid rgba(197, 22, 45, 0.5);
  border-radius: 4px;
  background: white;
  color: var(--pd-red-dark);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.03em;
  cursor: pointer;
}

.pd-quick-view__detail-link {
  border: 0;
  background: transparent;
  padding: 0;
  color: #6b7280;
  text-decoration: underline;
  cursor: pointer;
  font-size: 13px;
  text-align: left;
}

.pd-related__body small {
  color: #8d7d81;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.pd-related__body p {
  margin: 0;
  min-height: 38px;
  color: #2f2a2c;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.45;
}

.pd-related__body strong {
  display: block;
  color: var(--pd-red-dark);
  font-size: 16px;
}

.pd-related__body s {
  color: #a99ca0;
  font-size: 12px;
}

.pd-related__dots {
  display: flex;
  gap: 5px;
  margin-top: 2px;
}

.pd-related__dots span {
  width: 11px;
  height: 11px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.12);
}

.pd-drawer-overlay {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  justify-content: flex-end;
  background: rgba(17, 24, 39, 0.32);
}

.pd-drawer {
  width: min(360px, 100%);
  height: 100%;
  padding: 18px;
  background: white;
  box-shadow: -20px 0 40px rgba(0, 0, 0, 0.12);
}

.pd-drawer__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.pd-drawer__head small {
  color: var(--pd-red);
  font-size: 11px;
  text-transform: uppercase;
}

.pd-drawer__head h3 {
  margin: 4px 0 0;
  font-size: 20px;
}

.pd-drawer__close {
  width: 36px;
  height: 36px;
  border: 1px solid var(--pd-line);
  background: white;
}

.pd-drawer__list {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.pd-drawer__voucher {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 14px;
  border: 1px solid #f0dde1;
  background: #fff7f8;
}

.pd-drawer__label {
  display: block;
  color: var(--pd-red);
  font-size: 10px;
  text-transform: uppercase;
}

.pd-drawer__voucher strong {
  display: block;
  margin-top: 4px;
  color: var(--pd-red-dark);
  font-size: 24px;
}

.pd-drawer__voucher p,
.pd-drawer__voucher small {
  color: #4b5563;
}

.pd-drawer__voucher-info {
  flex: 1;
  min-width: 0;
}

.pd-drawer__amount {
  display: block;
  margin-top: 4px;
  color: var(--pd-red-dark);
  font-size: 22px;
}

.pd-drawer__min,
.pd-drawer__code {
  margin: 2px 0 0;
  color: #4b5563;
  font-size: 12px;
}

.pd-drawer__code b {
  color: var(--pd-red-dark);
}

.pd-drawer__voucher.is-selected {
  border-color: var(--pd-red);
  background: #fff1f2;
}

.pd-apply-btn {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 12px;
  background: var(--pd-red-dark);
  color: white;
  font-size: 12px;
  font-weight: 600;
  border-radius: 4px;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.15s;
}

.pd-apply-btn.is-applied {
  background: #059669;
}

.pd-copy {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  background: var(--pd-red-dark);
  color: white;
  font-size: 12px;
}

.pd-drawer-enter-active,
.pd-drawer-leave-active {
  transition: opacity 0.22s ease;
}

.pd-drawer-enter-active .pd-drawer,
.pd-drawer-leave-active .pd-drawer {
  transition: transform 0.22s ease;
}

.pd-drawer-enter-from,
.pd-drawer-leave-to {
  opacity: 0;
}

.pd-drawer-enter-from .pd-drawer,
.pd-drawer-leave-to .pd-drawer {
  transform: translateX(100%);
}

@media (max-width: 1100px) {
  .pd-site-header__inner,
  .pd-shell {
    width: min(100% - 24px, 1260px);
  }

  .pd-topbar__inner {
    padding: 10px 12px;
  }

  .pd-menu {
    display: none;
  }

  .pd-hamburger {
    display: inline-flex;
  }

  .pd-mobile-menu {
    display: block;
  }

  .pd-main {
    grid-template-columns: 1fr;
  }

  .pd-service-grid,
  .pd-related__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .pd-site-header__inner,
  .pd-shell {
    width: min(100% - 18px, 1260px);
  }

  .pd-topbar__inner {
    padding: 10px 8px;
  }

  .pd-nav-shell {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .pd-actions {
    flex-wrap: wrap;
    justify-content: space-between;
  }

  .pd-search {
    width: 100%;
    min-width: 0;
    order: 3;
  }

  .pd-gallery {
    grid-template-columns: 1fr;
  }

  .pd-gallery__thumbs {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .pd-gallery__stage {
    min-height: 360px;
  }

  .pd-buy-row,
  .pd-service-grid,
  .pd-related__grid {
    grid-template-columns: 1fr;
  }

  .pd-dropdown-panel {
    display: none !important;
  }

  .pd-size-head,
  .pd-info__topline {
    align-items: flex-start;
    flex-direction: column;
  }

  .pd-tabs__head {
    flex-wrap: wrap;
  }

  .user-account-btn {
    flex: 1;
  }

  .pd-quick-view__card {
    grid-template-columns: 1fr;
  }
}
</style>


