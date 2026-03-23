import { createRouter, createWebHistory } from "vue-router"

const LoginManager = () => import("../views/auth/LoginManager.vue")
const CustomerLoginPage = () => import("../views/customer/pages/CustomerLoginPage.vue")
const Register = () => import("../views/auth/Register.vue")

const CustomerHomeView = () => import("../views/customer/HomeView.vue")
const CustomerProfileView = () => import("../views/customer/ProfileView.vue")
const CustomerProductsPage = () => import("../views/customer/pages/CustomerProductsPage.vue")
const CustomerAboutPage = () => import("../views/customer/pages/CustomerAboutPage.vue")
const CustomerNewsPage = () => import("../views/customer/pages/CustomerNewsPage.vue")
const CustomerContactPage = () => import("../views/customer/pages/CustomerContactPage.vue")
const CustomerCartPage = () => import("../views/customer/pages/CustomerCartPage.vue")
const OrderLookupPage = () => import("../views/customer/pages/OrderLookupPage.vue")
const ProductDetail = () => import("../views/customer/ProductDetail.vue")
const Checkout = () => import("../views/customer/Checkout.vue")
const VNPayTest = () => import("../views/customer/VNPayTest.vue")
const PaymentReturn = () => import("../views/PaymentReturn.vue")
const NotificationsPage = () => import("../views/shared/NotificationsPage.vue")

const EmployeeLayout = () => import("../views/employee/EmployeeLayout.vue")
const EmployeeGiaoCa = () => import("../views/employee/MoKetCaTest.vue")
const EmployeeGiaoCaTest = () => import("../views/employee/GiaoCaTest.vue")
const EmployeeDangKyDoiCa = () => import("../views/employee/DangKyDoiCa.vue")
const EmployeeProfileView = () => import("../views/employee/ProfileView.vue")
const EmployeeHome = () => import("../views/employee/EmployeeHome.vue")
const EmployeeProductList = () => import("../views/employee/san-pham/EmployeeProductList.vue")

const AdminLayout = () => import("../views/admin/AdminLayout/AdminLayout.vue")
const AdminHome = () => import("../views/admin/AdminHome.vue")

const AdminProductList = () => import("../views/admin/san-pham/ProductList.vue")
const AdminProductForm = () => import("../views/admin/san-pham/ProductForm.vue")
const BienTheSanPham = () => import("../views/admin/san-pham/BienTheSanPham.vue")

const DanhMucList = () => import("../views/admin/danh-muc/DanhMucList.vue")
const DanhMucForm = () => import("../views/admin/danh-muc/DanhMucForm.vue")

const HoaDonList = () => import("../views/admin/hoa-don/HoaDonList.vue")
const HoaDonDetail = () => import("../views/admin/hoa-don/HoaDonDetail.vue")
const POSCheckout = () => import("../views/admin/hoa-don/POSCheckout.vue")
const BanHangView = () => import("../views/admin/hoa-don/BanHangView.vue")

const KhachHangList = () => import("../views/admin/khach-hang/KhachHangList.vue")
const KhachHangForm = () => import("../views/admin/khach-hang/KhachHangForm.vue")

const KhuyenMaiList = () => import("../views/admin/khuyen-mai/KhuyenMaiList.vue")
const KhuyenMaiForm = () => import("../views/admin/khuyen-mai/KhuyenMaiForm.vue")
const VoucherForm = () => import("../views/admin/khuyen-mai/VoucherForm.vue")

const KichThuocList = () => import("../views/admin/kich-thuoc/KichThuocList.vue")
const KichThuocForm = () => import("../views/admin/kich-thuoc/KichThuocForm.vue")

const LoaiList = () => import("../views/admin/loai/LoaiList.vue")
const LoaiForm = () => import("../views/admin/loai/LoaiForm.vue")

const MauSacList = () => import("../views/admin/mau-sac/MauSacList.vue")
const MauSacForm = () => import("../views/admin/mau-sac/MauSacForm.vue")

const PhuongThucThanhToanList = () => import("../views/admin/phuong-thuc-thanh-toan/PhuongThucThanhToanList.vue")
const PhuongThucThanhToanForm = () => import("../views/admin/phuong-thuc-thanh-toan/PhuongThucThanhToanForm.vue")

const NhanVienList = () => import("../views/admin/nhan-vien/NhanVienList.vue")
const NhanVienForm = () => import("../views/admin/nhan-vien/NhanVienForm.vue")
const LichLamViec = () => import("../views/admin/lich-lam-viec/LichLamViec.vue")
const LichCaLam = () => import("../views/admin/lich-lam-viec/LichCaLam.vue")
const LichSuHoatDong = () => import("../views/admin/lich-lam-viec/LichSuHoatDong.vue")
const ThongKeDoanhThu = () => import("../views/admin/thong-ke/ThongKeDoanhThu.vue")
const AdminProfileView = () => import("../views/admin/profile/ProfileView.vue")

const routes = [
  { path: "/", redirect: "/trang-chu" },

  { path: "/auth/staff-login", component: LoginManager, alias: ["/dang-nhap", "/admin/login", "/employee/login", "/staff/login"] },
  { path: "/auth/customer-login", component: CustomerLoginPage, alias: ["/client/login", "/khach-hang/dang-nhap"] },
  { path: "/auth/customer-register", component: Register, alias: ["/client/register", "/khach-hang/dang-ky"] },
  { path: "/login", redirect: "/auth/customer-login" },
  { path: "/register", redirect: "/auth/customer-register" },

  { path: "/trang-chu", component: CustomerHomeView, alias: ["/dirtywave/trangchu", "/dirtywave/home"] },
  { path: "/home", redirect: "/trang-chu" },
  { path: "/san-pham", component: CustomerProductsPage },
  { path: "/gioi-thieu", component: CustomerAboutPage },
  { path: "/tin-tuc", component: CustomerNewsPage },
  { path: "/lien-he", component: CustomerContactPage },
  { path: "/gio-hang", component: CustomerCartPage },
  { path: "/tra-cuu-don-hang", component: OrderLookupPage, alias: ["/dirtywave/tra-cuu-don-hang"] },
  { path: "/customer/profile", component: CustomerProfileView, meta: { requiresCustomerAuth: true } },
  { path: "/customer/notifications", component: NotificationsPage },

  {
    path: "/product/:id",
    name: "product-detail",
    component: ProductDetail,
    props: true
  },

  {
    path: "/checkout",
    name: "checkout",
    component: Checkout,
    meta: { requiresCustomerAuth: true }
  },

  {
    path: "/vnpay-test",
    name: "vnpay-test",
    component: VNPayTest
  },

  {
    path: "/payment-return",
    name: "payment-return",
    component: PaymentReturn
  },

  /* ================= EMPLOYEE ================= */

  {
    path: "/employee",
    component: EmployeeLayout,
    children: [

      { path: "", redirect: "/employee/trang-chu" },
      { path: "trang-chu", component: EmployeeHome },
      { path: "dashboard", component: ThongKeDoanhThu },

      /* BAN HANG */

      { path: "ban-hang", component: BanHangView },

      /* HOA DON */

      { path: "hoa-don/list", component: HoaDonList },
      { path: "hoa-don/pos", component: POSCheckout },
      { path: "hoa-don/detail", component: HoaDonDetail },
      { path: "hoa-don/detail/:id", component: HoaDonDetail },

      /* SAN PHAM */

      { path: "san-pham/list", component: EmployeeProductList },
      { path: "san-pham/form", redirect: "/employee/san-pham/list" },
      { path: "san-pham/form/:id", redirect: "/employee/san-pham/list" },
      { path: "san-pham/bien-the", component: BienTheSanPham },

      /* KHUYEN MAI */

      { path: "khuyen-mai/list", component: KhuyenMaiList },
      { path: "khuyen-mai/form", component: KhuyenMaiForm },
      { path: "khuyen-mai/form/:id", component: KhuyenMaiForm },
      { path: "khuyen-mai/voucher/new", component: VoucherForm },
      { path: "khuyen-mai/voucher/:id", component: VoucherForm },

      /* GIAO CA */

      { path: "giao-ca", component: EmployeeGiaoCa },
      { path: "giao-ca-test", component: EmployeeGiaoCaTest },
      { path: "dang-ky-doi-ca", component: EmployeeDangKyDoiCa },

      /* PROFILE */

      { path: "profile", component: EmployeeProfileView },
      { path: "notifications", component: NotificationsPage }

    ]
  },

  /* ================= ADMIN ================= */

  {
    path: "/admin",
    component: AdminLayout,
    children: [

      { path: "", redirect: "/admin/trang-chu" },
      { path: "trang-chu", component: AdminHome },

      /* SAN PHAM */

      { path: "san-pham/list", component: AdminProductList },
      { path: "san-pham/form", component: AdminProductForm },
      { path: "san-pham/form/:id", component: AdminProductForm },
      { path: "san-pham/bien-the", component: BienTheSanPham },

      /* BAN HANG */

      { path: "ban-hang", component: BanHangView },

      /* HOA DON */

      { path: "hoa-don/list", component: HoaDonList },
      { path: "hoa-don/pos", component: POSCheckout },
      { path: "hoa-don/detail/create", component: HoaDonDetail },
      { path: "hoa-don/detail/:id", component: HoaDonDetail },
      { path: "hoa-don/detail", component: HoaDonDetail },

      /* DANH MUC */

      { path: "danh-muc/list", component: DanhMucList },
      { path: "danh-muc/form", component: DanhMucForm },
      { path: "danh-muc/form/:id", component: DanhMucForm },

      /* KHACH HANG */

      { path: "khach-hang/list", component: KhachHangList },
      { path: "khach-hang/form", component: KhachHangForm },
      { path: "khach-hang/form/:id", component: KhachHangForm },

      /* KHUYEN MAI */

      { path: "khuyen-mai/list", component: KhuyenMaiList },
      { path: "khuyen-mai/form", component: KhuyenMaiForm },
      { path: "khuyen-mai/form/:id", component: KhuyenMaiForm },
      { path: "khuyen-mai/voucher/new", component: VoucherForm },
      { path: "khuyen-mai/voucher/:id", component: VoucherForm },

      /* SIZE */

      { path: "kich-thuoc/list", component: KichThuocList },
      { path: "kich-thuoc/form", component: KichThuocForm },
      { path: "kich-thuoc/form/:id", component: KichThuocForm },

      /* LOAI */

      { path: "loai/list", component: LoaiList },
      { path: "loai/form", component: LoaiForm },
      { path: "loai/form/:id", component: LoaiForm },

      /* MAU SAC */

      { path: "mau-sac/list", component: MauSacList },
      { path: "mau-sac/form", component: MauSacForm },
      { path: "mau-sac/form/:id", component: MauSacForm },

      /* PTTT */

      { path: "phuong-thuc-thanh-toan/list", component: PhuongThucThanhToanList },
      { path: "phuong-thuc-thanh-toan/form", component: PhuongThucThanhToanForm },
      { path: "phuong-thuc-thanh-toan/form/:id", component: PhuongThucThanhToanForm },

      /* NHAN VIEN */

      { path: "nhan-vien/list", component: NhanVienList },
      { path: "nhan-vien/form", component: NhanVienForm },
      { path: "nhan-vien/form/:id", component: NhanVienForm },

      /* LICH LAM VIEC */

      {
        path: "lich-lam-viec/lich-lam-viec",
        alias: ["lich-lam-viec", "lichlamviec"],
        component: LichLamViec
      },
      {
        path: "lich-lam-viec/ca-lam",
        alias: ["lich-lam-viec/lich-ca-lam", "lich-calam"],
        component: LichCaLam
      },
      {
        path: "lich-lam-viec/su-hoat-dong",
        alias: ["lich-lam-viec/lich-su-hoat-dong", "lich-su-hoat"],
        component: LichSuHoatDong
      },

      /* THONG KE */

      { path: "thong-ke/doanh-thu", component: ThongKeDoanhThu },

      /* PROFILE */

      { path: "profile", component: AdminProfileView },
      { path: "notifications", component: NotificationsPage }

    ]
  },

  { path: "/:pathMatch(.*)*", redirect: "/trang-chu" }
]

const normalizeRole = (role) => String(role || "").trim().toUpperCase().replace(/^ROLE_/, "")

const isCustomerRole = (role) => {
  const normalized = normalizeRole(role)
  return normalized === "CUSTOMER" || normalized === "KHACH_HANG" || normalized === "KHACHHANG" || normalized === "USER"
}

const isEmployeeRole = (role) => {
  const normalized = normalizeRole(role)
  return normalized === "EMPLOYEE" || normalized === "NHAN_VIEN" || normalized === "NHANVIEN"
}

const isAdminRole = (role) => normalizeRole(role) === "ADMIN"

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const role = localStorage.getItem("role")
  const isAdminArea = to.path === "/admin" || to.path.startsWith("/admin/")
  const isEmployeeArea = to.path === "/employee" || to.path.startsWith("/employee/")
  const isInternalLogin = ["/auth/staff-login", "/dang-nhap", "/admin/login", "/employee/login", "/staff/login"].includes(to.path)
  const isClientAuth = [
    "/auth/customer-login",
    "/auth/customer-register",
    "/client/login",
    "/client/register",
    "/khach-hang/dang-nhap",
    "/khach-hang/dang-ky"
  ].includes(to.path)

  if (isAdminArea && !isAdminRole(role)) return "/auth/staff-login"
  if (isEmployeeArea && !isEmployeeRole(role)) return "/auth/staff-login"

  if (isInternalLogin) {
    if (isAdminRole(role)) return "/admin/trang-chu"
    if (isEmployeeRole(role)) return "/employee/trang-chu"
  }

  if (isClientAuth) {
    if (isCustomerRole(role)) return "/trang-chu"
    return true
  }

  // Protect routes that require a logged-in customer
  if (to.meta?.requiresCustomerAuth && !isCustomerRole(role)) {
    return { path: '/auth/customer-login', query: { redirect: to.fullPath } }
  }

  return true
})

export default router