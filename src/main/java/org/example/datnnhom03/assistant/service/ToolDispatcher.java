package org.example.datnnhom03.assistant.service;

import org.example.datnnhom03.assistant.policy.AssistantRole;
import org.example.datnnhom03.assistant.tool.AssistantTool;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ToolDispatcher {

    private final Map<String, AssistantTool> toolMap = new HashMap<>();

    public ToolDispatcher(List<AssistantTool> tools) {
        for (AssistantTool tool : tools) {
            toolMap.put(tool.getName(), tool);
        }
    }

    public Map<String, Object> dispatch(String toolName, AssistantRole role, Map<String, Object> args) {
        AssistantTool tool = toolMap.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("Không tìm thấy tool: " + toolName);
        }
        if (!tool.supports(role)) {
            throw new IllegalStateException("Role " + role + " không được dùng tool " + toolName);
        }
        return tool.execute(args);
    }
}