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
@Table(name = "AnhMinhHoa")
public class AnhMinhHoa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "maAnh")
    private String maAnh;

    @Column(name = "anhMinhHoa")
    private String anhMinhHoa;

    @Column(name = "ngayThem")
    private LocalDateTime ngayThem;

    @Column(name = "trangThai")
    private String trangThai;

}
