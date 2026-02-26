import { createRouter, createWebHistory } from "vue-router";

import AdminLayout from "@/components/admin/AdminLayout.vue";
import AdminDashboard from "@/views/admin/dashboard/Dashboard.vue";
import DanhMuc from "@/views/admin/danhmuc/DanhMuc.vue";
import Loai from "@/views/admin/loai/Loai.vue";
import KichThuoc from "@/views/admin/kichthuoc/KichThuoc.vue";
import MauSac from "@/views/admin/mausac/MauSac.vue";
import KhuyenMai from "@/views/admin/khuyenmai/KhuyenMai.vue";
import KhachHang from "@/views/admin/khachhang/KhachHang.vue";
import NhanVien from "@/views/admin/nhanvien/NhanVien.vue";
import SanPham from "@/views/admin/sanpham/SanPham.vue";
import HoaDon from "@/views/admin/hoadon/HoaDon.vue";
import PhuongThucThanhToan from "@/views/admin/phuongthucthanhtoan/PhuongThucThanhToan.vue";
import TaiKhoan from "@/views/admin/taikhoan/TaiKhoan.vue";

import Login from "@/components/login/Login.vue";
import Home from "@/views/Home.vue";

const routes = [
  {
    path: "/",
    component: Login,
  },
  {
    path: "/home",
    component: Home,
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
      { path: "tai-khoan", component: TaiKhoan }
    ],
  },
];
  
const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;