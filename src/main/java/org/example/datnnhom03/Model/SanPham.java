package org.example.datnnhom03.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "SanPham")
public class SanPham {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // hiện tại bảng có idKhuyenMai nhưng chưa FK → tạm để Integer
    @Column(name = "idKhuyenMai")
    private Integer idKhuyenMai;

    @Column(name = "maSanPham")
    private String maSanPham;

    @Column(name = "tenSanPham")
    private String tenSanPham;

    @Column(name = "moTa", columnDefinition = "NVARCHAR(MAX)")
    private String moTa;

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

    @OneToMany(mappedBy = "sanPham")
    private List<SanPhamChiTiet> sanPhamChiTiets;
}
