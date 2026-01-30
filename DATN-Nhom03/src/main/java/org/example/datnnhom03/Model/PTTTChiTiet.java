package org.example.datnnhom03.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "PTTTChiTiet")
public class PTTTChiTiet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ====== Quan hệ ======

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idHoaDon")
    private HoaDon hoaDon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idPTTT")
    private PTTT phuongThucThanhToan;

    // ====== Thông tin ======

    @Column(name = "soTien")
    private Double soTien;

    @Column(name = "maGiaoDich")
    private String maGiaoDich;

    @Column(name = "trangThai")
    private String trangThai;

    @Column(name = "ngayTao")
    private LocalDateTime ngayTao;

    @Column(name = "ngaySua")
    private LocalDateTime ngaySua;
}