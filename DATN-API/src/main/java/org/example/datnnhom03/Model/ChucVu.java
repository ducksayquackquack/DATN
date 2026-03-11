package org.example.datnnhom03.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "ChucVu")
public class ChucVu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "maChucVu")
    private String maChucVu;

    @Column(name ="tenChucVu")
    private String tenChucVu;

    @Column(name = "ngaySua")
    private LocalDateTime ngaySua;

    @Column(name = "trangThai")
    private String trangThai;
}
