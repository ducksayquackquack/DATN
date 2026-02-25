import { createRouter, createWebHistory } from "vue-router";

import AdminLayout from "@/components/admin/AdminLayout.vue";
import AdminDashboard from "@/views/admin/AdminDashboard.vue";
import DanhMuc from "@/views/admin/DanhMuc.vue";
import Loai from "@/views/admin/Loai.vue";
import KichThuoc from "@/views/admin/KichThuoc.vue";
import MauSac from "@/views/admin/MauSac.vue";
import KhuyenMai from "@/views/admin/KhuyenMai.vue";
import KhachHang from "@/views/admin/KhachHang.vue";
import NhanVien from "@/views/admin/NhanVien.vue";
import SanPham from "@/views/admin/SanPham.vue";
import HoaDon from "@/views/admin/HoaDon.vue";
import PhuongThucThanhToan from "@/views/admin/PhuongThucThanhToan.vue";
import TaiKhoan from "@/views/admin/TaiKhoan.vue";

import AdminLogin from "@/components/admin/AdminLogin.vue";
import Home from "@/views/Home.vue";

const routes = [
  {
    path: "/",
    component: AdminLogin, // 🔥 login page
  },

  {
    path: "/home",
    component: Home, // 🔥 customer home
  },

  {
    path: "/admin",
    component: AdminLayout,
    redirect: "/admin/dashboard",
    children: [
      { path: "dashboard", component: AdminDashboard },
      { path: "san-pham", component: SanPham },
      { path: "danh-muc", component: DanhMuc },
      { path: "loai", component: Loai },
      { path: "kich-thuoc", component: KichThuoc },
      { path: "mau-sac", component: MauSac },
      { path: "khuyen-mai", component: KhuyenMai },
      { path: "hoa-don", component: HoaDon },
      { path: "phuong-thuc-thanh-toan", redirect: "/admin/pttt" },
      { path: "pttt", component: PhuongThucThanhToan },
      { path: "khach-hang", component: KhachHang },
      { path: "nhan-vien", component: NhanVien },
      { path: "tai-khoan", component: TaiKhoan },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;