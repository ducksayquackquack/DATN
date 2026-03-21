package org.example.datnnhom03.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "OrderStatusHistory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @Column(name = "HoaDonId")
    private Integer hoaDonId;

    @Column(name = "FromStatusId")
    private Integer fromStatusId;

    @Column(name = "ToStatusId")
    private Integer toStatusId;

    @Column(name = "ChangedAt")
    private LocalDateTime changedAt;

    @Column(name = "ChangedBy")
    private Integer changedBy;

    @Column(name = "Note")
    private String note;
}
