-- Migration: Add metadataJson column to ChatMessage table
-- Date: 2026-03-26
-- Description: Adds metadataJson column to store additional message metadata like products, quickReplies, etc.

USE DirtyWave;
GO

-- Check if column already exists
IF NOT EXISTS (
    SELECT 1 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_NAME = 'ChatMessage' 
    AND COLUMN_NAME = 'metadataJson'
    AND TABLE_SCHEMA = 'dbo'
)
BEGIN
    PRINT 'Adding metadataJson column to ChatMessage table...';
    
    ALTER TABLE dbo.ChatMessage
    ADD metadataJson NVARCHAR(MAX) NULL;
    
    PRINT 'metadataJson column added successfully.';
END
ELSE
BEGIN
    PRINT 'metadataJson column already exists in ChatMessage table.';
END
GO
