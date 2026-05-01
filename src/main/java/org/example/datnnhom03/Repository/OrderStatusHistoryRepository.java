package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.OrderStatusHistory;
import org.example.datnnhom03.Repository.projection.OrderStatusHistoryView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
    @Query(value = """
    SELECT h.ChangedAt AS changedAt,
           osFrom.Name AS fromStatus,
           osTo.Name   AS toStatus,
           h.Note      AS note
    FROM dbo.OrderStatusHistory h
    LEFT JOIN dbo.OrderStatus osFrom ON osFrom.Id = h.FromStatusId
    JOIN dbo.OrderStatus osTo ON osTo.Id = h.ToStatusId
    WHERE h.HoaDonId = :hoaDonId
    ORDER BY h.ChangedAt DESC
""", nativeQuery = true)
    List<OrderStatusHistoryView> viewByHoaDonId(@Param("hoaDonId") Integer hoaDonId);
    List<OrderStatusHistory> findByHoaDonIdOrderByChangedAtDesc(Integer hoaDonId);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO OrderStatusHistory(hoaDonId, fromStatusId, toStatusId, changedAt, note)
        VALUES (:hoaDonId, :fromId, :toId, :changedAt, :note)
    """, nativeQuery = true)
    void insertHistory(@Param("hoaDonId") Integer hoaDonId,
                       @Param("fromId") Integer fromId,
                       @Param("toId") Integer toId,
                       @Param("changedAt") LocalDateTime changedAt,
                       @Param("note") String note);

    // cái viewByHoaDonId anh đang có giữ nguyên
}
