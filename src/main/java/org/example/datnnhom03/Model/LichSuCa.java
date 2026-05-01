package org.example.datnnhom03.Model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "LichSuCa")
public class LichSuCa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer idNhanVien;

    private Integer idCaLam;

    private LocalDate ngay;

    private BigDecimal tienCa;

    private BigDecimal tienDauCa;

    private BigDecimal doanhThu;

    private BigDecimal tienChuyenKhoan;

    private String ghiChu;

    private String trangThai;
}