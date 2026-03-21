package org.example.datnnhom03.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "KhachHang")
public class KhachHang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "idTaiKhoan")
    private TaiKhoan taiKhoan;

    @Column(name = "maKhachHang")
    private String maKhachHang;

    @Column(name = "tenKhachHang")
    private String tenKhachHang;

    @Column(name = "gioiTinh")
    private String gioiTinh;

    @Column(name = "ngaySinh")
    private LocalDate ngaySinh;

    @Column(name = "soDienThoai")
    private String soDienThoai;

    @Column(name = "trangThai")
    private String trangThai;

    @JsonProperty("idTaiKhoan")
    public void setIdTaiKhoan(Integer idTaiKhoan) {
        if (idTaiKhoan == null) return;
        if (this.taiKhoan == null) {
            this.taiKhoan = new TaiKhoan();
        }
        this.taiKhoan.setId(idTaiKhoan);
    }

    @JsonProperty("taiKhoanId")
    public void setTaiKhoanId(Integer taiKhoanId) {
        setIdTaiKhoan(taiKhoanId);
    }

}
