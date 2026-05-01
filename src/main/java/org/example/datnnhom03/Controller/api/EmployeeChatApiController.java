package org.example.datnnhom03.Controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.datnnhom03.Model.ChatMessage;
import org.example.datnnhom03.Model.ChatSession;
import org.example.datnnhom03.Model.NhanVien;
import org.example.datnnhom03.Service.ChatMessageService;
import org.example.datnnhom03.Service.ChatSessionService;
import org.example.datnnhom03.security.AuthenticatedActorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/employee-chat")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"}, allowCredentials = "true")
public class EmployeeChatApiController {

    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;
    private final AuthenticatedActorService authenticatedActorService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EmployeeChatApiController(ChatSessionService chatSessionService,
                                     ChatMessageService chatMessageService,
                                     AuthenticatedActorService authenticatedActorService) {
        this.chatSessionService = chatSessionService;
        this.chatMessageService = chatMessageService;
        this.authenticatedActorService = authenticatedActorService;
    }

    @GetMapping("/sessions")
    public ResponseEntity<?> getSessions(@RequestParam(required = false) String status,
                                         Authentication authentication) {
        try {
            List<ChatSession> baseSessions = chatSessionService.getActiveSessions();
            Optional<NhanVien> currentEmployee = authenticatedActorService.getNhanVien(authentication);
            boolean isAdmin = authenticatedActorService.isAdmin(authentication);

            List<ChatSession> sessions = baseSessions.stream()
                    .filter(Objects::nonNull)
                    .filter(session -> isAdmin || canAccessSession(currentEmployee.orElse(null), session))
                    .filter(session -> status == null || status.isBlank() || status.equalsIgnoreCase(session.getStatus()))
                    .collect(Collectors.toList());

            List<Map<String, Object>> sessionDTOs = sessions.stream().map(session -> {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", session.getId());
                dto.put("sessionCode", session.getSessionCode());
                dto.put("customerName", session.getCustomerName());
                dto.put("customerEmail", session.getCustomerEmail());
                dto.put("customerPhone", session.getCustomerPhone());
                dto.put("assignedEmployeeId", session.getAssignedEmployeeId());
                dto.put("assignedEmployeeName", session.getAssignedEmployeeName());
                dto.put("status", session.getStatus());
                dto.put("chatMode", session.getChatMode());
                dto.put("acceptedAt", session.getAcceptedAt());
                dto.put("lastMessageAt", session.getLastMessageAt());
                dto.put("createdAt", session.getCreatedAt());
                dto.put("updatedAt", session.getUpdatedAt());
                dto.put("unreadCount", chatMessageService.getUnreadCount(session.getId()));

                List<ChatMessage> recentMessages = chatMessageService.getRecentMessages(session.getId(), 1);
                if (!recentMessages.isEmpty()) {
                    ChatMessage lastMsg = recentMessages.get(0);
                    dto.put("lastMessage", Map.of(
                            "content", Optional.ofNullable(lastMsg.getContent()).orElse(""),
                            "senderType", Optional.ofNullable(lastMsg.getSenderType()).orElse(""),
                            "createdAt", lastMsg.getCreatedAt()
                    ));
                }
                return dto;
            }).sorted((a, b) -> {
                LocalDateTime timeA = (LocalDateTime) a.get("lastMessageAt");
                LocalDateTime timeB = (LocalDateTime) b.get("lastMessageAt");
                if (timeA == null && timeB == null) return 0;
                if (timeA == null) return 1;
                if (timeB == null) return -1;
                return timeB.compareTo(timeA);
            }).collect(Collectors.toList());

            return ResponseEntity.ok(sessionDTOs);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @GetMapping("/sessions/{id}/messages")
    public ResponseEntity<?> getMessages(@PathVariable Integer id,
                                         Authentication authentication) {
        try {
            Optional<ChatSession> sessionOpt = chatSessionService.getSessionById(id);
            if (sessionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            if (!canAccessSession(authentication, sessionOpt.get())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Không được truy cập phiên chat này"));
            }

            List<ChatMessage> messages = chatMessageService.getMessagesBySessionId(id);
            List<Map<String, Object>> messageDTOs = messages.stream().map(msg -> {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", msg.getId());
                dto.put("sessionId", msg.getSessionId());
                dto.put("senderType", msg.getSenderType());
                dto.put("senderName", msg.getSenderName());
                dto.put("senderId", msg.getSenderId());
                dto.put("content", msg.getContent());
                dto.put("messageType", msg.getMessageType());
                dto.put("attachmentUrl", msg.getAttachmentUrl());
                dto.put("isread", msg.getIsread());
                dto.put("createdAt", msg.getCreatedAt());
                dto.put("metadataJson", msg.getMetadataJson());
                return dto;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(messageDTOs);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @PostMapping("/sessions/{id}/accept")
    public ResponseEntity<?> acceptSession(@PathVariable Integer id,
                                           Authentication authentication) {
        try {
            Optional<NhanVien> currentEmployeeOpt = authenticatedActorService.getNhanVien(authentication);
            if (currentEmployeeOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "Không xác định được nhân viên hiện tại"));
            }
            NhanVien currentEmployee = currentEmployeeOpt.get();

            ChatSession session = chatSessionService.assignToEmployee(id, currentEmployee.getId(), currentEmployee.getTenNhanVien());
            if (session == null) {
                return ResponseEntity.notFound().build();
            }

            ChatMessage systemMessage = new ChatMessage();
            systemMessage.setChatSession(session);
            systemMessage.setSenderType("SYSTEM");
            systemMessage.setSenderName("System");
            systemMessage.setContent(String.format("Nhân viên %s đã tiếp nhận cuộc trò chuyện", currentEmployee.getTenNhanVien()));
            systemMessage.setMessageType("SYSTEM");
            systemMessage.setCreatedAt(LocalDateTime.now());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("sessionStatus", "IN_PROGRESS");
            metadata.put("assignedEmployeeName", currentEmployee.getTenNhanVien());
            systemMessage.setMetadataJson(objectMapper.writeValueAsString(metadata));
            chatMessageService.saveMessage(systemMessage);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã tiếp nhận cuộc trò chuyện",
                    "session", chatSessionService.getSessionDTO(session)
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không thể tiếp nhận cuộc trò chuyện"));
        }
    }

    @PostMapping("/sessions/{id}/reply")
    public ResponseEntity<?> replyToSession(@PathVariable Integer id,
                                            @RequestBody Map<String, Object> request,
                                            Authentication authentication) {
        try {
            String message = stringValue(request.get("message"));
            if (message == null || message.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Nội dung phản hồi không được để trống"));
            }

            Optional<ChatSession> sessionOpt = chatSessionService.getSessionById(id);
            if (sessionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            ChatSession session = sessionOpt.get();
            if (!canAccessSession(authentication, session)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "Không được trả lời phiên chat này"));
            }

            String employeeName = authenticatedActorService.getDisplayName(authentication);
            String employeeId = authenticatedActorService.getNhanVien(authentication)
                    .map(NhanVien::getId)
                    .map(String::valueOf)
                    .orElse("");

            ChatMessage employeeMessage = new ChatMessage();
            employeeMessage.setChatSession(session);
            employeeMessage.setSenderType("EMPLOYEE");
            employeeMessage.setSenderName(employeeName);
            employeeMessage.setSenderId(employeeId);
            employeeMessage.setContent(message.trim());
            employeeMessage.setMessageType("TEXT");
            employeeMessage.setIsread(false);
            employeeMessage.setCreatedAt(LocalDateTime.now());
            chatMessageService.saveMessage(employeeMessage);

            session.setLastMessageAt(LocalDateTime.now());
            session.setUpdatedAt(LocalDateTime.now());
            chatSessionService.saveSession(session);

            return ResponseEntity.ok(Map.of("success", true, "message", "Đã gửi tin nhắn"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không thể gửi tin nhắn"));
        }
    }

    @PostMapping("/sessions/{id}/close")
    public ResponseEntity<?> closeSession(@PathVariable Integer id,
                                          Authentication authentication) {
        try {
            Optional<ChatSession> sessionOpt = chatSessionService.getSessionById(id);
            if (sessionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            if (!canAccessSession(authentication, sessionOpt.get())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "Không được đóng phiên chat này"));
            }

            ChatSession session = chatSessionService.closeSession(id);
            if (session == null) {
                return ResponseEntity.notFound().build();
            }

            ChatMessage systemMessage = new ChatMessage();
            systemMessage.setChatSession(session);
            systemMessage.setSenderType("SYSTEM");
            systemMessage.setSenderName("System");
            systemMessage.setContent("Cuộc trò chuyện đã kết thúc. Cảm ơn anh/chị đã liên hệ với DirtyWave!");
            systemMessage.setMessageType("SYSTEM");
            systemMessage.setCreatedAt(LocalDateTime.now());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("sessionStatus", "CLOSED");
            systemMessage.setMetadataJson(objectMapper.writeValueAsString(metadata));
            chatMessageService.saveMessage(systemMessage);

            return ResponseEntity.ok(Map.of("success", true, "message", "Đã đóng cuộc trò chuyện"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không thể đóng cuộc trò chuyện"));
        }
    }

    @PostMapping("/sessions/{id}/mark-read")
    public ResponseEntity<?> markAsRead(@PathVariable Integer id,
                                        Authentication authentication) {
        try {
            Optional<ChatSession> sessionOpt = chatSessionService.getSessionById(id);
            if (sessionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            if (!canAccessSession(authentication, sessionOpt.get())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "Không được cập nhật phiên chat này"));
            }
            chatMessageService.markAllAsRead(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã đánh dấu đã đọc"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không thể đánh dấu đã đọc"));
        }
    }

    private boolean canAccessSession(Authentication authentication, ChatSession session) {
        if (authenticatedActorService.isAdmin(authentication)) {
            return true;
        }
        NhanVien employee = authenticatedActorService.getNhanVien(authentication).orElse(null);
        return canAccessSession(employee, session);
    }

    private boolean canAccessSession(NhanVien employee, ChatSession session) {
        if (employee == null || session == null) return false;
        String status = String.valueOf(session.getStatus() == null ? "" : session.getStatus()).trim().toUpperCase();
        Integer assignedEmployeeId = session.getAssignedEmployeeId();
        if (assignedEmployeeId == null) {
            return status.equals("OPEN") || status.equals("WAITING_EMPLOYEE");
        }
        return Objects.equals(employee.getId(), assignedEmployeeId);
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }
}
