package org.example.datnnhom03.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "NhanVien")
public class NhanVien {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "idTaiKhoan")
    private TaiKhoan taiKhoan;

    @ManyToOne
    @JoinColumn(name = "idChucVu")
    private ChucVu chucVu;

    @Column(name = "maNhanVien")
    private String maNhanVien;

    @Column(name = "tenNhanVien")
    private String tenNhanVien;

    @Column(name = "gioiTinh")
    private String gioiTinh;

    @Column(name = "ngaySinh")
    private LocalDate ngaySinh;

    @Column(name = "diaChi")
    private String diaChi;

    @Column(name = "soDienThoai")
    private String soDienThoai;

    @Column(name = "trangThai")
    private String trangThai;

    @Column(name = "anh")
    private String anh;
}
