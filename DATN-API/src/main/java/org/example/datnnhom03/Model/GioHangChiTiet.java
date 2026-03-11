package org.example.datnnhom03.Model;


import jakarta.persistence.*;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "GioHangChiTiet")
public class GioHangChiTiet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "idGioHang")
    private GioHang gioHang;

    @ManyToOne
    @JoinColumn(name = "idSanPhamChiTiet")
    private SanPhamChiTiet sanPhamChiTiet;

    @Column(name = "soLuong")
    private Integer soLuong;

    @Column(name = "ngayTao")
    private LocalDateTime ngayTao;

    @Column(name = "trangThai")
    private String trangThai;
}
