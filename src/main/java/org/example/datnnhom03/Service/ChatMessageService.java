package org.example.datnnhom03.Service;

import org.example.datnnhom03.Model.ChatMessage;
import org.example.datnnhom03.Model.ChatSession;
import org.example.datnnhom03.Repository.ChatMessageRepository;
import org.example.datnnhom03.Repository.ChatSessionRepository;
import org.example.datnnhom03.dto.ChatMessageDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatMessageService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private ChatSessionService chatSessionService;

    /**
     * Save a new message to a chat session
     */
    public ChatMessage saveMessage(Integer sessionId, String senderType, String senderName, String content) {
        Optional<ChatSession> sessionOptional = chatSessionRepository.findById(sessionId);
        if (sessionOptional.isPresent()) {
            ChatSession session = sessionOptional.get();

            ChatMessage message = new ChatMessage();
            message.setChatSession(session);
            message.setSenderType(senderType); // CUSTOMER, EMPLOYEE, SYSTEM
            message.setSenderName(senderName);
            message.setContent(content);
            message.setMessageType("TEXT");
            message.setIsread(false);
            message.setCreatedAt(LocalDateTime.now());

            ChatMessage savedMessage = chatMessageRepository.save(message);

            // Update session's last message time
            chatSessionService.updateLastMessageTime(sessionId);

            return savedMessage;
        }
        return null;
    }

    /**
     * Save a message with attachment
     */
    public ChatMessage saveMessageWithAttachment(Integer sessionId, String senderType, String senderName, 
                                               String content, String attachmentUrl, String messageType) {
        Optional<ChatSession> sessionOptional = chatSessionRepository.findById(sessionId);
        if (sessionOptional.isPresent()) {
            ChatSession session = sessionOptional.get();

            ChatMessage message = new ChatMessage();
            message.setChatSession(session);
            message.setSenderType(senderType);
            message.setSenderName(senderName);
            message.setContent(content);
            message.setMessageType(messageType); // IMAGE, FILE, etc.
            message.setAttachmentUrl(attachmentUrl);
            message.setIsread(false);
            message.setCreatedAt(LocalDateTime.now());

            ChatMessage savedMessage = chatMessageRepository.save(message);

            // Update session's last message time
            chatSessionService.updateLastMessageTime(sessionId);

            return savedMessage;
        }
        return null;
    }

    /**
     * Get all messages in a session
     */
    public List<ChatMessage> getSessionMessages(Integer sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    /**
     * Get unread messages in a session
     */
    public List<ChatMessage> getUnreadMessages(Integer sessionId) {
        return chatMessageRepository.findBySessionIdAndIsreadFalse(sessionId);
    }

    /**
     * Mark a message as read
     */
    public ChatMessage markAsRead(Long messageId) {
        Optional<ChatMessage> optional = chatMessageRepository.findById(messageId);
        if (optional.isPresent()) {
            ChatMessage message = optional.get();
            message.setIsread(true);
            return chatMessageRepository.save(message);
        }
        return null;
    }

    /**
     * Mark all messages in a session as read
     */
    public void markAllAsRead(Integer sessionId) {
        List<ChatMessage> unreadMessages = getUnreadMessages(sessionId);
        unreadMessages.forEach(msg -> msg.setIsread(true));
        chatMessageRepository.saveAll(unreadMessages);
    }

    /**
     * Get count of unread messages in a session
     */
    public long getUnreadCount(Integer sessionId) {
        return chatMessageRepository.countBySessionIdAndIsreadFalse(sessionId);
    }

    /**
     * Search messages by content
     */
    public List<ChatMessage> searchMessages(Integer sessionId, String keyword) {
        return chatMessageRepository.searchMessagesByContent(sessionId, keyword);
    }

    /**
     * Get recent messages
     */
    public List<ChatMessage> getRecentMessages(Integer sessionId, Integer limit) {
        return chatMessageRepository.findRecentMessagesInSession(sessionId, limit);
    }

    /**
     * Get message by ID
     */
    public Optional<ChatMessage> getMessageById(Long messageId) {
        return chatMessageRepository.findById(messageId);
    }

    /**
     * Convert message to DTO
     */
    public ChatMessageDTO convertToDTO(ChatMessage message) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(message.getId());
        dto.setSessionId(message.getChatSession().getId());
        dto.setSenderType(message.getSenderType());
        dto.setSenderName(message.getSenderName());
        dto.setSenderId(message.getSenderId());
        dto.setContent(message.getContent());
        dto.setMessageType(message.getMessageType());
        dto.setAttachmentUrl(message.getAttachmentUrl());
        dto.setIsread(message.getIsread());
        dto.setMetadataJson(message.getMetadataJson());
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }

    /**
     * Convert messages to DTOs
     */
    public List<ChatMessageDTO> convertToDTOs(List<ChatMessage> messages) {
        return messages.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Save a message entity directly
     */
    public ChatMessage saveMessage(ChatMessage message) {
        if (message.getCreatedAt() == null) {
            message.setCreatedAt(LocalDateTime.now());
        }
        ChatMessage savedMessage = chatMessageRepository.save(message);
        
        // Update session's last message time
        if (message.getChatSession() != null && message.getChatSession().getId() != null) {
            chatSessionService.updateLastMessageTime(message.getChatSession().getId());
        }
        
        return savedMessage;
    }

    /**
     * Get messages by session ID
     */
    public List<ChatMessage> getMessagesBySessionId(Integer sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }
}
