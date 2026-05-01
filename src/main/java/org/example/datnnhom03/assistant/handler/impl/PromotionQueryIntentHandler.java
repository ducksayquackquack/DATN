package org.example.datnnhom03.assistant.handler.impl;

import org.example.datnnhom03.assistant.handler.AssistantHandlerResult;
import org.example.datnnhom03.assistant.handler.AssistantRequestContext;
import org.example.datnnhom03.assistant.handler.IntentHandler;
import org.example.datnnhom03.assistant.model.AssistantIntent;
import org.example.datnnhom03.assistant.service.ToolDispatcher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PromotionQueryIntentHandler extends AbstractToolIntentHandler implements IntentHandler {
    public PromotionQueryIntentHandler(ToolDispatcher toolDispatcher) { super(toolDispatcher); }
    @Override public AssistantIntent supportedIntent() { return AssistantIntent.PROMOTION_QUERY; }

    @Override
    public AssistantHandlerResult handle(AssistantRequestContext context) {
        AssistantHandlerResult result = AssistantHandlerResult.of("", false);
        Map<String, Object> data = call(context, "get_promotions", Map.of(), result);
        List<Map<String, Object>> items = items(data);
        result.setGrounded(!items.isEmpty());
        if (items.isEmpty()) {
            result.setReply("Hiện chưa có chương trình khuyến mãi đang hoạt động.");
            return result;
        }
        result.setReply("Hiện có " + items.size() + " chương trình ưu đãi đang hoạt động. Em có thể lọc tiếp theo mức giảm hoặc điều kiện áp dụng nếu anh/chị cần.");
        return result;
    }
}
