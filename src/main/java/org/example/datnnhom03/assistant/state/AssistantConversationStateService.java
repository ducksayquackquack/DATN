package org.example.datnnhom03.assistant.state;

import org.example.datnnhom03.assistant.dto.AssistantChatRequest;
import org.example.datnnhom03.assistant.model.AssistantIntent;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AssistantConversationStateService {

    private static final String[] TRACKED_KEYS = new String[]{
            "customerPhone", "customerEmail", "customerName", "orderCode", "productCode", "variantCode",
            "pageType", "actorEmployeeId", "actorEmail", "lastCustomerMessage", "currentReplyDraft",
            "shippingAddress", "paymentMethod", "productName", "productDescription", "productStatus",
            "targetStatus", "discountValue", "productId", "expectedDeliveryDate", "lastUserMessage",
            "quantity", "productHint", "heightCm", "weightKg"
    };

    private final Map<String, AssistantConversationState> stateStore = new ConcurrentHashMap<>();

    public void mergeIntoRequest(AssistantChatRequest request) {
        String sessionCode = normalizeSessionCode(request);
        AssistantConversationState state = stateStore.get(sessionCode);
        if (state == null) {
            return;
        }
        Map<String, Object> context = request.getContext();
        if (context == null) {
            context = new LinkedHashMap<>();
            request.setContext(context);
        }
        for (Map.Entry<String, Object> entry : state.getSlots().entrySet()) {
            context.putIfAbsent(entry.getKey(), entry.getValue());
        }
        if (state.getPendingActionToken() != null && !state.getPendingActionToken().isBlank()) {
            context.putIfAbsent("pendingActionToken", state.getPendingActionToken());
        }
    }

    public void remember(AssistantChatRequest request, AssistantIntent intent, String pageType) {
        String sessionCode = normalizeSessionCode(request);
        AssistantConversationState state = stateStore.computeIfAbsent(sessionCode, key -> {
            AssistantConversationState created = new AssistantConversationState();
            created.setSessionCode(key);
            return created;
        });
        state.setPageType(pageType);
        state.setLastIntent(intent);
        state.setUpdatedAt(Instant.now());

        Map<String, Object> context = request.getContext() == null ? Map.of() : request.getContext();
        if (request.getMessage() != null && !request.getMessage().isBlank()) {
            state.getSlots().put("lastUserMessage", request.getMessage().trim());
        }
        for (String key : TRACKED_KEYS) {
            Object value = context.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                state.getSlots().put(key, value);
            }
        }
    }

    public void rememberPendingActionToken(String sessionCode, String pendingActionToken) {
        AssistantConversationState state = stateStore.computeIfAbsent(sessionCode, key -> {
            AssistantConversationState created = new AssistantConversationState();
            created.setSessionCode(key);
            return created;
        });
        state.setPendingActionToken(pendingActionToken);
        state.setUpdatedAt(Instant.now());
    }

    public void clearPendingActionToken(String sessionCode) {
        AssistantConversationState state = stateStore.get(sessionCode);
        if (state != null) {
            state.setPendingActionToken(null);
            state.setUpdatedAt(Instant.now());
        }
    }

    public AssistantConversationState getState(String sessionCode) {
        return stateStore.get(sessionCode);
    }

    private String normalizeSessionCode(AssistantChatRequest request) {
        if (request.getSessionCode() != null && !request.getSessionCode().isBlank()) {
            return request.getSessionCode().trim();
        }
        return "AI-LOCAL";
    }
}
