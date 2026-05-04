USE DirtyWave;
GO

SET NOCOUNT ON;
GO

BEGIN TRY
    BEGIN TRAN;

    /* Admin names */
    ;WITH AdminNames AS (
        SELECT *
        FROM (VALUES
            (N'admin@dirtywave.com', N'Nguyễn Đức Anh', N'Nam', CAST('1990-01-01' AS DATE), N'Hà Nội', N'0900000000'),
            (N'admin2@dirtywave.com', N'Trần Minh Quân', N'Nam', CAST('1992-05-14' AS DATE), N'TP. Hồ Chí Minh', N'0900000001')
        ) AS a(email, tenNhanVien, gioiTinh, ngaySinh, diaChi, soDienThoai)
    )
    UPDATE nv
    SET
        nv.tenNhanVien = a.tenNhanVien,
        nv.gioiTinh = a.gioiTinh,
        nv.ngaySinh = a.ngaySinh,
        nv.diaChi = a.diaChi,
        nv.soDienThoai = a.soDienThoai,
        nv.trangThaiHoatDong = COALESCE(nv.trangThaiHoatDong, N'Hoạt động')
    FROM dbo.NhanVien nv
    JOIN dbo.TaiKhoan tk ON tk.id = nv.idTaiKhoan
    JOIN AdminNames a ON LOWER(tk.email) = LOWER(a.email);

    /* Employee names */
    ;WITH EmployeeNames AS (
        SELECT *
        FROM (VALUES
            (N'employee1@dirtywave.com', N'Nguyễn Minh Khoa', N'Nam', CAST('1998-01-15' AS DATE), N'Hà Nội', N'0901000001'),
            (N'employee2@dirtywave.com', N'Trần Quốc Bảo', N'Nam', CAST('1997-08-22' AS DATE), N'Hà Nội', N'0901000002'),
            (N'employee3@dirtywave.com', N'Lê Hoàng Nam', N'Nam', CAST('1999-03-09' AS DATE), N'Đà Nẵng', N'0901000003'),
            (N'employee4@dirtywave.com', N'Phạm Gia Huy', N'Nam', CAST('1996-11-02' AS DATE), N'Hải Phòng', N'0901000004'),
            (N'employee5@dirtywave.com', N'Võ Thành Đạt', N'Nam', CAST('1998-06-18' AS DATE), N'Cần Thơ', N'0901000005'),
            (N'employee6@dirtywave.com', N'Bùi Tuấn Anh', N'Nam', CAST('1997-10-12' AS DATE), N'Hà Nội', N'0901000006'),
            (N'employee7@dirtywave.com', N'Đặng Ngọc Sơn', N'Nam', CAST('1995-12-25' AS DATE), N'Nam Định', N'0901000007'),
            (N'employee8@dirtywave.com', N'Hoàng Minh Trí', N'Nam', CAST('1998-04-30' AS DATE), N'Nghệ An', N'0901000008'),
            (N'employee9@dirtywave.com', N'Đoàn Quang Hiếu', N'Nam', CAST('1999-09-14' AS DATE), N'Thanh Hóa', N'0901000009'),
            (N'employee10@dirtywave.com', N'Nguyễn Thị Mai Anh', N'Nữ', CAST('2000-02-20' AS DATE), N'Hà Nội', N'0901000010'),
            (N'employee11@dirtywave.com', N'Trần Khánh Linh', N'Nữ', CAST('1999-07-07' AS DATE), N'TP. Hồ Chí Minh', N'0901000011'),
            (N'employee12@dirtywave.com', N'Lê Thu Thảo', N'Nữ', CAST('2001-01-11' AS DATE), N'Đà Nẵng', N'0901000012')
        ) AS e(email, tenNhanVien, gioiTinh, ngaySinh, diaChi, soDienThoai)
    )
    UPDATE nv
    SET
        nv.tenNhanVien = e.tenNhanVien,
        nv.gioiTinh = e.gioiTinh,
        nv.ngaySinh = e.ngaySinh,
        nv.diaChi = e.diaChi,
        nv.soDienThoai = e.soDienThoai,
        nv.trangThaiHoatDong = COALESCE(nv.trangThaiHoatDong, N'Hoạt động')
    FROM dbo.NhanVien nv
    JOIN dbo.TaiKhoan tk ON tk.id = nv.idTaiKhoan
    JOIN EmployeeNames e ON LOWER(tk.email) = LOWER(e.email);

    /* Customer names */
    ;WITH CustomerNames AS (
        SELECT *
        FROM (VALUES
            (N'customer1@dirtywave.com', N'Nguyễn Hải Nam', N'Nam', CAST('1999-03-12' AS DATE), N'0912000001'),
            (N'customer2@dirtywave.com', N'Trần Bảo Ngọc', N'Nữ', CAST('2000-07-22' AS DATE), N'0912000002'),
            (N'customer3@dirtywave.com', N'Lê Đức Mạnh', N'Nam', CAST('1998-11-08' AS DATE), N'0912000003'),
            (N'customer4@dirtywave.com', N'Phạm Thu Hà', N'Nữ', CAST('2001-04-17' AS DATE), N'0912000004'),
            (N'customer5@dirtywave.com', N'Võ Hoàng Long', N'Nam', CAST('1997-09-26' AS DATE), N'0912000005'),
            (N'customer6@dirtywave.com', N'Bùi Khánh Vy', N'Nữ', CAST('2002-01-05' AS DATE), N'0912000006'),
            (N'customer7@dirtywave.com', N'Đặng Tuấn Kiệt', N'Nam', CAST('1999-12-19' AS DATE), N'0912000007'),
            (N'customer8@dirtywave.com', N'Hoàng Gia Hân', N'Nữ', CAST('2000-05-03' AS DATE), N'0912000008'),
            (N'customer9@dirtywave.com', N'Đoàn Minh Tâm', N'Nam', CAST('1998-10-14' AS DATE), N'0912000009'),
            (N'customer10@dirtywave.com', N'Nguyễn Phương Linh', N'Nữ', CAST('2001-08-30' AS DATE), N'0912000010'),
            (N'customer11@dirtywave.com', N'Trần Ngọc Ánh', N'Nữ', CAST('2002-02-13' AS DATE), N'0912000011'),
            (N'customer12@dirtywave.com', N'Lê Quang Huy', N'Nam', CAST('1997-06-09' AS DATE), N'0912000012')
        ) AS c(email, tenKhachHang, gioiTinh, ngaySinh, soDienThoai)
    )
    UPDATE kh
    SET
        kh.tenKhachHang = c.tenKhachHang,
        kh.gioiTinh = c.gioiTinh,
        kh.ngaySinh = c.ngaySinh,
        kh.soDienThoai = c.soDienThoai,
        kh.trangThai = COALESCE(kh.trangThai, N'Hoạt động')
    FROM dbo.KhachHang kh
    JOIN dbo.TaiKhoan tk ON tk.id = kh.idTaiKhoan
    JOIN CustomerNames c ON LOWER(tk.email) = LOWER(c.email);

    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
GO

SELECT tk.email, nv.tenNhanVien, nv.maNhanVien
FROM dbo.NhanVien nv
JOIN dbo.TaiKhoan tk ON tk.id = nv.idTaiKhoan
WHERE tk.vaiTro IN (N'ADMIN', N'EMPLOYEE')
ORDER BY tk.email;

SELECT tk.email, kh.tenKhachHang, kh.maKhachHang
FROM dbo.KhachHang kh
JOIN dbo.TaiKhoan tk ON tk.id = kh.idTaiKhoan
WHERE tk.vaiTro = N'CUSTOMER'
ORDER BY tk.email;
GO
