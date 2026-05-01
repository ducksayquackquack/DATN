package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Integer> {

    // Find by session code
    Optional<ChatSession> findBySessionCode(String sessionCode);

    // Find by customer email
    List<ChatSession> findByCustomerEmail(String customerEmail);

    // Find all active sessions
    List<ChatSession> findByStatus(String status);

    // Find sessions assigned to a specific employee
    List<ChatSession> findByAssignedEmployeeId(Integer employeeId);

    // Custom query to find sessions with unread messages
    @Query("SELECT cs FROM ChatSession cs WHERE cs.status = :status ORDER BY cs.createdAt DESC")
    List<ChatSession> findSessionsByStatusOrderByCreated(@Param("status") String status);

    // Find recent sessions
    @Query(value = "SELECT TOP (:limit) * FROM ChatSession ORDER BY createdAt DESC", nativeQuery = true)
    List<ChatSession> findRecentSessions(@Param("limit") Integer limit);

    // Find sessions waiting for human support
    List<ChatSession> findByStatusAndChatMode(String status, String chatMode);

    // Find sessions by a list of statuses
    List<ChatSession> findByStatusIn(Collection<String> statuses);
}
