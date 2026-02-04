package org.example.datnnhom03.Model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "KhachHang",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "idTaiKhoan")
        }
)
public class KhachHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "idTaiKhoan", nullable = false)
    private TaiKhoan taiKhoan;

    @Column(name = "maKhachHang", nullable = false)
    private String maKhachHang;

    @Column(name = "tenKhachHang", nullable = false)
    private String tenKhachHang;

    @Column(name = "gioiTinh")
    private String gioiTinh;

    @Column(name = "ngaySinh")
    private LocalDate ngaySinh;

    @Column(name = "soDienThoai")
    private String soDienThoai;

    @Column(name = "trangThai")
    private String trangThai;
}
