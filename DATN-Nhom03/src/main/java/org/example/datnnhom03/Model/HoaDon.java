package org.example.datnnhom03.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Column(name = "phiShip")
    private Double phiShip;

    @Column(name = "giaSauGiamGia")
    private Double giaSauGiamGia;

    @Column(name = "thanhTien")
    private Double thanhTien;

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

    @Column(name = "trangThai")
    private String trangThai;
}
