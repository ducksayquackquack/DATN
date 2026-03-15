USE DirtyWave;
GO

SET NOCOUNT ON;
GO

BEGIN TRY
    BEGIN TRAN;

    UPDATE nv
    SET
        nv.tenNhanVien = N'Nguyen Duc Anh',
        nv.diaChi = N'Ha Noi'
    FROM dbo.NhanVien nv
    JOIN dbo.TaiKhoan tk ON tk.id = nv.idTaiKhoan
    WHERE LOWER(tk.email) = N'admin@dirtywave.com';

    UPDATE nv
    SET
        nv.tenNhanVien = N'Tran Minh Quan',
        nv.diaChi = N'TP Ho Chi Minh'
    FROM dbo.NhanVien nv
    JOIN dbo.TaiKhoan tk ON tk.id = nv.idTaiKhoan
    WHERE LOWER(tk.email) = N'admin2@dirtywave.com';

    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
GO

SELECT tk.email, nv.tenNhanVien, nv.diaChi
FROM dbo.NhanVien nv
JOIN dbo.TaiKhoan tk ON tk.id = nv.idTaiKhoan
WHERE LOWER(tk.email) IN (N'admin@dirtywave.com', N'admin2@dirtywave.com')
ORDER BY tk.email;
GO
