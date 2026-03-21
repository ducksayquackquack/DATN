package org.example.datnnhom03.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "HoaDon")
public class HoaDon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idNhanVien")
    private NhanVien nhanVien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idKhachHang")
    private KhachHang khachHang;

    @Column(name = "maHoaDon")
    private String maHoaDon;

    @Column(name = "tenKhachHang")
    private String tenKhachHang;

    @Column(name = "soDienThoaiNhanHang")
    private String soDienThoaiNhanHang;

    @Column(name = "diaChiNhanHang")
    private String diaChiNhanHang;

    @Column(name = "ngayNhanHangDuKien")
    private LocalDate ngayNhanHangDuKien;

    @Column(name = "ngayNhanHangMongMuon")
    private LocalDate ngayNhanHangMongMuon;

    @Column(name = "ngayTao")
    private LocalDateTime ngayTao;

    @Column(name = "ngayHuy")
    private LocalDateTime ngayHuy;

    @Column(name = "phiShip")
    private BigDecimal phiShip;

    @Column(name = "giaSauGiamGia")
    private BigDecimal giaSauGiamGia;

    @Column(name = "thanhTien")
    private BigDecimal thanhTien;

    @Column(name = "OrderStatusId")
    private Integer orderStatusId;

    // nếu DB có trangThai thì giữ:
    @Column(name = "trangThai")
    private String trangThai;

    @Column(name = "phuongThucThanhToan")
    private String phuongThucThanhToan;

    @Column(name = "paidAt")
    private LocalDateTime paidAt;

    @Column(name = "cashCollectedAt")
    private LocalDateTime cashCollectedAt;

    @Column(name = "statusNote", columnDefinition = "NVARCHAR(MAX)")
    private String statusNote;
}
