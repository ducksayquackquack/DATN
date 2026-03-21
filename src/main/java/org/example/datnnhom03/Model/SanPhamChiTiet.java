package org.example.datnnhom03.Model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "SanPhamChiTiet")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SanPhamChiTiet {

    //
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idSanPham")
    @JsonBackReference
    private SanPham sanPham;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idChatLieu")
    private ChatLieu chatLieu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idMauSac")
    private MauSac mauSac;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idKichThuoc")
    private KichThuoc kichThuoc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idHang")
    private Hang hang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idXuatSu")
    private XuatSu xuatSu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idDanhMuc")
    private DanhMuc danhMuc;

    @ManyToOne(fetch = FetchType.LAZY)
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

    @Column(name = "hinhAnh")
    private String hinhAnh;
}