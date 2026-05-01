package org.example.datnnhom03.Mapper;

import org.example.datnnhom03.Model.HoaDon;
import org.example.datnnhom03.Model.HoaDonChiTiet;
import org.example.datnnhom03.dto.hoadon.HoaDonItemDTO;
import org.example.datnnhom03.dto.hoadon.HoaDonRowDTO;

import java.math.BigDecimal;

public class HoaDonMapper {

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    // Map HoaDon -> RowDTO (dùng cho list + header detail)
    public static HoaDonRowDTO toRow(HoaDon hd, String statusCode, String statusName) {
        HoaDonRowDTO dto = new HoaDonRowDTO();

        dto.id = hd.getId();
        dto.maHoaDon = hd.getMaHoaDon();

        dto.phiShip = nz(hd.getPhiShip());
        dto.giaSauGiamGia = nz(hd.getGiaSauGiamGia());
        dto.thanhTien = nz(hd.getThanhTien());

        dto.ngayTao = hd.getNgayTao();
        dto.ngayHuy = hd.getNgayHuy();

        dto.orderStatusId = hd.getOrderStatusId();
        dto.orderStatusCode = statusCode;
        dto.orderStatusName = statusName;

        // tên NV/KH: nếu entity có quan hệ lazy thì có thể null
        dto.nhanVienId = (hd.getNhanVien() != null ? hd.getNhanVien().getId() : null);
        dto.khachHangId = (hd.getKhachHang() != null ? hd.getKhachHang().getId() : null);

        dto.tenNhanVien = (hd.getNhanVien() != null ? hd.getNhanVien().getTenNhanVien() : null);
        dto.tenKhachHang = (hd.getKhachHang() != null ? hd.getKhachHang().getTenKhachHang() : null);

        dto.soDienThoaiNhanHang = hd.getSoDienThoaiNhanHang();
        dto.diaChiNhanHang = hd.getDiaChiNhanHang();

        dto.ngayNhanHangDuKien = hd.getNgayNhanHangDuKien();
        dto.ngayNhanHangMongMuon = hd.getNgayNhanHangMongMuon();

        dto.phuongThucThanhToan = hd.getPhuongThucThanhToan();
        dto.paidAt = hd.getPaidAt();
        dto.cashCollectedAt = hd.getCashCollectedAt();
        dto.statusNote = hd.getStatusNote();
        dto.orderType = (hd.getStatusNote() != null && hd.getStatusNote().toUpperCase().contains("[POS]")) ? "POS" : "ONLINE";

        return dto;
    }

    // Map HoaDonChiTiet -> ItemDTO
    public static HoaDonItemDTO toItem(HoaDonChiTiet it) {
        HoaDonItemDTO dto = new HoaDonItemDTO();

        dto.id = it.getId();
        dto.soLuong = it.getSoLuong();
        dto.thanhTien = it.getThanhTien() == null ? BigDecimal.ZERO : it.getThanhTien();
        dto.trangThai = it.getTrangThai();

        if (it.getSanPhamChiTiet() != null) {
            dto.spctId = it.getSanPhamChiTiet().getId();
            dto.giaBan = (it.getSanPhamChiTiet().getGiaBan() != null ? it.getSanPhamChiTiet().getGiaBan() : BigDecimal.ZERO);
            dto.maSanPhamChiTiet = it.getSanPhamChiTiet().getMa();
            dto.maSanPham = (it.getSanPhamChiTiet().getSanPham() != null) ? it.getSanPhamChiTiet().getSanPham().getMaSanPham() : null;

            // Optional hiển thị thêm (nếu DTO có field)
            // dto.tenSanPham = it.getSanPhamChiTiet().getSanPham() != null ? it.getSanPhamChiTiet().getSanPham().getTenSanPham() : null;
            // dto.tenMau = it.getSanPhamChiTiet().getMauSac() != null ? it.getSanPhamChiTiet().getMauSac().getTenMau() : null;
            // dto.tenKichThuoc = it.getSanPhamChiTiet().getKichThuoc() != null ? it.getSanPhamChiTiet().getKichThuoc().getTenKichThuoc() : null;
        }

        return dto;
    }

    private static BigDecimal toBigDecimal(Double v) {
        return v == null ? BigDecimal.ZERO : BigDecimal.valueOf(v);
    }
}
