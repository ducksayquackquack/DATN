package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.ChatSessionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionEventRepository extends JpaRepository<ChatSessionEvent, Long> {

    // Find all events for a specific chat session
    List<ChatSessionEvent> findBySessionIdOrderByCreatedAtAsc(Integer sessionId);

    // Find events by type
    List<ChatSessionEvent> findByEventType(String eventType);

    // Find events for a session by type
    List<ChatSessionEvent> findBySessionIdAndEventType(Integer sessionId, String eventType);

    // Find events by actor (employee or customer who triggered the event)
    List<ChatSessionEvent> findByActorId(String actorId);

    // Find recent events in a session
    @Query(value = "SELECT TOP (:limit) * FROM ChatSessionEvent WHERE sessionId = :sessionId ORDER BY createdAt DESC", nativeQuery = true)
    List<ChatSessionEvent> findRecentEventsInSession(@Param("sessionId") Integer sessionId, @Param("limit") Integer limit);

    // Find all ACCEPTED events (when an employee accepts a chat)
    List<ChatSessionEvent> findByEventTypeOrderByCreatedAtDesc(String eventType);

    // Count events of a specific type in a session
    long countBySessionIdAndEventType(Integer sessionId, String eventType);
}
