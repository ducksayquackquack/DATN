package org.example.datnnhom03.assistant.tool;

import org.example.datnnhom03.assistant.policy.AssistantRole;

import java.util.Map;

public interface AssistantTool {
    String getName();
    boolean supports(AssistantRole role);
    Map<String, Object> execute(Map<String, Object> args);
}