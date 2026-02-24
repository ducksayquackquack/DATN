package org.example.datnnhom03.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "DiaChi")
public class DiaChi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "idKhachHang")
    private KhachHang khachHang;

    @Column(name = "diaChiCuThe")
    private String diaChiCuThe;

    @Column(name = "tinhThanh")
    private String tinhThanh;

    @Column(name = "quanHuyen")
    private String quanHuyen;

    @Column(name = "phuongXa")
    private String phuongXa;

    @Column(name = "trangThai")
    private String trangThai;

}
