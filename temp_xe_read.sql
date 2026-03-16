SET QUOTED_IDENTIFIER ON;
WITH src AS (
  SELECT CAST(t.target_data AS XML) AS x
  FROM sys.dm_xe_sessions s
  JOIN sys.dm_xe_session_targets t ON s.address=t.event_session_address
  WHERE s.name='TrackJdbcErrors' AND t.target_name='ring_buffer'
)
SELECT TOP 20
  ev.value('(action[@name="client_app_name"]/value)[1]','nvarchar(256)') AS app_name,
  ev.value('(action[@name="database_name"]/value)[1]','nvarchar(256)') AS db_name,
  ev.value('(data[@name="error_number"]/value)[1]','int') AS error_number,
  ev.value('(data[@name="message"]/value)[1]','nvarchar(max)') AS message,
  ev.value('(action[@name="sql_text"]/value)[1]','nvarchar(max)') AS sql_text
FROM src
CROSS APPLY x.nodes('//RingBufferTarget/event[@name="error_reported"]') AS n(ev)
ORDER BY ev.value('(@timestamp)[1]','datetime2') DESC;
