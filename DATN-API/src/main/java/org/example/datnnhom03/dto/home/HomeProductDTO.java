package org.example.datnnhom03.dto.home;

import java.math.BigDecimal;

public class HomeProductDTO {

    private Integer id;
    private String maSanPham;
    private String tenSanPham;
    private String moTa;
    private Double giaBan;
    private BigDecimal giaTriKhuyenMai; // ← CHANGED HERE
    private Integer idDanhMuc;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMaSanPham() {
        return maSanPham;
    }

    public void setMaSanPham(String maSanPham) {
        this.maSanPham = maSanPham;
    }

    public String getTenSanPham() {
        return tenSanPham;
    }

    public void setTenSanPham(String tenSanPham) {
        this.tenSanPham = tenSanPham;
    }

    public String getMoTa() {
        return moTa;
    }

    public void setMoTa(String moTa) {
        this.moTa = moTa;
    }

    public Double getGiaBan() {
        return giaBan;
    }

    public void setGiaBan(Double giaBan) {
        this.giaBan = giaBan;
    }

    public BigDecimal getGiaTriKhuyenMai() {   // ← CHANGED
        return giaTriKhuyenMai;
    }

    public void setGiaTriKhuyenMai(BigDecimal giaTriKhuyenMai) {  // ← CHANGED
        this.giaTriKhuyenMai = giaTriKhuyenMai;
    }

    public Integer getIdDanhMuc() {
        return idDanhMuc;
    }

    public void setIdDanhMuc(Integer idDanhMuc) {
        this.idDanhMuc = idDanhMuc;
    }
}