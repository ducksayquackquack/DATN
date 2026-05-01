package org.example.datnnhom03.dto;

import lombok.Data;

@Data
public class TaiKhoanDTO {
    private Integer id;
    private String vaiTro;
    private String email;
    private String matKhau;
    private String password;
    private String username;
    private String tenDangNhap;
    private String avatar;
    private String trangThaiHoatDong;
    private String trangThaiTaiKhoan;

    // Optional customer profile info for one-step registration.
    private String tenKhachHang;
    private String gioiTinh;
    private String ngaySinh;
    private String soDienThoai;

    // Optional default address info for one-step registration.
    private String diaChiCuThe;
    private String tinhThanh;
    private String quanHuyen;
    private String phuongXa;
}