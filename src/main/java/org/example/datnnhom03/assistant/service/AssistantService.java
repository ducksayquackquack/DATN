package org.example.datnnhom03.assistant.service;

import org.example.datnnhom03.assistant.action.AssistantActionExecutionService;
import org.example.datnnhom03.assistant.action.AssistantPendingActionService;
import org.example.datnnhom03.assistant.action.PendingAssistantAction;
import org.example.datnnhom03.assistant.audit.AssistantActionAuditService;
import org.example.datnnhom03.assistant.dto.AssistantChatRequest;
import org.example.datnnhom03.assistant.dto.AssistantChatResponse;
import org.example.datnnhom03.assistant.handler.AssistantHandlerResult;
import org.example.datnnhom03.assistant.handler.AssistantRequestContext;
import org.example.datnnhom03.assistant.handler.IntentHandler;
import org.example.datnnhom03.assistant.handler.IntentHandlerRegistry;
import org.example.datnnhom03.assistant.model.AssistantIntent;
import org.example.datnnhom03.assistant.planner.AssistantExecutionPlan;
import org.example.datnnhom03.assistant.planner.AssistantPlannerService;
import org.example.datnnhom03.assistant.policy.AssistantPermissionService;
import org.example.datnnhom03.assistant.recommendation.AssistantRecommendationService;
import org.example.datnnhom03.assistant.policy.AssistantRole;
import org.example.datnnhom03.assistant.state.AssistantConversationState;
import org.example.datnnhom03.assistant.state.AssistantConversationStateService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AssistantService {

    private final AssistantPermissionService permissionService;
    private final OpenAiGateway openAiGateway;
    private final AssistantIntentDetector intentDetector;
    private final AssistantConversationStateService conversationStateService;
    private final IntentHandlerRegistry handlerRegistry;
    private final AssistantSlotFillingService slotFillingService;
    private final AssistantActionPolicyService actionPolicyService;
    private final AssistantPendingActionService pendingActionService;
    private final AssistantActionExecutionService actionExecutionService;
    private final AssistantPlannerService plannerService;
    private final AssistantRecommendationService recommendationService;
    private final AssistantActionSafetyService actionSafetyService;
    private final AssistantActionAuditService actionAuditService;

    public AssistantService(AssistantPermissionService permissionService,
                            OpenAiGateway openAiGateway,
                            AssistantIntentDetector intentDetector,
                            AssistantConversationStateService conversationStateService,
                            IntentHandlerRegistry handlerRegistry,
                            AssistantSlotFillingService slotFillingService,
                            AssistantActionPolicyService actionPolicyService,
                            AssistantPendingActionService pendingActionService,
                            AssistantActionExecutionService actionExecutionService,
                            AssistantPlannerService plannerService,
                            AssistantRecommendationService recommendationService,
                            AssistantActionSafetyService actionSafetyService,
                            AssistantActionAuditService actionAuditService) {
        this.permissionService = permissionService;
        this.openAiGateway = openAiGateway;
        this.intentDetector = intentDetector;
        this.conversationStateService = conversationStateService;
        this.handlerRegistry = handlerRegistry;
        this.slotFillingService = slotFillingService;
        this.actionPolicyService = actionPolicyService;
        this.pendingActionService = pendingActionService;
        this.actionExecutionService = actionExecutionService;
        this.plannerService = plannerService;
        this.recommendationService = recommendationService;
        this.actionSafetyService = actionSafetyService;
        this.actionAuditService = actionAuditService;
    }

    public AssistantChatResponse chat(AssistantChatRequest request) {
        permissionService.validateAssistantEnabled();
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return AssistantChatResponse.fail("Nội dung không được để trống");
        }

        AssistantRole role = AssistantRole.from(request.getRole());
        permissionService.validateRoleAllowed(role);
        conversationStateService.mergeIntoRequest(request);

        String pageType = resolvePageType(request);
        String normalized = normalize(request.getMessage());
        String sessionCode = fallbackSessionCode(request);
        AssistantConversationState state = conversationStateService.getState(sessionCode);

        Map<String, Object> slotValues = extractStructuredSlotValues(request);
        if (!slotValues.isEmpty()) {
            request.getContext().putAll(slotValues);
        }

        AssistantIntent detectedIntent;
        AssistantExecutionPlan executionPlan;
        AssistantIntent intent;

        if (shouldContinuePreviousIntent(slotValues, state, normalized)) {
            detectedIntent = state.getLastIntent();
            intent = state.getLastIntent();
            executionPlan = new AssistantExecutionPlan();
            executionPlan.setDetectedIntent(detectedIntent);
            executionPlan.setPlannedIntent(intent);
            executionPlan.setConfidence("HIGH");
            executionPlan.setRoute("STRUCTURED_SLOT_CONTINUE");
            executionPlan.setReasoning("Tiếp tục luồng trước đó vì request gửi slotValues có cấu trúc.");
            executionPlan.setUsedStateHints(true);
            executionPlan.getExtractedContext().putAll(slotValues);
        } else {
            detectedIntent = intentDetector.detect(normalized, pageType);
            executionPlan = plannerService.plan(request, normalized, pageType, role, detectedIntent, state);
            if (!executionPlan.getExtractedContext().isEmpty()) {
                request.getContext().putAll(executionPlan.getExtractedContext());
            }
            intent = executionPlan.getPlannedIntent() == null ? detectedIntent : executionPlan.getPlannedIntent();
        }

        slotFillingService.enrichRequestContext(request, intent);

        if (intent == AssistantIntent.CONFIRM_ACTION) {
            return confirmPendingAction(request, role, pageType, sessionCode, intent);
        }
        if (intent == AssistantIntent.CANCEL_ACTION) {
            return cancelPendingAction(request, role, pageType, sessionCode, intent);
        }

        if (intent == AssistantIntent.BLOCKED_ACTION || isBlockedActionIntent(normalized)) {
            AssistantChatResponse blocked = baseResponse(request, role, buildBlockedActionMessage(pageType));
            blocked.setIntent(AssistantIntent.BLOCKED_ACTION.wireValue());
            blocked.setConfidence("HIGH");
            blocked.setGrounded(true);
            blocked.setHandoffSuggested(true);
            blocked.setQuickReplies(defaultQuickReplies(role, pageType, AssistantIntent.BLOCKED_ACTION));
            blocked.setSuggestedActions(List.of(
                    Map.of("type", "handoff", "label", "Chuyển người phụ trách xác nhận", "priority", "high"),
                    Map.of("type", "explain", "label", "Giải thích quy trình an toàn", "priority", "medium")
            ));
            blocked.setDebug(buildDebugInfo(request, pageType, AssistantIntent.BLOCKED_ACTION, true, 0, List.of(), null));
            conversationStateService.remember(request, AssistantIntent.BLOCKED_ACTION, pageType);
            return blocked;
        }

        if (!permissionService.isIntentAllowed(role, intent)) {
            AssistantChatResponse restricted = baseResponse(request, role, permissionService.buildRoleRestrictionMessage(role, intent));
            restricted.setIntent(AssistantIntent.ROLE_RESTRICTED.wireValue());
            restricted.setConfidence("HIGH");
            restricted.setGrounded(true);
            restricted.setQuickReplies(defaultQuickReplies(role, pageType, AssistantIntent.ROLE_RESTRICTED));
            restricted.setDebug(buildDebugInfo(request, pageType, AssistantIntent.ROLE_RESTRICTED, true, 0, List.of(), null));
            conversationStateService.remember(request, AssistantIntent.ROLE_RESTRICTED, pageType);
            return restricted;
        }

        List<String> missingSlots = slotFillingService.findMissingSlots(intent, request);
        if (!missingSlots.isEmpty()) {
            AssistantChatResponse ask = baseResponse(request, role, slotFillingService.buildMissingSlotQuestion(intent, missingSlots));
            ask.setIntent(intent.wireValue());
            ask.setConfidence("MEDIUM");
            ask.setGrounded(false);
            ask.setMissingSlots(missingSlots);
            ask.setQuickReplies(slotQuickReplies(intent, missingSlots));
            ask.setSuggestedActions(actionPolicyService.buildSuggestedActions(intent));
            ask.setWarnings(List.of());
            ask.setDebug(buildDebugInfo(request, pageType, intent, false, 0, missingSlots, executionPlan));
            conversationStateService.remember(request, intent, pageType);
            return ask;
        }

        state = conversationStateService.getState(sessionCode);
        AssistantRequestContext context = new AssistantRequestContext(request, role, intent, pageType, normalized, state);
        context.getMetadata().put("plannerRoute", executionPlan.getRoute());
        context.getMetadata().put("plannerConfidence", executionPlan.getConfidence());
        context.getMetadata().put("plannerReasoning", executionPlan.getReasoning());
        IntentHandler handler = handlerRegistry.getHandler(intent);
        AssistantHandlerResult result = handler != null
                ? handler.handle(context)
                : AssistantHandlerResult.of(buildGeneralFallbackReply(role, pageType, request.getMessage()), false);

        String finalMessage = maybePolishDraft(intent, role, pageType, request, result);
        AssistantChatResponse response = baseResponse(request, role, finalMessage);
        applyResultToResponse(request, pageType, intent, missingSlots, result, response, executionPlan);
        conversationStateService.remember(request, intent, pageType);
        if (stringValue(result.getMetadata().get("pendingActionToken")).length() > 0) {
            conversationStateService.rememberPendingActionToken(sessionCode, stringValue(result.getMetadata().get("pendingActionToken")));
        }
        return response;
    }

    private AssistantChatResponse confirmPendingAction(AssistantChatRequest request,
                                                       AssistantRole role,
                                                       String pageType,
                                                       String sessionCode,
                                                       AssistantIntent intent) {
        String token = stringValue(request.getContext().get("pendingActionToken"));
        if (token.isBlank()) {
            AssistantConversationState state = conversationStateService.getState(sessionCode);
            token = state == null ? "" : stringValue(state.getPendingActionToken());
        }
        PendingAssistantAction pending = pendingActionService.consume(sessionCode, token);
        AssistantHandlerResult result = actionExecutionService.execute(pending);
        AssistantChatResponse response = baseResponse(request, role, result.getReply());
        applyResultToResponse(request, pageType, intent, List.of(), result, response, null);
        response.setActionStatus(stringValue(result.getMetadata().getOrDefault("actionStatus", pending == null ? "NO_PENDING_ACTION" : "EXECUTED")));
        response.setRequiresConfirmation(false);
        response.setConfirmationMessage("");
        response.setPendingActionToken("");
        conversationStateService.clearPendingActionToken(sessionCode);
        conversationStateService.remember(request, intent, pageType);
        return response;
    }

    private AssistantChatResponse cancelPendingAction(AssistantChatRequest request,
                                                      AssistantRole role,
                                                      String pageType,
                                                      String sessionCode,
                                                      AssistantIntent intent) {
        PendingAssistantAction canceled = pendingActionService.cancel(sessionCode);
        conversationStateService.clearPendingActionToken(sessionCode);
        String cancelMessage = canceled == null ? "Hiện không có thao tác chờ để hủy." : "Em đã hủy thao tác nháp đang chờ xác nhận.";
        AssistantChatResponse response = baseResponse(request, role, cancelMessage);
        response.setIntent(intent.wireValue());
        response.setGrounded(true);
        response.setConfidence("HIGH");
        response.setActionStatus(canceled == null ? "NO_PENDING_ACTION" : "CANCELED");
        response.setAuditId(actionAuditService.recordCancellation(canceled, cancelMessage));
        response.setQuickReplies(defaultQuickReplies(role, pageType, intent));
        response.setDebug(buildDebugInfo(request, pageType, intent, true, 0, List.of(), null));
        conversationStateService.remember(request, intent, pageType);
        return response;
    }

    private void applyResultToResponse(AssistantChatRequest request,
                                       String pageType,
                                       AssistantIntent intent,
                                       List<String> missingSlots,
                                       AssistantHandlerResult result,
                                       AssistantChatResponse response,
                                       AssistantExecutionPlan executionPlan) {
        response.setIntent(intent.wireValue());
        response.setGrounded(result.isGrounded());
        response.setConfidence(resolveConfidence(result));
        response.setToolCalls(result.getToolCalls());
        response.setQuickReplies(defaultQuickReplies(AssistantRole.from(request.getRole()), pageType, intent));
        response.setData(new LinkedHashMap<>(result.getPayload()));

        List<Map<String, Object>> suggestedActions = new ArrayList<>();
        if (!result.getSuggestedActions().isEmpty()) {
            suggestedActions.addAll(result.getSuggestedActions());
        }
        if (suggestedActions.isEmpty()) {
            suggestedActions.addAll(actionPolicyService.buildSuggestedActions(intent));
        }
        suggestedActions.addAll(recommendationService.recommend(request, AssistantRole.from(request.getRole()), pageType, intent, result, conversationStateService.getState(fallbackSessionCode(request))));
        response.setSuggestedActions(dedupeActions(suggestedActions));

        boolean requiresConfirmation = actionPolicyService.requiresConfirmation(intent, result.getPayload())
                && stringValue(result.getMetadata().get("pendingActionToken")).length() > 0;
        response.setRequiresConfirmation(requiresConfirmation);
        response.setConfirmationMessage(requiresConfirmation ? actionPolicyService.confirmationMessage(intent) : "");
        response.setPendingActionToken(stringValue(result.getMetadata().get("pendingActionToken")));
        response.setActionStatus(stringValue(result.getMetadata().getOrDefault("actionStatus", requiresConfirmation ? "PENDING_CONFIRMATION" : "COMPLETED")));
        response.setAuditId(stringValue(result.getMetadata().get("auditId")));
        response.setWarnings(stringList(result.getMetadata().get("warnings")));
        response.setHandoffSuggested(shouldSuggestHandoff(pageType, intent, result.isGrounded(), normalize(request.getMessage()))
                || "HANDOFF_REQUIRED".equalsIgnoreCase(response.getActionStatus()));
        response.setDebug(buildDebugInfo(request, pageType, intent, result.isGrounded(), result.getToolCalls().size(), missingSlots, executionPlan));
        if (result.getPayload().containsKey("safety")) {
            response.getDebug().put("safety", result.getPayload().get("safety"));
        }
    }

    private String maybePolishDraft(AssistantIntent intent,
                                    AssistantRole role,
                                    String pageType,
                                    AssistantChatRequest request,
                                    AssistantHandlerResult result) {
        if (intent != AssistantIntent.CUSTOMER_REPLY_DRAFT || !result.isGrounded() || !openAiGateway.isReady()) {
            return result.getReply();
        }
        String systemPrompt = "Bạn là trợ lý bán hàng nội bộ. Viết lại câu trả lời cho khách ngắn gọn, lịch sự, không bịa thêm chính sách. Nếu dữ liệu nội bộ có mã đơn/trạng thái/tồn kho thì giữ nguyên.";
        String polished = openAiGateway.askAssistant(systemPrompt, request.getMessage(), result.getReply());
        return polished == null || polished.isBlank() ? result.getReply() : polished.trim();
    }

    private String resolveConfidence(AssistantHandlerResult result) {
        if (!result.isGrounded()) return "LOW";
        return result.getToolCalls().size() >= 2 ? "HIGH" : "MEDIUM";
    }

    private AssistantChatResponse baseResponse(AssistantChatRequest request, AssistantRole role, String message) {
        return AssistantChatResponse.ok(fallbackSessionCode(request), role.name(), message);
    }

    private String resolvePageType(AssistantChatRequest request) {
        Map<String, Object> context = request.getContext() == null ? Map.of() : request.getContext();
        String pageType = stringValue(context.get("pageType"));
        if (!pageType.isBlank()) return pageType;
        String source = stringValue(request.getSource()).toUpperCase(Locale.ROOT);
        if (source.contains("CHAT")) return "CUSTOMER_CHAT";
        if (source.contains("ORDER")) return "ORDER_DETAIL";
        if (source.contains("ADMIN")) return "ADMIN_DASHBOARD";
        if (source.contains("EMPLOYEE")) return "EMPLOYEE_PANEL";
        return "GENERAL";
    }

    private boolean shouldSuggestHandoff(String pageType, AssistantIntent intent, boolean grounded, String normalized) {
        if (intent == AssistantIntent.BLOCKED_ACTION) return true;
        if (containsAny(normalized, "khiếu nại", "khieu nai", "luật", "pháp lý", "refund", "hoàn tiền")) return true;
        return !grounded && "CUSTOMER_CHAT".equals(pageType);
    }

    private boolean isBlockedActionIntent(String normalized) {
        return containsAny(normalized, "hoàn tiền", "refund", "hủy đơn", "huy don", "xóa khách", "xoa khach", "xóa tài khoản", "xoa tai khoan", "sửa tồn", "sua ton", "đổi vai trò", "doi vai tro");
    }

    private String buildBlockedActionMessage(String pageType) {
        if ("CUSTOMER_CHAT".equals(pageType)) {
            return "Yêu cầu này cần người phụ trách xác nhận trực tiếp để đảm bảo an toàn. Em có thể giúp anh/chị soạn lời giải thích cho khách hoặc chuẩn bị thông tin bàn giao.";
        }
        return "Yêu cầu này đang nằm trong nhóm thao tác nhạy cảm. Em có thể hỗ trợ tra cứu, giải thích quy trình hoặc chuẩn bị nội dung để người phụ trách xác nhận.";
    }

    private String buildGeneralFallbackReply(AssistantRole role, String pageType, String message) {
        if ("CUSTOMER_CHAT".equals(pageType)) {
            return "Em đã hiểu yêu cầu. Nếu anh/chị gửi thêm mã đơn, tin nhắn khách hoặc sản phẩm cần kiểm tra, em sẽ hỗ trợ sát hơn ngay.";
        }
        if (role == AssistantRole.ADMIN) {
            return "Em sẵn sàng hỗ trợ admin tra cứu vận hành, doanh thu, đơn trễ, tồn kho, khách hàng và chuẩn bị action an toàn. Anh/chị nói rõ mục tiêu cần kiểm tra là em đi tiếp ngay.";
        }
        return "Em sẵn sàng hỗ trợ tra cứu đơn, kiểm tra tồn kho, xem lịch sử khách, soạn phản hồi cho khách và chuẩn bị thao tác nghiệp vụ an toàn. Anh/chị nói rõ thêm thông tin cần kiểm tra nhé.";
    }

    private Map<String, Object> buildDebugInfo(AssistantChatRequest request,
                                               String pageType,
                                               AssistantIntent intent,
                                               boolean grounded,
                                               int toolCount,
                                               List<String> missingSlots,
                                               AssistantExecutionPlan executionPlan) {
        AssistantConversationState state = conversationStateService.getState(fallbackSessionCode(request));
        Map<String, Object> debug = new LinkedHashMap<>();
        debug.put("pageType", pageType);
        debug.put("intent", intent.wireValue());
        debug.put("grounded", grounded);
        debug.put("toolCount", toolCount);
        debug.put("missingSlots", missingSlots);
        debug.put("stateAvailable", state != null);
        if (state != null) {
            debug.put("stateLastIntent", state.getLastIntent() == null ? "" : state.getLastIntent().wireValue());
            debug.put("stateSlotKeys", new ArrayList<>(state.getSlots().keySet()));
            debug.put("pendingActionToken", stringValue(state.getPendingActionToken()));
        }
        if (executionPlan != null) {
            Map<String, Object> planner = new LinkedHashMap<>();
            planner.put("detectedIntent", executionPlan.getDetectedIntent() == null ? "" : executionPlan.getDetectedIntent().wireValue());
            planner.put("plannedIntent", executionPlan.getPlannedIntent() == null ? "" : executionPlan.getPlannedIntent().wireValue());
            planner.put("route", executionPlan.getRoute());
            planner.put("confidence", executionPlan.getConfidence());
            planner.put("reasoning", executionPlan.getReasoning());
            planner.put("usedStateHints", executionPlan.isUsedStateHints());
            planner.put("extractedContext", executionPlan.getExtractedContext());
            debug.put("planner", planner);
        }
        return debug;
    }

    private List<String> defaultQuickReplies(AssistantRole role, String pageType, AssistantIntent intent) {
        if (intent == AssistantIntent.CONFIRM_ACTION || intent == AssistantIntent.CANCEL_ACTION) {
            return List.of("Tạo thao tác mới", "Xem lại payload", "Tiếp tục tra cứu", "Hỏi việc khác");
        }
        if ("CUSTOMER_CHAT".equals(pageType)) {
            if (intent == AssistantIntent.CUSTOMER_REPLY_DRAFT) {
                return List.of("Viết mềm hơn", "Rút gọn còn 2 câu", "Thêm upsell nhẹ", "Chèn thông tin đơn hàng");
            }
            return List.of("Khách này từng mua chưa?", "Tóm tắt khách đang chat", "Viết giúp tôi câu trả lời", "Kiểm tra tồn kho mẫu này");
        }
        if (role == AssistantRole.ADMIN) {
            return List.of("Doanh thu tháng này", "Đơn trễ cần xử lý", "Top sản phẩm", "Tồn thấp");
        }
        return List.of("Tra cứu đơn hàng", "Kiểm tra tồn kho", "Khách này từng mua chưa?", "Soạn câu trả lời cho khách");
    }

    private List<String> slotQuickReplies(AssistantIntent intent, List<String> missingSlots) {
        if (missingSlots.contains("orderCodeOrPhone") || missingSlots.contains("orderCode")) {
            return List.of("Mã đơn HD...", "Số điện thoại nhận hàng");
        }
        if (missingSlots.contains("customerIdentity") || missingSlots.contains("customerPhone")) {
            return List.of("Số điện thoại khách", "Email khách", "Tên khách");
        }
        if (missingSlots.contains("heightCm") || missingSlots.contains("weightKg")) {
            return List.of("165cm 55kg", "170cm 65kg", "175cm 75kg");
        }
        if (missingSlots.contains("discountValue")) {
            return List.of("10%", "15%", "50000đ");
        }
        return defaultQuickReplies(AssistantRole.EMPLOYEE, "GENERAL", intent);
    }

    private List<Map<String, Object>> dedupeActions(List<Map<String, Object>> actions) {
        List<Map<String, Object>> deduped = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        for (Map<String, Object> action : actions) {
            String key = stringValue(action.get("type")) + "|" + stringValue(action.get("label"));
            if (!keys.contains(key)) {
                keys.add(key);
                deduped.add(action);
            }
        }
        return deduped;
    }

    private List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object item : list) {
                String text = stringValue(item);
                if (!text.isBlank()) out.add(text);
            }
            return out;
        }
        return List.of();
    }

    private Map<String, Object> extractStructuredSlotValues(AssistantChatRequest request) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (request == null || request.getContext() == null) {
            return out;
        }
        Object token = request.getPendingActionToken();
        if (token != null && !String.valueOf(token).isBlank()) {
            request.getContext().putIfAbsent("pendingActionToken", String.valueOf(token).trim());
        }
        Object raw = request.getContext().get("slotValues");
        if (raw instanceof Map<?, ?> rawMap) {
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) continue;
                String key = String.valueOf(entry.getKey()).trim();
                if (key.isBlank()) continue;
                Object value = entry.getValue();
                if (value instanceof String s) {
                    String trimmed = s.trim();
                    if (!trimmed.isBlank()) {
                        out.put(key, trimmed);
                    }
                } else {
                    out.put(key, value);
                }
            }
        }
        return out;
    }

    private boolean shouldContinuePreviousIntent(Map<String, Object> slotValues,
                                                 AssistantConversationState state,
                                                 String normalized) {
        if (slotValues == null || slotValues.isEmpty() || state == null || state.getLastIntent() == null) {
            return false;
        }
        if (state.getLastIntent() == AssistantIntent.CONFIRM_ACTION || state.getLastIntent() == AssistantIntent.CANCEL_ACTION) {
            return false;
        }
        return normalized.isBlank()
                || "continue_pending_action".equalsIgnoreCase(normalized)
                || containsAny(normalized, "chiều cao", "chieu cao", "cân nặng", "can nang", "mã hoặc gợi ý sản phẩm", "so luong", "số lượng");
    }

    private String fallbackSessionCode(AssistantChatRequest request) {
        return request.getSessionCode() != null && !request.getSessionCode().isBlank()
                ? request.getSessionCode().trim()
                : "AI-LOCAL";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String text, String... keywords) {
        String normalizedText = normalize(text);
        for (String keyword : keywords) {
            if (!normalize(keyword).isBlank() && normalizedText.contains(normalize(keyword))) {
                return true;
            }
        }
        return false;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
