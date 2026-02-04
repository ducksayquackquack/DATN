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
        name = "NhanVien",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "idTaiKhoan")
        }
)
public class NhanVien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "idTaiKhoan", nullable = false)
    private TaiKhoan taiKhoan;

    @ManyToOne
    @JoinColumn(name = "idChucVu", nullable = false)
    private ChucVu chucVu;

    @Column(name = "maNhanVien", nullable = false)
    private String maNhanVien;

    @Column(name = "tenNhanVien", nullable = false)
    private String tenNhanVien;

    @Column(name = "gioiTinh")
    private String gioiTinh;

    @Column(name = "ngaySinh")
    private LocalDate ngaySinh;

    @Column(name = "diaChi")
    private String diaChi;

    @Column(name = "soDienThoai")
    private String soDienThoai;

    @Column(name = "trangThaiHoatDong")
    private String trangThaiHoatDong;

    @Column(name = "anh")
    private String anh;
}
