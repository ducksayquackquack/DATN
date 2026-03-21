-- Migration: add statusNote and phuongThucThanhToan columns to HoaDon table
-- Run against the DirtyWave database before restarting the Spring Boot app

IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.HoaDon') AND name = N'statusNote'
)
BEGIN
    ALTER TABLE dbo.HoaDon ADD statusNote NVARCHAR(MAX) NULL;
END

IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.HoaDon') AND name = N'phuongThucThanhToan'
)
BEGIN
    ALTER TABLE dbo.HoaDon ADD phuongThucThanhToan NVARCHAR(100) NULL;
END
