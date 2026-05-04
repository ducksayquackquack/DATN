-- ============================================================
-- MIGRATION: Add Chat Feature Tables to DirtyWave Database
-- Date: 2026-03-26
-- Purpose: Integrate AI chat helper functionality
-- Source: Extracted from DirtyWave6.sql and updateDW6.sql
-- ============================================================

USE DirtyWave;
GO

PRINT '========== STARTING CHAT MIGRATION ==========';
GO

-- ============================================================
-- TABLE 1: ChatSession
-- Stores chat sessions between customers and support staff
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES 
    WHERE TABLE_NAME = 'ChatSession' AND TABLE_SCHEMA = 'dbo')
BEGIN
    PRINT 'Creating table: ChatSession...';
    
    CREATE TABLE dbo.ChatSession (
        id INT IDENTITY(1,1) NOT NULL,
        sessionCode NVARCHAR(100) NOT NULL,
        customerName NVARCHAR(255) NULL,
        customerEmail NVARCHAR(100) NULL,
        customerPhone NVARCHAR(20) NULL,
        assignedEmployeeId INT NULL,
        assignedEmployeeName NVARCHAR(255) NULL,
        status NVARCHAR(50) NOT NULL,           -- Active, Closed, Waiting, etc.
        chatMode NVARCHAR(50) NULL,             -- AUTO (AI), HUMAN, TRANSFER, etc.
        acceptedAt DATETIME2 NULL,
        lastMessageAt DATETIME2 NULL,
        createdAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        updatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        
        CONSTRAINT PK_ChatSession PRIMARY KEY (id),
        CONSTRAINT UQ_ChatSession_sessionCode UNIQUE (sessionCode),
        CONSTRAINT FK_ChatSession_Employee FOREIGN KEY (assignedEmployeeId) 
            REFERENCES dbo.NhanVien(id) ON DELETE SET NULL
    );
    
    -- Create indexes for better query performance
    CREATE INDEX IX_ChatSession_customerEmail ON dbo.ChatSession(customerEmail);
    CREATE INDEX IX_ChatSession_status ON dbo.ChatSession(status);
    CREATE INDEX IX_ChatSession_createdAt ON dbo.ChatSession(createdAt);
    CREATE INDEX IX_ChatSession_assignedEmployeeId ON dbo.ChatSession(assignedEmployeeId);
    
    PRINT 'Table ChatSession created successfully.';
END
ELSE
BEGIN
    PRINT 'Table ChatSession already exists. Skipping.';
END

GO

-- ============================================================
-- TABLE 2: ChatMessage
-- Stores individual messages within chat sessions
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES 
    WHERE TABLE_NAME = 'ChatMessage' AND TABLE_SCHEMA = 'dbo')
BEGIN
    PRINT 'Creating table: ChatMessage...';
    
    CREATE TABLE dbo.ChatMessage (
        id BIGINT IDENTITY(1,1) NOT NULL,
        sessionId INT NOT NULL,
        senderType NVARCHAR(50) NOT NULL,      -- CUSTOMER, EMPLOYEE, SYSTEM
        senderName NVARCHAR(255) NOT NULL,
        senderId NVARCHAR(100) NULL,           -- Optional: ID of sender
        content NVARCHAR(MAX) NOT NULL,
        messageType NVARCHAR(50) NULL,         -- TEXT, IMAGE, FILE, SYSTEM, etc.
        attachmentUrl NVARCHAR(MAX) NULL,      -- For file/image messages
        isread BIT NOT NULL DEFAULT 0,
        createdAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        
        CONSTRAINT PK_ChatMessage PRIMARY KEY (id),
        CONSTRAINT FK_ChatMessage_Session FOREIGN KEY (sessionId) 
            REFERENCES dbo.ChatSession(id) ON DELETE CASCADE,
        CONSTRAINT CK_ChatMessage_SenderType CHECK (senderType IN (N'CUSTOMER', N'EMPLOYEE', N'SYSTEM'))
    );
    
    -- Create indexes for better query performance
    CREATE INDEX IX_ChatMessage_sessionId ON dbo.ChatMessage(sessionId);
    CREATE INDEX IX_ChatMessage_createdAt ON dbo.ChatMessage(createdAt);
    CREATE INDEX IX_ChatMessage_senderType ON dbo.ChatMessage(senderType);
    CREATE INDEX IX_ChatMessage_senderId ON dbo.ChatMessage(senderId);
    
    PRINT 'Table ChatMessage created successfully.';
END
ELSE
BEGIN
    PRINT 'Table ChatMessage already exists. Skipping.';
END

GO

-- ============================================================
-- TABLE 3: ChatSessionEvent
-- Logs all events that occur during a chat session
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES 
    WHERE TABLE_NAME = 'ChatSessionEvent' AND TABLE_SCHEMA = 'dbo')
BEGIN
    PRINT 'Creating table: ChatSessionEvent...';
    
    CREATE TABLE dbo.ChatSessionEvent (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        sessionId INT NOT NULL,
        eventType NVARCHAR(50) NOT NULL,       -- REQUEST_HUMAN, ACCEPTED, CLOSED, TRANSFERRED, etc.
        actorType NVARCHAR(20) NULL,           -- CUSTOMER, EMPLOYEE, SYSTEM
        actorId NVARCHAR(100) NULL,
        actorName NVARCHAR(255) NULL,
        payloadJson NVARCHAR(MAX) NULL,        -- Additional event data as JSON
        createdAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        
        CONSTRAINT FK_ChatSessionEvent_Session FOREIGN KEY (sessionId) 
            REFERENCES dbo.ChatSession(id) ON DELETE CASCADE
    );
    
    -- Create indexes for better query performance
    CREATE INDEX IX_ChatSessionEvent_sessionId ON dbo.ChatSessionEvent(sessionId);
    CREATE INDEX IX_ChatSessionEvent_eventType ON dbo.ChatSessionEvent(eventType);
    CREATE INDEX IX_ChatSessionEvent_createdAt ON dbo.ChatSessionEvent(createdAt);
    
    PRINT 'Table ChatSessionEvent created successfully.';
END
ELSE
BEGIN
    PRINT 'Table ChatSessionEvent already exists. Skipping.';
END

GO

-- ============================================================
-- VERIFICATION QUERIES
-- Check if all tables were created successfully
-- ============================================================
PRINT '';
PRINT '========== VERIFICATION ==========';

-- Check ChatSession table
IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES 
    WHERE TABLE_NAME = 'ChatSession' AND TABLE_SCHEMA = 'dbo')
BEGIN
    PRINT 'ChatSession table: EXISTS ✓';
    SELECT 'ChatSession columns:' AS Info;
    SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME = 'ChatSession'
    ORDER BY ORDINAL_POSITION;
END
ELSE
BEGIN
    PRINT 'ChatSession table: NOT FOUND ✗';
END

GO

-- Check ChatMessage table
IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES 
    WHERE TABLE_NAME = 'ChatMessage' AND TABLE_SCHEMA = 'dbo')
BEGIN
    PRINT 'ChatMessage table: EXISTS ✓';
    SELECT 'ChatMessage columns:' AS Info;
    SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME = 'ChatMessage'
    ORDER BY ORDINAL_POSITION;
END
ELSE
BEGIN
    PRINT 'ChatMessage table: NOT FOUND ✗';
END

GO

-- Check ChatSessionEvent table
IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES 
    WHERE TABLE_NAME = 'ChatSessionEvent' AND TABLE_SCHEMA = 'dbo')
BEGIN
    PRINT 'ChatSessionEvent table: EXISTS ✓';
    SELECT 'ChatSessionEvent columns:' AS Info;
    SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME = 'ChatSessionEvent'
    ORDER BY ORDINAL_POSITION;
END
ELSE
BEGIN
    PRINT 'ChatSessionEvent table: NOT FOUND ✗';
END

GO

PRINT '';
PRINT '========== MIGRATION COMPLETED ==========';
PRINT 'Chat tables have been added to DirtyWave database.';
GO
