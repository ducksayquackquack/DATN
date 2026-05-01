package org.example.datnnhom03.Service;

import org.example.datnnhom03.Model.ChatSession;
import org.example.datnnhom03.Model.ChatSessionEvent;
import org.example.datnnhom03.Model.NhanVien;
import org.example.datnnhom03.Repository.ChatSessionEventRepository;
import org.example.datnnhom03.Repository.ChatSessionRepository;
import org.example.datnnhom03.Repository.NhanVienRepository;
import org.example.datnnhom03.dto.ChatSessionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatSessionService {

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private ChatSessionEventRepository chatSessionEventRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    /**
     * Create a new chat session
     */
    public ChatSession createSession(String customerEmail, String customerName, String customerPhone) {
        String sessionCode = "CS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        ChatSession session = new ChatSession();
        session.setSessionCode(sessionCode);
        session.setCustomerEmail(customerEmail);
        session.setCustomerName(customerName);
        session.setCustomerPhone(customerPhone);
        session.setStatus("OPEN");
        session.setChatMode("AUTO");
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());

        ChatSession savedSession = chatSessionRepository.save(session);
        logEvent(savedSession, "SESSION_CREATED", "SYSTEM", "Customer", null);
        return savedSession;
    }

    public Optional<ChatSession> getSessionById(Integer sessionId) {
        return chatSessionRepository.findById(sessionId);
    }

    public List<ChatSession> getCustomerSessions(String customerEmail) {
        return chatSessionRepository.findByCustomerEmail(customerEmail);
    }

    public List<ChatSession> getActiveSessions() {
        return chatSessionRepository.findByStatusIn(Arrays.asList(
                "OPEN",
                "WAITING_EMPLOYEE",
                "IN_PROGRESS"
        ));
    }

    public List<ChatSession> getEmployeeSessions(Integer employeeId) {
        return chatSessionRepository.findByAssignedEmployeeId(employeeId);
    }

    public ChatSession assignToEmployee(Integer sessionId, Integer employeeId, String employeeName) {
        Optional<ChatSession> optional = chatSessionRepository.findById(sessionId);
        if (optional.isEmpty()) {
            return null;
        }

        ChatSession session = optional.get();
        session.setStatus("IN_PROGRESS");
        session.setChatMode("HUMAN");
        session.setAcceptedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        session.setLastMessageAt(LocalDateTime.now());

        if (employeeId != null) {
            nhanVienRepository.findById(employeeId).ifPresent(session::setAssignedEmployee);
        }

        if (employeeName != null && !employeeName.isBlank()) {
            session.setAssignedEmployeeName(employeeName.trim());
        } else if (session.getAssignedEmployee() != null && session.getAssignedEmployee().getTenNhanVien() != null) {
            session.setAssignedEmployeeName(session.getAssignedEmployee().getTenNhanVien());
        }

        ChatSession updated = chatSessionRepository.save(session);

        String actorId = employeeId == null ? null : employeeId.toString();
        String actorName = updated.getAssignedEmployeeName() == null ? "Nhân viên" : updated.getAssignedEmployeeName();
        logEvent(updated, "ACCEPTED", "EMPLOYEE", actorName, actorId);

        return updated;
    }

    public ChatSession closeSession(Integer sessionId) {
        Optional<ChatSession> optional = chatSessionRepository.findById(sessionId);
        if (optional.isPresent()) {
            ChatSession session = optional.get();
            session.setStatus("CLOSED");
            session.setUpdatedAt(LocalDateTime.now());

            ChatSession updated = chatSessionRepository.save(session);
            logEvent(updated, "CLOSED", "SYSTEM", "System", null);
            return updated;
        }
        return null;
    }

    public void updateLastMessageTime(Integer sessionId) {
        Optional<ChatSession> optional = chatSessionRepository.findById(sessionId);
        if (optional.isPresent()) {
            ChatSession session = optional.get();
            session.setLastMessageAt(LocalDateTime.now());
            chatSessionRepository.save(session);
        }
    }

    public void logEvent(ChatSession session, String eventType, String actorType, String actorName, String actorId) {
        ChatSessionEvent event = new ChatSessionEvent();
        event.setChatSession(session);
        event.setEventType(eventType);
        event.setActorType(actorType);
        event.setActorName(actorName);
        event.setActorId(actorId);
        event.setCreatedAt(LocalDateTime.now());

        chatSessionEventRepository.save(event);
    }

    public ChatSessionDTO getSessionDTO(ChatSession session) {
        ChatSessionDTO dto = new ChatSessionDTO();
        dto.setId(session.getId());
        dto.setSessionCode(session.getSessionCode());
        dto.setCustomerName(session.getCustomerName());
        dto.setCustomerEmail(session.getCustomerEmail());
        dto.setCustomerPhone(session.getCustomerPhone());
        dto.setAssignedEmployeeId(session.getAssignedEmployeeId());
        dto.setAssignedEmployeeName(session.getAssignedEmployeeName());
        dto.setStatus(session.getStatus());
        dto.setChatMode(session.getChatMode());
        dto.setAcceptedAt(session.getAcceptedAt());
        dto.setLastMessageAt(session.getLastMessageAt());
        dto.setCreatedAt(session.getCreatedAt());
        dto.setUpdatedAt(session.getUpdatedAt());
        return dto;
    }

    public List<ChatSessionDTO> getAllSessionsDTO() {
        return chatSessionRepository.findAll().stream()
                .map(this::getSessionDTO)
                .collect(Collectors.toList());
    }

    public ChatSession getSessionByCode(String sessionCode) {
        Optional<ChatSession> optional = chatSessionRepository.findBySessionCode(sessionCode);
        return optional.orElse(null);
    }

    public ChatSession saveSession(ChatSession session) {
        return chatSessionRepository.save(session);
    }
}
