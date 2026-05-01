USE DirtyWave;
GO

IF COL_LENGTH('dbo.HoaDon', 'statusNote') IS NULL
BEGIN
    ALTER TABLE dbo.HoaDon
    ADD statusNote NVARCHAR(MAX) NULL;

    PRINT 'Added dbo.HoaDon.statusNote';
END
ELSE
BEGIN
    PRINT 'dbo.HoaDon.statusNote already exists';
END
GO
