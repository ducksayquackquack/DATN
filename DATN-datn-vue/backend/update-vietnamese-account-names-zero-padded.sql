USE DirtyWave;
GO

SET NOCOUNT ON;
GO

BEGIN TRY
    BEGIN TRAN;

    ;WITH EmployeeNames AS (
        SELECT *
        FROM (VALUES
            (N'employee01@dirtywave.com', N'Nguyen Minh Khoa'),
            (N'employee02@dirtywave.com', N'Tran Quoc Bao'),
            (N'employee03@dirtywave.com', N'Le Hoang Nam'),
            (N'employee04@dirtywave.com', N'Pham Gia Huy'),
            (N'employee05@dirtywave.com', N'Vo Thanh Dat'),
            (N'employee06@dirtywave.com', N'Bui Tuan Anh'),
            (N'employee07@dirtywave.com', N'Dang Ngoc Son'),
            (N'employee08@dirtywave.com', N'Hoang Minh Tri'),
            (N'employee09@dirtywave.com', N'Doan Quang Hieu'),
            (N'employee10@dirtywave.com', N'Nguyen Thi Mai Anh'),
            (N'employee11@dirtywave.com', N'Tran Khanh Linh'),
            (N'employee12@dirtywave.com', N'Le Thu Thao')
        ) AS e(email, tenNhanVien)
    )
    UPDATE nv
    SET nv.tenNhanVien = e.tenNhanVien
    FROM dbo.NhanVien nv
    JOIN dbo.TaiKhoan tk ON tk.id = nv.idTaiKhoan
    JOIN EmployeeNames e ON LOWER(tk.email) = LOWER(e.email);

    ;WITH CustomerNames AS (
        SELECT *
        FROM (VALUES
            (N'customer01@dirtywave.com', N'Nguyen Hai Nam'),
            (N'customer02@dirtywave.com', N'Tran Bao Ngoc'),
            (N'customer03@dirtywave.com', N'Le Duc Manh'),
            (N'customer04@dirtywave.com', N'Pham Thu Ha'),
            (N'customer05@dirtywave.com', N'Vo Hoang Long'),
            (N'customer06@dirtywave.com', N'Bui Khanh Vy'),
            (N'customer07@dirtywave.com', N'Dang Tuan Kiet'),
            (N'customer08@dirtywave.com', N'Hoang Gia Han'),
            (N'customer09@dirtywave.com', N'Doan Minh Tam'),
            (N'customer10@dirtywave.com', N'Nguyen Phuong Linh'),
            (N'customer11@dirtywave.com', N'Tran Ngoc Anh'),
            (N'customer12@dirtywave.com', N'Le Quang Huy')
        ) AS c(email, tenKhachHang)
    )
    UPDATE kh
    SET kh.tenKhachHang = c.tenKhachHang
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

SELECT tk.email, nv.tenNhanVien
FROM dbo.NhanVien nv
JOIN dbo.TaiKhoan tk ON tk.id = nv.idTaiKhoan
WHERE tk.email LIKE N'employee%dirtywave.com'
ORDER BY tk.email;

SELECT tk.email, kh.tenKhachHang
FROM dbo.KhachHang kh
JOIN dbo.TaiKhoan tk ON tk.id = kh.idTaiKhoan
WHERE tk.email LIKE N'customer%dirtywave.com'
ORDER BY tk.email;
GO
