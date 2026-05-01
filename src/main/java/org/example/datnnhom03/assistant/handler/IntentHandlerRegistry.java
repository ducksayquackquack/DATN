package org.example.datnnhom03.assistant.handler;

import org.example.datnnhom03.assistant.model.AssistantIntent;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class IntentHandlerRegistry {
    private final Map<AssistantIntent, IntentHandler> handlers = new EnumMap<>(AssistantIntent.class);

    public IntentHandlerRegistry(List<IntentHandler> handlerList) {
        for (IntentHandler handler : handlerList) {
            handlers.put(handler.supportedIntent(), handler);
        }
    }

    public IntentHandler getHandler(AssistantIntent intent) {
        return handlers.get(intent);
    }
}
