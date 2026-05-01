package org.example.datnnhom03.dto.hoadon;

import java.math.BigDecimal;

public class HoaDonItemDTO {
    public Integer id;          // ✅ thêm để mapper dto.id = it.getId() không lỗi
    public Integer spctId;
    public String maSanPham;
    public String maSanPhamChiTiet;
    public Integer soLuong;
    public BigDecimal giaBan;   // (nếu anh dùng)
    public BigDecimal thanhTien; // ✅ đổi sang BigDecimal
    public String trangThai;
}