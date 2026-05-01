package org.example.datnnhom03.Model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "LichLamViec")
public class LichLamViec {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer idNhanVien;

    private Integer idCaLam;

    private LocalDate ngayLam;

    private String trangThai;
}