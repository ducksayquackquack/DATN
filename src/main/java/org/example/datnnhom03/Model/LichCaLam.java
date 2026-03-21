package org.example.datnnhom03.Model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "CaLam")
public class LichCaLam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String tenCa;

    private String moTa;

    private LocalTime gioBatDau;

    private LocalTime gioKetThuc;

    private String trangThai;
}