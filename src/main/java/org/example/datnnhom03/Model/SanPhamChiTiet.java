package org.example.datnnhom03.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "SanPhamChiTiet")
public class SanPhamChiTiet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma")
    private String ma;

    @Column(name = "giaNhap")
    private BigDecimal giaNhap;

    @Column(name = "giaBan")
    private BigDecimal giaBan;

    @Column(name = "soLuong")
    private Integer soLuong;

    @ManyToOne
    @JoinColumn(name = "idSanPham")
    private SanPham sanPham;

    @ManyToOne
    @JoinColumn(name = "idChatLieu")
    private ChatLieu chatLieu;

    @ManyToOne
    @JoinColumn(name = "idMauSac")
    private MauSac mauSac;

    @ManyToOne
    @JoinColumn(name = "idKichThuoc")
    private KichThuoc kichThuoc;

    @ManyToOne
    @JoinColumn(name = "idHang")
    private Hang hang;

    @ManyToOne
    @JoinColumn(name = "idXuatSu")
    private XuatSu xuatSu;

    @ManyToOne
    @JoinColumn(name = "idDanhMuc")
    private DanhMuc danhMuc;

    @ManyToOne
    @JoinColumn(name = "idLoai")
    private Loai loai;

    @Column(name = "ngayTao")
    private LocalDateTime ngayTao;

    @Column(name = "ngaySua")
    private LocalDateTime ngaySua;

    @Column(name = "nguoiTao")
    private String nguoiTao;

    @Column(name = "nguoiSua")
    private String nguoiSua;

    @Column(name = "trangThai")
    private String trangThai;

    @Column(name = "barcode")
    private String barcode;
}
