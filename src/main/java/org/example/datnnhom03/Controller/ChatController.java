package org.example.datnnhom03.Controller;

import org.example.datnnhom03.Model.ChatSession;
import org.example.datnnhom03.Model.ChatMessage;
import org.example.datnnhom03.Service.ChatSessionService;
import org.example.datnnhom03.Service.ChatMessageService;
import org.example.datnnhom03.dto.ChatSessionDTO;
import org.example.datnnhom03.dto.ChatMessageDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ChatController {

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private ChatMessageService chatMessageService;

    // ==================== Chat Session Endpoints ====================

    /**
     * Create a new chat session
     * POST /api/chat/sessions
     */
    @PostMapping("/sessions")
    public ResponseEntity<ChatSessionDTO> createSession(@RequestBody Map<String, String> request) {
        String customerEmail = request.get("customerEmail");
        String customerName = request.get("customerName");
        String customerPhone = request.get("customerPhone");

        ChatSession session = chatSessionService.createSession(customerEmail, customerName, customerPhone);
        return ResponseEntity.ok(chatSessionService.getSessionDTO(session));
    }

    /**
     * Get a specific chat session by ID
     * GET /api/chat/sessions/{sessionId}
     */
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ChatSessionDTO> getSession(@PathVariable Integer sessionId) {
        Optional<ChatSession> optional = chatSessionService.getSessionById(sessionId);
        if (optional.isPresent()) {
            return ResponseEntity.ok(chatSessionService.getSessionDTO(optional.get()));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Get all sessions for a customer
     * GET /api/chat/sessions/customer/{email}
     */
    @GetMapping("/sessions/customer/{email}")
    public ResponseEntity<List<ChatSessionDTO>> getCustomerSessions(@PathVariable String email) {
        List<ChatSession> sessions = chatSessionService.getCustomerSessions(email);
        List<ChatSessionDTO> dtoList = sessions.stream()
                .map(chatSessionService::getSessionDTO)
                .toList();
        return ResponseEntity.ok(dtoList);
    }

    /**
     * Get all active sessions (admin/support view)
     * GET /api/chat/sessions/active
     */
    @GetMapping("/sessions/active")
    public ResponseEntity<List<ChatSessionDTO>> getActiveSessions() {
        List<ChatSession> sessions = chatSessionService.getActiveSessions();
        List<ChatSessionDTO> dtoList = sessions.stream()
                .map(chatSessionService::getSessionDTO)
                .toList();
        return ResponseEntity.ok(dtoList);
    }

    /**
     * Get all sessions assigned to an employee
     * GET /api/chat/sessions/employee/{employeeId}
     */
    @GetMapping("/sessions/employee/{employeeId}")
    public ResponseEntity<List<ChatSessionDTO>> getEmployeeSessions(@PathVariable Integer employeeId) {
        List<ChatSession> sessions = chatSessionService.getEmployeeSessions(employeeId);
        List<ChatSessionDTO> dtoList = sessions.stream()
                .map(chatSessionService::getSessionDTO)
                .toList();
        return ResponseEntity.ok(dtoList);
    }

    /**
     * Assign session to an employee (transfer from AI to human support)
     * POST /api/chat/sessions/{sessionId}/assign
     */
    @PostMapping("/sessions/{sessionId}/assign")
    public ResponseEntity<ChatSessionDTO> assignSession(
            @PathVariable Integer sessionId,
            @RequestBody Map<String, Object> request) {
        Integer employeeId = (Integer) request.get("employeeId");
        String employeeName = (String) request.get("employeeName");

        ChatSession session = chatSessionService.assignToEmployee(sessionId, employeeId, employeeName);
        if (session != null) {
            return ResponseEntity.ok(chatSessionService.getSessionDTO(session));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Close a chat session
     * POST /api/chat/sessions/{sessionId}/close
     */
    @PostMapping("/sessions/{sessionId}/close")
    public ResponseEntity<ChatSessionDTO> closeSession(@PathVariable Integer sessionId) {
        ChatSession session = chatSessionService.closeSession(sessionId);
        if (session != null) {
            return ResponseEntity.ok(chatSessionService.getSessionDTO(session));
        }
        return ResponseEntity.notFound().build();
    }

    // ==================== Chat Message Endpoints ====================

    /**
     * Send a new message
     * POST /api/chat/sessions/{sessionId}/messages
     */
    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ChatMessageDTO> sendMessage(
            @PathVariable Integer sessionId,
            @RequestBody Map<String, String> request) {
        String senderType = request.get("senderType"); // CUSTOMER, EMPLOYEE, SYSTEM
        String senderName = request.get("senderName");
        String content = request.get("content");

        ChatMessage message = chatMessageService.saveMessage(sessionId, senderType, senderName, content);
        if (message != null) {
            return ResponseEntity.ok(chatMessageService.convertToDTO(message));
        }
        return ResponseEntity.badRequest().build();
    }

    /**
     * Send a message with attachment
     * POST /api/chat/sessions/{sessionId}/messages/upload
     */
    @PostMapping("/sessions/{sessionId}/messages/upload")
    public ResponseEntity<ChatMessageDTO> sendMessageWithAttachment(
            @PathVariable Integer sessionId,
            @RequestBody Map<String, String> request) {
        String senderType = request.get("senderType");
        String senderName = request.get("senderName");
        String content = request.get("content");
        String attachmentUrl = request.get("attachmentUrl");
        String messageType = request.get("messageType"); // IMAGE, FILE, etc.

        ChatMessage message = chatMessageService.saveMessageWithAttachment(
                sessionId, senderType, senderName, content, attachmentUrl, messageType);
        if (message != null) {
            return ResponseEntity.ok(chatMessageService.convertToDTO(message));
        }
        return ResponseEntity.badRequest().build();
    }

    /**
     * Get all messages in a session
     * GET /api/chat/sessions/{sessionId}/messages
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getMessages(@PathVariable Integer sessionId) {
        List<ChatMessage> messages = chatMessageService.getSessionMessages(sessionId);
        return ResponseEntity.ok(chatMessageService.convertToDTOs(messages));
    }

    /**
     * Get recent messages in a session
     * GET /api/chat/sessions/{sessionId}/messages/recent?limit=10
     */
    @GetMapping("/sessions/{sessionId}/messages/recent")
    public ResponseEntity<List<ChatMessageDTO>> getRecentMessages(
            @PathVariable Integer sessionId,
            @RequestParam(defaultValue = "20") Integer limit) {
        List<ChatMessage> messages = chatMessageService.getRecentMessages(sessionId, limit);
        return ResponseEntity.ok(chatMessageService.convertToDTOs(messages));
    }

    /**
     * Get unread messages in a session
     * GET /api/chat/sessions/{sessionId}/messages/unread
     */
    @GetMapping("/sessions/{sessionId}/messages/unread")
    public ResponseEntity<List<ChatMessageDTO>> getUnreadMessages(@PathVariable Integer sessionId) {
        List<ChatMessage> messages = chatMessageService.getUnreadMessages(sessionId);
        return ResponseEntity.ok(chatMessageService.convertToDTOs(messages));
    }

    /**
     * Mark a message as read
     * POST /api/chat/messages/{messageId}/read
     */
    @PostMapping("/messages/{messageId}/read")
    public ResponseEntity<ChatMessageDTO> markAsRead(@PathVariable Long messageId) {
        ChatMessage message = chatMessageService.markAsRead(messageId);
        if (message != null) {
            return ResponseEntity.ok(chatMessageService.convertToDTO(message));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Mark all messages in a session as read
     * POST /api/chat/sessions/{sessionId}/messages/read-all
     */
    @PostMapping("/sessions/{sessionId}/messages/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(@PathVariable Integer sessionId) {
        chatMessageService.markAllAsRead(sessionId);
        return ResponseEntity.ok(Map.of("status", "success", "message", "All messages marked as read"));
    }

    /**
     * Get unread message count for a session
     * GET /api/chat/sessions/{sessionId}/unread-count
     */
    @GetMapping("/sessions/{sessionId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable Integer sessionId) {
        long count = chatMessageService.getUnreadCount(sessionId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    /**
     * Search messages by content
     * GET /api/chat/sessions/{sessionId}/search?keyword=xxx
     */
    @GetMapping("/sessions/{sessionId}/search")
    public ResponseEntity<List<ChatMessageDTO>> searchMessages(
            @PathVariable Integer sessionId,
            @RequestParam String keyword) {
        List<ChatMessage> messages = chatMessageService.searchMessages(sessionId, keyword);
        return ResponseEntity.ok(chatMessageService.convertToDTOs(messages));
    }

    // ==================== Health Check ====================

    /**
     * Check if chat service is running
     * GET /api/chat/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "running", "service", "chat-service"));
    }
}
