-- Add Chat Tables to DirtyWave6 Database
-- Run this on your DirtyWave6 database

USE DirtyWave6;
GO

-- ChatSession: Stores chat session information
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES 
    WHERE TABLE_NAME = 'ChatSession' AND TABLE_SCHEMA = 'dbo')
BEGIN
    CREATE TABLE dbo.ChatSession (
        id INT IDENTITY(1,1) NOT NULL,
        sessionCode NVARCHAR(100) NOT NULL,
        customerName NVARCHAR(255) NULL,
        customerEmail NVARCHAR(100) NULL,
        customerPhone NVARCHAR(20) NULL,
        assignedEmployeeId INT NULL,
        assignedEmployeeName NVARCHAR(255) NULL,
        status NVARCHAR(50) NOT NULL DEFAULT N'OPEN',
        chatMode NVARCHAR(50) NULL DEFAULT N'AUTO',
        acceptedAt DATETIME2 NULL,
        lastMessageAt DATETIME2 NULL,
        createdAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        updatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        
        CONSTRAINT PK_ChatSession PRIMARY KEY (id),
        CONSTRAINT UQ_ChatSession_sessionCode UNIQUE (sessionCode),
        CONSTRAINT FK_ChatSession_Employee FOREIGN KEY (assignedEmployeeId) 
            REFERENCES dbo.NhanVien(id),
        CONSTRAINT CK_ChatSession_Status CHECK (status IN (N'OPEN', N'WAITING_EMPLOYEE', N'IN_PROGRESS', N'CLOSED')),
        CONSTRAINT CK_ChatSession_ChatMode CHECK (chatMode IN (N'AUTO', N'HUMAN', N'TRANSFER'))
    );
    
    CREATE INDEX IX_ChatSession_customerEmail ON dbo.ChatSession(customerEmail);
    CREATE INDEX IX_ChatSession_status ON dbo.ChatSession(status);
    CREATE INDEX IX_ChatSession_assignedEmployeeId ON dbo.ChatSession(assignedEmployeeId);
    CREATE INDEX IX_ChatSession_createdAt ON dbo.ChatSession(createdAt);
    
    PRINT 'ChatSession table created successfully';
END
ELSE
BEGIN
    PRINT 'ChatSession table already exists';
END
GO

-- ChatMessage: Stores individual messages in a chat session
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES 
    WHERE TABLE_NAME = 'ChatMessage' AND TABLE_SCHEMA = 'dbo')
BEGIN
    CREATE TABLE dbo.ChatMessage (
        id BIGINT IDENTITY(1,1) NOT NULL,
        sessionId INT NOT NULL,
        senderType NVARCHAR(50) NOT NULL,
        senderName NVARCHAR(255) NOT NULL,
        senderId NVARCHAR(100) NULL,
        content NVARCHAR(MAX) NOT NULL,
        messageType NVARCHAR(50) NULL,
        attachmentUrl NVARCHAR(MAX) NULL,
        isread BIT NOT NULL DEFAULT 0,
        metadataJson NVARCHAR(MAX) NULL,
        createdAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        
        CONSTRAINT PK_ChatMessage PRIMARY KEY (id),
        CONSTRAINT FK_ChatMessage_Session FOREIGN KEY (sessionId) 
            REFERENCES dbo.ChatSession(id) ON DELETE CASCADE,
        CONSTRAINT CK_ChatMessage_SenderType CHECK (senderType IN (N'CUSTOMER', N'EMPLOYEE', N'SYSTEM', N'BOT'))
    );
    
    CREATE INDEX IX_ChatMessage_sessionId ON dbo.ChatMessage(sessionId);
    CREATE INDEX IX_ChatMessage_createdAt ON dbo.ChatMessage(createdAt);
    CREATE INDEX IX_ChatMessage_senderType ON dbo.ChatMessage(senderType);
    CREATE INDEX IX_ChatMessage_senderId ON dbo.ChatMessage(senderId);
    
    PRINT 'ChatMessage table created successfully';
END
ELSE
BEGIN
    PRINT 'ChatMessage table already exists';
    
    -- Add metadataJson column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_NAME = 'ChatMessage' 
        AND COLUMN_NAME = 'metadataJson'
        AND TABLE_SCHEMA = 'dbo'
    )
    BEGIN
        ALTER TABLE dbo.ChatMessage
        ADD metadataJson NVARCHAR(MAX) NULL;
        PRINT 'metadataJson column added to ChatMessage';
    END
END
GO

-- ChatSessionEvent: Logs all events during a chat session
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES 
    WHERE TABLE_NAME = 'ChatSessionEvent' AND TABLE_SCHEMA = 'dbo')
BEGIN
    CREATE TABLE dbo.ChatSessionEvent (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        sessionId INT NOT NULL,
        eventType NVARCHAR(50) NOT NULL,
        actorType NVARCHAR(50) NOT NULL,
        actorName NVARCHAR(255) NULL,
        actorId NVARCHAR(100) NULL,
        createdAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        
        CONSTRAINT FK_ChatSessionEvent_Session FOREIGN KEY (sessionId) 
            REFERENCES dbo.ChatSession(id) ON DELETE CASCADE,
        CONSTRAINT CK_ChatSessionEvent_EventType CHECK (eventType IN (
            N'SESSION_CREATED', N'MESSAGE_SENT', N'ACCEPTED', N'TRANSFERRED', N'CLOSED', N'REOPENED'
        )),
        CONSTRAINT CK_ChatSessionEvent_ActorType CHECK (actorType IN (
            N'CUSTOMER', N'EMPLOYEE', N'SYSTEM', N'BOT'
        ))
    );
    
    CREATE INDEX IX_ChatSessionEvent_sessionId ON dbo.ChatSessionEvent(sessionId);
    CREATE INDEX IX_ChatSessionEvent_eventType ON dbo.ChatSessionEvent(eventType);
    CREATE INDEX IX_ChatSessionEvent_createdAt ON dbo.ChatSessionEvent(createdAt);
    
    PRINT 'ChatSessionEvent table created successfully';
END
ELSE
BEGIN
    PRINT 'ChatSessionEvent table already exists';
END
GO

PRINT 'Chat tables migration completed successfully!';
GO
