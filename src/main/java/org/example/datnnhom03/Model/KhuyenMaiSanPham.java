package org.example.datnnhom03.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "KhuyenMaiSanPham")
public class KhuyenMaiSanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idKhuyenMai", nullable = false)
    private KhuyenMai khuyenMai;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idSanPham", nullable = false)
    private SanPham sanPham;
}