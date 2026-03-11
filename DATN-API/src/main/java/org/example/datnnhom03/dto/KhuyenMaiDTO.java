package org.example.datnnhom03.dto;

import java.time.LocalDate;

public class KhuyenMaiDTO {

    private Integer id;
    private String maKhuyenMai;
    private String tenKhuyenMai;

    private Integer giaTriGiam;   // map với entity.giaTri
    private LocalDate ngayBatDau;
    private LocalDate ngayKetThuc;

    private String trangThai;

    public KhuyenMaiDTO() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMaKhuyenMai() {
        return maKhuyenMai;
    }

    public void setMaKhuyenMai(String maKhuyenMai) {
        this.maKhuyenMai = maKhuyenMai;
    }

    public String getTenKhuyenMai() {
        return tenKhuyenMai;
    }

    public void setTenKhuyenMai(String tenKhuyenMai) {
        this.tenKhuyenMai = tenKhuyenMai;
    }

    public Integer getGiaTriGiam() {
        return giaTriGiam;
    }

    public void setGiaTriGiam(Integer giaTriGiam) {
        this.giaTriGiam = giaTriGiam;
    }

    public LocalDate getNgayBatDau() {
        return ngayBatDau;
    }

    public void setNgayBatDau(LocalDate ngayBatDau) {
        this.ngayBatDau = ngayBatDau;
    }

    public LocalDate getNgayKetThuc() {
        return ngayKetThuc;
    }

    public void setNgayKetThuc(LocalDate ngayKetThuc) {
        this.ngayKetThuc = ngayKetThuc;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }
}
