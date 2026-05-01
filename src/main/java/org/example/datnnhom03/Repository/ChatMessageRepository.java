package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Find all messages for a specific session
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Integer sessionId);

    // Find unread messages in a session
    List<ChatMessage> findBySessionIdAndIsreadFalse(Integer sessionId);

    // Find messages by sender type
    List<ChatMessage> findBySessionIdAndSenderType(Integer sessionId, String senderType);

    // Find all messages by a specific sender
    List<ChatMessage> findBySenderIdOrderByCreatedAtDesc(String senderId);

    // Find recent messages in a session
    @Query(value = "SELECT TOP (:limit) * FROM ChatMessage WHERE sessionId = :sessionId ORDER BY createdAt DESC", nativeQuery = true)
    List<ChatMessage> findRecentMessagesInSession(@Param("sessionId") Integer sessionId, @Param("limit") Integer limit);

    // Count unread messages in a session
    long countBySessionIdAndIsreadFalse(Integer sessionId);

    // Find messages containing specific content
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatSession.id = :sessionId AND cm.content LIKE %:keyword% ORDER BY cm.createdAt DESC")
    List<ChatMessage> searchMessagesByContent(@Param("sessionId") Integer sessionId, @Param("keyword") String keyword);
}
