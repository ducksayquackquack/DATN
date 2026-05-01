package org.example.datnnhom03.chatbot.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CustomerIntentResult {
    private String intent;
    private String confidence;
    private Map<String, Object> entities = new LinkedHashMap<>();
    private List<String> missingFields = new ArrayList<>();
    private boolean clarificationNeeded;

    public static CustomerIntentResult of(String intent, String confidence) {
        CustomerIntentResult result = new CustomerIntentResult();
        result.setIntent(intent);
        result.setConfidence(confidence);
        return result;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public Map<String, Object> getEntities() {
        return entities;
    }

    public void setEntities(Map<String, Object> entities) {
        this.entities = entities == null ? new LinkedHashMap<>() : entities;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields == null ? new ArrayList<>() : missingFields;
    }

    public boolean isClarificationNeeded() {
        return clarificationNeeded;
    }

    public void setClarificationNeeded(boolean clarificationNeeded) {
        this.clarificationNeeded = clarificationNeeded;
    }
}
