USE DirtyWave;
GO

SET NOCOUNT ON;
GO

BEGIN TRY
	BEGIN TRAN;

	DECLARE @EmployeeChucVuId INT = (
		SELECT TOP 1 id
		FROM dbo.ChucVu
		WHERE maChucVu = N'EMPLOYEE'
		ORDER BY id
	);

	IF @EmployeeChucVuId IS NULL
	BEGIN
		RAISERROR(N'Khong tim thay ChucVu EMPLOYEE.', 16, 1);
	END

	;WITH MissingEmployeeAccounts AS (
		SELECT
			tk.id AS idTaiKhoan,
			tk.email,
			ROW_NUMBER() OVER (ORDER BY tk.id) AS rn
		FROM dbo.TaiKhoan tk
		LEFT JOIN dbo.NhanVien nv ON nv.idTaiKhoan = tk.id
		WHERE tk.vaiTro = N'EMPLOYEE'
		  AND nv.id IS NULL
	)
	, EmployeeNames AS (
		SELECT *
		FROM (VALUES
			(1, N'Nguyễn Minh Khoa'),
			(2, N'Trần Quốc Bảo'),
			(3, N'Lê Hoàng Nam'),
			(4, N'Phạm Gia Huy'),
			(5, N'Võ Thành Đạt'),
			(6, N'Bùi Tuấn Anh'),
			(7, N'Đặng Ngọc Sơn'),
			(8, N'Hoàng Minh Trí'),
			(9, N'Đoàn Quang Hiếu'),
			(10, N'Nguyễn Thị Mai Anh'),
			(11, N'Trần Khánh Linh'),
			(12, N'Lê Thu Thảo'),
			(13, N'Phạm Ngọc Hân'),
			(14, N'Võ Bảo Châu'),
			(15, N'Đặng Mỹ Tiên')
		) AS n(rn, tenNhanVien)
	)
	INSERT INTO dbo.NhanVien (
		idTaiKhoan,
		idChucVu,
		maNhanVien,
		tenNhanVien,
		gioiTinh,
		ngaySinh,
		diaChi,
		soDienThoai,
		trangThaiHoatDong,
		anh
	)
	SELECT
		m.idTaiKhoan,
		@EmployeeChucVuId,
		CONCAT(N'NV', RIGHT(CONCAT(N'000', CAST(m.idTaiKhoan AS NVARCHAR(10))), 3)),
		COALESCE(en.tenNhanVien, CONCAT(N'Nhan vien ', RIGHT(CONCAT(N'00', CAST(m.rn AS NVARCHAR(10))), 2))),
		N'Nam',
		DATEADD(YEAR, -25, CAST(GETDATE() AS DATE)),
		N'Ha Noi',
		CONCAT(N'09', RIGHT(CONCAT(N'00000000', CAST(m.idTaiKhoan AS NVARCHAR(10))), 8)),
		N'Hoat dong',
		NULL
	FROM MissingEmployeeAccounts m
	LEFT JOIN EmployeeNames en ON en.rn = m.rn;

	COMMIT;
END TRY
BEGIN CATCH
	IF @@TRANCOUNT > 0 ROLLBACK;
	THROW;
END CATCH;
GO

SELECT
	nv.id,
	nv.maNhanVien,
	nv.tenNhanVien,
	nv.idTaiKhoan,
	tk.email,
	tk.vaiTro
FROM dbo.NhanVien nv
JOIN dbo.TaiKhoan tk ON tk.id = nv.idTaiKhoan
WHERE tk.vaiTro = N'EMPLOYEE'
ORDER BY nv.id;
GO
