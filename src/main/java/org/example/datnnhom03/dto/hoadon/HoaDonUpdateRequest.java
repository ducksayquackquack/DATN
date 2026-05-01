package org.example.datnnhom03.dto.hoadon;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class HoaDonUpdateRequest {
    public Integer nhanVienId;
    public Integer khachHangId;

    public String soDienThoaiNhanHang;
    public String diaChiNhanHang;

    public LocalDate ngayNhanHangDuKien;
    public LocalDate ngayNhanHangMongMuon;

    public BigDecimal phiShip;
    public BigDecimal giaSauGiamGia;
    public BigDecimal thanhTien;
    public String orderStatusCode;
    public String statusNote;
    public String phuongThucThanhToan;
    public LocalDateTime paidAt;
    public LocalDateTime cashCollectedAt;
    public String orderType;
}
