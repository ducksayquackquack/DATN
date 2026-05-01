package org.example.datnnhom03.Model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "PhieuGiamGia")
public class PhieuGiamGia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "maPhieuGiamGia")
    private String maPhieuGiamGia;

    @Column(name = "tenPhieuGiamGia")
    private String tenPhieuGiamGia;

    // true = cá nhân | false = công khai
    @Column(name = "loaiPhieuGiamGia")
    private Boolean loaiPhieuGiamGia;

    // true = giảm % | false = giảm tiền
    @Column(name = "hinhThucGiam")
    private Boolean hinhThucGiam;

    @Column(name = "giaTriGiamGia")
    private BigDecimal giaTriGiamGia;

    @Column(name = "hoaDonToiThieu")
    private BigDecimal hoaDonToiThieu;

    @Column(name = "soLuongSuDung")
    private Integer soLuongSuDung;

    @Column(name = "ngayBatDau")
    private LocalDate ngayBatDau;

    @Column(name = "ngayKetThuc")
    private LocalDate ngayKetThuc;

    @Column(name = "trangThai")
    private String trangThai;
}