package org.example.datnnhom03.assistant.planner;

import org.example.datnnhom03.assistant.model.AssistantIntent;

import java.util.LinkedHashMap;
import java.util.Map;

public class AssistantExecutionPlan {
    private AssistantIntent detectedIntent;
    private AssistantIntent plannedIntent;
    private String confidence = "MEDIUM";
    private String route = "DETECTOR";
    private String reasoning = "";
    private boolean usedStateHints;
    private final Map<String, Object> extractedContext = new LinkedHashMap<>();

    public AssistantIntent getDetectedIntent() {
        return detectedIntent;
    }

    public void setDetectedIntent(AssistantIntent detectedIntent) {
        this.detectedIntent = detectedIntent;
    }

    public AssistantIntent getPlannedIntent() {
        return plannedIntent;
    }

    public void setPlannedIntent(AssistantIntent plannedIntent) {
        this.plannedIntent = plannedIntent;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public boolean isUsedStateHints() {
        return usedStateHints;
    }

    public void setUsedStateHints(boolean usedStateHints) {
        this.usedStateHints = usedStateHints;
    }

    public Map<String, Object> getExtractedContext() {
        return extractedContext;
    }
}
