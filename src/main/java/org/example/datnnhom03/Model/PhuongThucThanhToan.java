package org.example.datnnhom03.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "PhuongThucThanhToan")
@Getter
@Setter
public class PhuongThucThanhToan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String ma;
    private String ten;
    private String moTa;
    private String trangThai;

    private LocalDateTime ngayTao;
    private LocalDateTime ngaySua;
}

