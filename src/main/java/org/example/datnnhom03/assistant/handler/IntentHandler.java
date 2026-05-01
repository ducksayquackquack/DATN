package org.example.datnnhom03.assistant.handler;

import org.example.datnnhom03.assistant.model.AssistantIntent;

public interface IntentHandler {
    AssistantIntent supportedIntent();
    AssistantHandlerResult handle(AssistantRequestContext context);
}
