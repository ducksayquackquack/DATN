import { createRouter, createWebHistory } from "vue-router"

import Login from "../views/auth/Login.vue"
import CustomerHomeView from "../views/customer/HomeView.vue"
import ProductDetail from "../views/customer/ProductDetail.vue"

import EmployeeDashboardView from "../views/employee/DashboardView.vue"
import EmployeeLayout from "../views/employee/EmployeeLayout.vue"

import AdminLayout from "../views/admin/AdminLayout/AdminLayout.vue"

import AdminProductList from "../views/admin/san-pham/ProductList.vue"
import AdminProductForm from "../views/admin/san-pham/ProductForm.vue"

import DanhMucList from "../views/admin/danh-muc/DanhMucList.vue"
import DanhMucForm from "../views/admin/danh-muc/DanhMucForm.vue"

import HoaDonList from "../views/admin/hoa-don/HoaDonList.vue"
import HoaDonDetail from "../views/admin/hoa-don/HoaDonDetail.vue"

import KhachHangList from "../views/admin/khach-hang/KhachHangList.vue"
import KhachHangForm from "../views/admin/khach-hang/KhachHangForm.vue"

import KhuyenMaiList from "../views/admin/khuyen-mai/KhuyenMaiList.vue"
import KhuyenMaiForm from "../views/admin/khuyen-mai/KhuyenMaiForm.vue"

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

const routes = [
  { path: "/", redirect: "/login" },

  { path: "/login", component: Login },

  { path: "/home", component: CustomerHomeView },

  {
    path: "/product/:id",
    name: "product-detail",
    component: ProductDetail,
    props: true
  },

  /* ================= EMPLOYEE ================= */

  {
    path: "/employee",
    component: EmployeeLayout,
    children: [

      { path: "dashboard", component: EmployeeDashboardView },

      /* HOA DON */

      { path: "hoa-don/list", component: HoaDonList },
      { path: "hoa-don/detail", component: HoaDonDetail },
      { path: "hoa-don/detail/:id", component: HoaDonDetail },

      /* SAN PHAM */

      { path: "san-pham/list", component: AdminProductList },
      { path: "san-pham/form", component: AdminProductForm },
      { path: "san-pham/form/:id", component: AdminProductForm },

      /* KHUYEN MAI */

      { path: "khuyen-mai/list", component: KhuyenMaiList },
      { path: "khuyen-mai/form", component: KhuyenMaiForm },
      { path: "khuyen-mai/form/:id", component: KhuyenMaiForm }

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
      { path: "hoa-don/detail", component: HoaDonDetail },
      { path: "hoa-don/detail/:id", component: HoaDonDetail },

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
      { path: "nhan-vien/form/:id", component: NhanVienForm }

    ]
  },

  { path: "/:pathMatch(.*)*", redirect: "/home" }
]

export default createRouter({
  history: createWebHistory(),
  routes
})