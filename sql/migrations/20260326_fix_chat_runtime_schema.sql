-- Chat runtime compatibility migration for DirtyWave / DirtyWave6
-- Ensures schema matches DATN-API chat models/controllers.

IF DB_NAME() NOT IN ('DirtyWave', 'DirtyWave6')
BEGIN
    PRINT 'Warning: current database is ' + DB_NAME() + '. Expected DirtyWave or DirtyWave6.';
END
GO

IF COL_LENGTH('dbo.ChatSession', 'assignedEmployeeName') IS NULL
BEGIN
    ALTER TABLE dbo.ChatSession ADD assignedEmployeeName NVARCHAR(255) NULL;
    PRINT 'Added ChatSession.assignedEmployeeName';
END
GO

IF COL_LENGTH('dbo.ChatMessage', 'metadataJson') IS NULL
BEGIN
    ALTER TABLE dbo.ChatMessage ADD metadataJson NVARCHAR(MAX) NULL;
    PRINT 'Added ChatMessage.metadataJson';
END
GO

UPDATE dbo.ChatSession
SET status = 'OPEN'
WHERE status IS NULL
   OR UPPER(LTRIM(RTRIM(status))) IN ('ACTIVE', 'NEW', 'PENDING');

UPDATE dbo.ChatSession
SET status = 'IN_PROGRESS'
WHERE UPPER(LTRIM(RTRIM(status))) IN ('ASSIGNED', 'HANDLED', 'PROCESSING');

UPDATE dbo.ChatSession
SET status = 'WAITING_EMPLOYEE'
WHERE UPPER(LTRIM(RTRIM(status))) IN ('WAITING', 'WAITING_HUMAN', 'TRANSFER');
GO

IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_ChatSession_Status')
BEGIN
    ALTER TABLE dbo.ChatSession DROP CONSTRAINT CK_ChatSession_Status;
END
GO

ALTER TABLE dbo.ChatSession
ADD CONSTRAINT CK_ChatSession_Status
CHECK (status IN (N'OPEN', N'WAITING_EMPLOYEE', N'IN_PROGRESS', N'CLOSED'));
GO

IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_ChatSession_ChatMode')
BEGIN
    ALTER TABLE dbo.ChatSession DROP CONSTRAINT CK_ChatSession_ChatMode;
END
GO

ALTER TABLE dbo.ChatSession
ADD CONSTRAINT CK_ChatSession_ChatMode
CHECK (chatMode IS NULL OR chatMode IN (N'AUTO', N'HUMAN', N'TRANSFER'));
GO

IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_ChatMessage_SenderType')
BEGIN
    ALTER TABLE dbo.ChatMessage DROP CONSTRAINT CK_ChatMessage_SenderType;
END
GO

ALTER TABLE dbo.ChatMessage
ADD CONSTRAINT CK_ChatMessage_SenderType
CHECK (senderType IN (N'CUSTOMER', N'EMPLOYEE', N'SYSTEM', N'BOT'));
GO

PRINT 'Chat runtime schema migration completed.';
GO
