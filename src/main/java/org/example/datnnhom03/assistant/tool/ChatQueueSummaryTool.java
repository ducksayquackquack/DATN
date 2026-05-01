package org.example.datnnhom03.assistant.tool;

import org.example.datnnhom03.Model.ChatSession;
import org.example.datnnhom03.Repository.ChatSessionRepository;
import org.example.datnnhom03.assistant.policy.AssistantRole;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class ChatQueueSummaryTool implements AssistantTool {

    private final ChatSessionRepository chatSessionRepository;

    public ChatQueueSummaryTool(ChatSessionRepository chatSessionRepository) {
        this.chatSessionRepository = chatSessionRepository;
    }

    @Override
    public String getName() {
        return "get_chat_queue_summary";
    }

    @Override
    public boolean supports(AssistantRole role) {
        return true;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args) {
        Integer employeeId = intValue(args.get("employeeId"));
        int limit = intValue(args.getOrDefault("limit", 5)) == null ? 5 : intValue(args.getOrDefault("limit", 5));

        List<ChatSession> allSessions = chatSessionRepository.findAll();
        List<ChatSession> scopedSessions = employeeId == null
                ? allSessions
                : allSessions.stream()
                .filter(session -> Objects.equals(session.getAssignedEmployeeId(), employeeId))
                .collect(Collectors.toList());

        Map<String, Long> countsByStatus = scopedSessions.stream()
                .collect(Collectors.groupingBy(session -> normalize(session.getStatus()), LinkedHashMap::new, Collectors.counting()));

        long waitingCount = countAny(scopedSessions, "WAITING_EMPLOYEE", "OPEN");
        long inProgressCount = countAny(scopedSessions, "IN_PROGRESS");
        long closedCount = countAny(scopedSessions, "CLOSED");
        long humanModeCount = scopedSessions.stream().filter(session -> "HUMAN".equals(normalize(session.getChatMode()))).count();
        long unassignedWaiting = employeeId == null
                ? scopedSessions.stream()
                .filter(session -> session.getAssignedEmployeeId() == null)
                .filter(session -> isAnyStatus(session, "WAITING_EMPLOYEE", "OPEN"))
                .count()
                : 0;

        List<Map<String, Object>> recentItems = scopedSessions.stream()
                .sorted(Comparator.comparing(ChatSession::getLastMessageAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ChatSession::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .map(this::toSessionRow)
                .collect(Collectors.toList());

        List<Map<String, Object>> employeeLoads = employeeId == null
                ? buildEmployeeLoads(allSessions, limit)
                : List.of();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scope", employeeId == null ? "ALL" : "EMPLOYEE");
        result.put("employeeId", employeeId);
        result.put("waitingCount", waitingCount);
        result.put("inProgressCount", inProgressCount);
        result.put("closedCount", closedCount);
        result.put("humanModeCount", humanModeCount);
        result.put("unassignedWaiting", unassignedWaiting);
        result.put("countsByStatus", countsByStatus);
        result.put("count", recentItems.size());
        result.put("items", recentItems);
        result.put("employeeLoads", employeeLoads);
        result.put("summary", buildSummary(employeeId, waitingCount, inProgressCount, closedCount, unassignedWaiting, employeeLoads));
        return result;
    }

    private List<Map<String, Object>> buildEmployeeLoads(List<ChatSession> allSessions, int limit) {
        Map<Integer, EmployeeLoadAggregate> aggregateMap = new LinkedHashMap<>();
        for (ChatSession session : allSessions) {
            if (session.getAssignedEmployeeId() == null) continue;
            EmployeeLoadAggregate aggregate = aggregateMap.computeIfAbsent(session.getAssignedEmployeeId(), key -> {
                EmployeeLoadAggregate row = new EmployeeLoadAggregate();
                row.employeeId = key;
                row.employeeName = safe(session.getAssignedEmployeeName());
                return row;
            });
            aggregate.totalSessions++;
            if (isAnyStatus(session, "IN_PROGRESS")) aggregate.inProgress++;
            if (isAnyStatus(session, "WAITING_EMPLOYEE", "OPEN")) aggregate.waiting++;
            LocalDateTime updatedAt = session.getLastMessageAt() != null ? session.getLastMessageAt() : session.getUpdatedAt();
            if (aggregate.lastTouch == null || (updatedAt != null && updatedAt.isAfter(aggregate.lastTouch))) {
                aggregate.lastTouch = updatedAt;
            }
        }

        return aggregateMap.values().stream()
                .sorted(Comparator.comparingInt(EmployeeLoadAggregate::busyScore).reversed())
                .limit(limit)
                .map(EmployeeLoadAggregate::toMap)
                .collect(Collectors.toList());
    }

    private Map<String, Object> toSessionRow(ChatSession session) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("sessionId", session.getId());
        row.put("sessionCode", session.getSessionCode());
        row.put("customerName", session.getCustomerName());
        row.put("customerEmail", session.getCustomerEmail());
        row.put("customerPhone", session.getCustomerPhone());
        row.put("status", session.getStatus());
        row.put("chatMode", session.getChatMode());
        row.put("assignedEmployeeId", session.getAssignedEmployeeId());
        row.put("assignedEmployeeName", session.getAssignedEmployeeName());
        row.put("lastMessageAt", session.getLastMessageAt());
        row.put("updatedAt", session.getUpdatedAt());
        return row;
    }

    private String buildSummary(Integer employeeId,
                                long waitingCount,
                                long inProgressCount,
                                long closedCount,
                                long unassignedWaiting,
                                List<Map<String, Object>> employeeLoads) {
        if (employeeId != null) {
            return "Bạn đang có " + waitingCount + " phiên cần theo dõi, "
                    + inProgressCount + " phiên đang xử lý và "
                    + closedCount + " phiên đã đóng gần đây.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Toàn hệ thống hiện có ")
                .append(waitingCount)
                .append(" phiên đang chờ/ mới mở, ")
                .append(inProgressCount)
                .append(" phiên đang xử lý và ")
                .append(closedCount)
                .append(" phiên đã đóng.");
        if (unassignedWaiting > 0) {
            sb.append(" Có ").append(unassignedWaiting).append(" phiên chưa gán nhân viên.");
        }
        if (!employeeLoads.isEmpty()) {
            Map<String, Object> busiest = employeeLoads.get(0);
            sb.append(" Nhân viên đang tải cao nhất: ")
                    .append(safe(busiest.get("employeeName")))
                    .append(" (").append(safe(busiest.get("totalSessions"))).append(" phiên).");
        }
        return sb.toString();
    }

    private long countAny(List<ChatSession> sessions, String... statuses) {
        return sessions.stream().filter(session -> isAnyStatus(session, statuses)).count();
    }

    private boolean isAnyStatus(ChatSession session, String... statuses) {
        String current = normalize(session.getStatus());
        for (String status : statuses) {
            if (current.equals(normalize(status))) return true;
        }
        return false;
    }

    private Integer intValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(String.valueOf(value).replaceAll("[^0-9-]", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private String normalize(Object value) {
        return safe(value).toUpperCase(Locale.ROOT);
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static class EmployeeLoadAggregate {
        Integer employeeId;
        String employeeName;
        int totalSessions;
        int inProgress;
        int waiting;
        LocalDateTime lastTouch;

        int busyScore() {
            return inProgress * 10 + waiting * 5 + totalSessions;
        }

        Map<String, Object> toMap() {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("employeeId", employeeId);
            row.put("employeeName", employeeName);
            row.put("totalSessions", totalSessions);
            row.put("inProgress", inProgress);
            row.put("waiting", waiting);
            row.put("lastTouch", lastTouch);
            return row;
        }
    }
}
