package org.example.datnnhom03.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.datnnhom03.dto.hoadon.OrderStatusOptionDto;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class OrderStatusLookupRepository {

    @PersistenceContext
    private EntityManager em;

    public List<OrderStatusOptionDto> findAllOptions() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
            SELECT Id, Name
            FROM dbo.OrderStatus
            WHERE Code IN ('CHO_XAC_NHAN','CHO_LAY_HANG','DANG_GIAO','GIAO_THAT_BAI','HOAN_VE','HOAN_THANH','HUY')
            ORDER BY SortOrder, Id
        """).getResultList();

        return rows.stream()
                .map(r -> new OrderStatusOptionDto(((Number) r[0]).intValue(), (String) r[1]))
                .toList();
    }

    public String findNameById(Integer id) {
        if (id == null) return "";
        try {
            Object v = em.createNativeQuery("SELECT Name FROM dbo.OrderStatus WHERE Id = :id")
                    .setParameter("id", id)
                    .getSingleResult();
            return v == null ? "" : v.toString();
        } catch (Exception e) {
            return "";
        }
    }

    // ✅ cái này HoaDonApiController đang gọi => anh phải có
    public String findCodeById(Integer id) {
        if (id == null) return "";
        try {
            Object v = em.createNativeQuery("SELECT Code FROM dbo.OrderStatus WHERE Id = :id")
                    .setParameter("id", id)
                    .getSingleResult();
            return v == null ? "" : v.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public Integer findIdByCode(String code) {
        if (code == null || code.isBlank()) return null;
        try {
            Object v = em.createNativeQuery("SELECT Id FROM dbo.OrderStatus WHERE Code = :code")
                    .setParameter("code", code)
                    .getSingleResult();
            return v == null ? null : ((Number) v).intValue();
        } catch (Exception e) {
            return null;
        }
    }
}