USE DirtyWave;
GO

IF COL_LENGTH('dbo.KhuyenMai', 'donViGiam') IS NULL
BEGIN
    ALTER TABLE dbo.KhuyenMai
    ADD donViGiam NVARCHAR(20) NOT NULL
        CONSTRAINT DF_KhuyenMai_donViGiam DEFAULT(N'PERCENT');

    PRINT 'Added dbo.KhuyenMai.donViGiam';
END
ELSE
BEGIN
    PRINT 'dbo.KhuyenMai.donViGiam already exists';
END
GO

UPDATE dbo.KhuyenMai
SET donViGiam = N'PERCENT'
WHERE donViGiam IS NULL OR LTRIM(RTRIM(donViGiam)) = N'';
GO
