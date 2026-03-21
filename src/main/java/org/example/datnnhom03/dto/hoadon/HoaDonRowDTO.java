package org.example.datnnhom03.dto.hoadon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class HoaDonRowDTO {
    public Integer id;
    public String maHoaDon;

    public BigDecimal phiShip;
    public BigDecimal giaSauGiamGia;
    public BigDecimal thanhTien;

    public java.time.LocalDateTime ngayTao;
    public java.time.LocalDateTime ngayHuy;

    public Integer orderStatusId;
    public String orderStatusCode;
    public String orderStatusName;

    public Integer nhanVienId;
    public Integer khachHangId;
    public String tenNhanVien;
    public String tenKhachHang;

    public String soDienThoaiNhanHang;
    public String diaChiNhanHang;

    public java.time.LocalDate ngayNhanHangDuKien;
    public java.time.LocalDate ngayNhanHangMongMuon;

    public String phuongThucThanhToan;
    public java.time.LocalDateTime paidAt;
    public java.time.LocalDateTime cashCollectedAt;
    public String statusNote;
    public String orderType;
    public String paymentFlowCode;
    public String paymentFlowLabel;
    public String paymentFlowTone;

    // Two-dimensional order model
    public String fulfillmentStatusCode;
    public String fulfillmentStatusName;
    public String businessClosureStatus;
    public String businessClosureStatusName;
}
