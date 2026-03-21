package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.KhuyenMai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface KhuyenMaiRepository extends JpaRepository<KhuyenMai, Integer> {
	boolean existsByMaKhuyenMaiIgnoreCase(String maKhuyenMai);

	boolean existsByMaKhuyenMaiIgnoreCaseAndIdNot(String maKhuyenMai, Integer id);

	@Query(value = """
		SELECT MAX(TRY_CAST(SUBSTRING(maKhuyenMai, 3, LEN(maKhuyenMai) - 2) AS INT))
		FROM KhuyenMai
		WHERE maKhuyenMai LIKE 'KM[0-9]%'
	""", nativeQuery = true)
	Integer findMaxKhuyenMaiCodeNumber();
}
