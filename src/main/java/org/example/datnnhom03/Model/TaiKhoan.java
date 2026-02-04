package org.example.datnnhom03.Model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "TaiKhoan",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "email")
        }
)
public class TaiKhoan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "vaiTro", nullable = false)
    private String vaiTro; // ADMIN, EMPLOYEE, CUSTOMER

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "matKhau", nullable = false)
    private String matKhau;

    @Column(name = "avatar")
    private String avatar;

    @Column(name = "trangThaiHoatDong")
    private String trangThaiHoatDong;

    @Column(name = "trangThaiTaiKhoan", nullable = false)
    private String trangThaiTaiKhoan; // Kích hoạt / Khóa
}
