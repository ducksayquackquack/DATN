import { createRouter, createWebHistory } from "vue-router"

import Login from "../views/auth/Login.vue"
import Register from "../views/auth/Register.vue"
import CustomerHomeView from "../views/customer/HomeView.vue"
import CustomerProfileView from "../views/customer/ProfileView.vue"
import CustomerProductsPage from "../views/customer/pages/CustomerProductsPage.vue"
import CustomerAboutPage from "../views/customer/pages/CustomerAboutPage.vue"
import CustomerNewsPage from "../views/customer/pages/CustomerNewsPage.vue"
import CustomerContactPage from "../views/customer/pages/CustomerContactPage.vue"
import CustomerCartPage from "../views/customer/pages/CustomerCartPage.vue"
import ProductDetail from "../views/customer/ProductDetail.vue"
import Checkout from "../views/customer/Checkout.vue"
import VNPayTest from "../views/customer/VNPayTest.vue"
import PaymentReturn from "../views/PaymentReturn.vue"
import NotificationsPage from "../views/shared/NotificationsPage.vue"

import EmployeeLayout from "../views/employee/EmployeeLayout.vue"
import EmployeeGiaoCa from "../views/employee/MoKetCaTest.vue"
import EmployeeDangKyDoiCa from "../views/employee/DangKyDoiCa.vue"
import EmployeeProfileView from "../views/employee/ProfileView.vue"

import AdminLayout from "../views/admin/AdminLayout/AdminLayout.vue"

import AdminProductList from "../views/admin/san-pham/ProductList.vue"
import AdminProductForm from "../views/admin/san-pham/ProductForm.vue"

import DanhMucList from "../views/admin/danh-muc/DanhMucList.vue"
import DanhMucForm from "../views/admin/danh-muc/DanhMucForm.vue"

import HoaDonList from "../views/admin/hoa-don/HoaDonList.vue"
import HoaDonDetail from "../views/admin/hoa-don/HoaDonDetail.vue"
import POSCheckout from "../views/admin/hoa-don/POSCheckout.vue"

import KhachHangList from "../views/admin/khach-hang/KhachHangList.vue"
import KhachHangForm from "../views/admin/khach-hang/KhachHangForm.vue"

import KhuyenMaiList from "../views/admin/khuyen-mai/KhuyenMaiList.vue"
import KhuyenMaiForm from "../views/admin/khuyen-mai/KhuyenMaiForm.vue"
import VoucherForm from "../views/admin/khuyen-mai/VoucherForm.vue"

import KichThuocList from "../views/admin/kich-thuoc/KichThuocList.vue"
import KichThuocForm from "../views/admin/kich-thuoc/KichThuocForm.vue"

import LoaiList from "../views/admin/loai/LoaiList.vue"
import LoaiForm from "../views/admin/loai/LoaiForm.vue"

import MauSacList from "../views/admin/mau-sac/MauSacList.vue"
import MauSacForm from "../views/admin/mau-sac/MauSacForm.vue"

import PhuongThucThanhToanList from "../views/admin/phuong-thuc-thanh-toan/PhuongThucThanhToanList.vue"
import PhuongThucThanhToanForm from "../views/admin/phuong-thuc-thanh-toan/PhuongThucThanhToanForm.vue"

import NhanVienList from "../views/admin/nhan-vien/NhanVienList.vue"
import NhanVienForm from "../views/admin/nhan-vien/NhanVienForm.vue"
import LichLamViec from "../views/admin/lich-lam-viec/LichLamViec.vue"
import LichCaLam from "../views/admin/lich-lam-viec/LichCaLam.vue"
import LichSuHoatDong from "../views/admin/lich-lam-viec/LichSuHoatDong.vue"
import ThongKeDoanhThu from "../views/admin/thong-ke/ThongKeDoanhThu.vue"
import AdminProfileView from "../views/admin/profile/ProfileView.vue"

const routes = [
  { path: "/", redirect: "/login" },

  { path: "/login", component: Login },
  { path: "/register", component: Register },

  { path: "/home", component: CustomerHomeView },
  { path: "/san-pham", component: CustomerProductsPage },
  { path: "/gioi-thieu", component: CustomerAboutPage },
  { path: "/tin-tuc", component: CustomerNewsPage },
  { path: "/lien-he", component: CustomerContactPage },
  { path: "/gio-hang", component: CustomerCartPage },
  { path: "/customer/profile", component: CustomerProfileView },
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
    component: Checkout
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

      { path: "", redirect: "/employee/dashboard" },
      { path: "dashboard", component: ThongKeDoanhThu },

      /* HOA DON */

      { path: "hoa-don/list", component: HoaDonList },
      { path: "hoa-don/pos", component: POSCheckout },
      { path: "hoa-don/detail", component: HoaDonDetail },
      { path: "hoa-don/detail/:id", component: HoaDonDetail },

      /* SAN PHAM */

      { path: "san-pham/list", component: AdminProductList },
      { path: "san-pham/form", component: AdminProductForm },
      { path: "san-pham/form/:id", component: AdminProductForm },

      /* KHUYEN MAI */

      { path: "khuyen-mai/list", component: KhuyenMaiList },
      { path: "khuyen-mai/form", component: KhuyenMaiForm },
      { path: "khuyen-mai/form/:id", component: KhuyenMaiForm },
      { path: "khuyen-mai/voucher/new", component: VoucherForm },
      { path: "khuyen-mai/voucher/:id", component: VoucherForm },

      /* GIAO CA */

      { path: "giao-ca", component: EmployeeGiaoCa },
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

      { path: "", redirect: "/admin/san-pham/list" },

      /* SAN PHAM */

      { path: "san-pham/list", component: AdminProductList },
      { path: "san-pham/form", component: AdminProductForm },
      { path: "san-pham/form/:id", component: AdminProductForm },

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

  { path: "/:pathMatch(.*)*", redirect: "/home" }
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

const customerPrefixes = [
  "/home",
  "/san-pham",
  "/gioi-thieu",
  "/tin-tuc",
  "/lien-he",
  "/gio-hang",
  "/product",
  "/checkout",
  "/vnpay-test",
  "/payment-return",
  "/customer"
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const role = localStorage.getItem("role")
  const isCustomerArea = customerPrefixes.some((prefix) => to.path === prefix || to.path.startsWith(`${prefix}/`))

  if (!isCustomerArea) return true

  if (isAdminRole(role)) return "/admin"
  if (isEmployeeRole(role)) return "/employee"

  return true
})

export default router