package org.example.datnnhom03.dto.home;

public class HomeProductDTO {

    private Long id;
    private String maSanPham;
    private String tenSanPham;
    private String moTa;
    private Double giaBan;
    private Integer giaTriKhuyenMai;
    private Long idDanhMuc;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public Integer getGiaTriKhuyenMai() {
        return giaTriKhuyenMai;
    }

    public void setGiaTriKhuyenMai(Integer giaTriKhuyenMai) {
        this.giaTriKhuyenMai = giaTriKhuyenMai;
    }

    public Long getIdDanhMuc() {
        return idDanhMuc;
    }

    public void setIdDanhMuc(Long idDanhMuc) {
        this.idDanhMuc = idDanhMuc;
    }
}