package org.example.datnnhom03.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.datnnhom03.dto.PhuongThucThanhToanDTO;

@Getter
@Setter


public class PhuongThucThanhToanDTO {

    private Integer id;
    private String ma;
    private String ten;
    private String moTa;
    private String trangThai;

    public PhuongThucThanhToanDTO() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMa() {
        return ma;
    }

    public void setMa(String ma) {
        this.ma = ma;
    }

    public String getTen() {
        return ten;
    }

    public void setTen(String ten) {
        this.ten = ten;
    }

    public String getMoTa() {
        return moTa;
    }

    public void setMoTa(String moTa) {
        this.moTa = moTa;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }
}
