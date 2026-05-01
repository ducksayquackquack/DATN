package org.example.datnnhom03.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class LichLamViecDTO {

    private Integer id;

    private Integer idNhanVien;
    private String tenNhanVien;

    private Integer idCaLam;
    private String tenCa;

    private LocalTime gioBatDau;
    private LocalTime gioKetThuc;

    private LocalDate ngayLam;

    private String trangThai;

    public LichLamViecDTO(
            Integer id,
            Integer idNhanVien,
            String tenNhanVien,
            Integer idCaLam,
            String tenCa,
            LocalTime gioBatDau,
            LocalTime gioKetThuc,
            LocalDate ngayLam,
            String trangThai
    ) {
        this.id = id;
        this.idNhanVien = idNhanVien;
        this.tenNhanVien = tenNhanVien;
        this.idCaLam = idCaLam;
        this.tenCa = tenCa;
        this.gioBatDau = gioBatDau;
        this.gioKetThuc = gioKetThuc;
        this.ngayLam = ngayLam;
        this.trangThai = trangThai;
    }
}