package org.example.datnnhom03.assistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

@Service
public class OpenAiGateway {

    private static final Logger log = LoggerFactory.getLogger(OpenAiGateway.class);

    @Value("${assistant.openai.api-key:}")
    private String apiKey;

    @Value("${assistant.openai.model:gpt-4o-mini}")
    private String model;

    @Value("${assistant.openai.base-url:https://api.openai.com/v1/chat/completions}")
    private String baseUrl;

    @Value("${assistant.openai.temperature:0.25}")
    private double temperature;

    @Value("${assistant.demo-mode:true}")
    private boolean demoMode;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    public OpenAiGateway(RestTemplateBuilder restTemplateBuilder,
                         @Value("${assistant.openai.connect-timeout-ms:5000}") long connectTimeoutMs,
                         @Value("${assistant.openai.read-timeout-ms:20000}") long readTimeoutMs) {
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .readTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }

    public boolean isReady() {
        return !demoMode && apiKey != null && !apiKey.isBlank();
    }

    public String askAssistant(String systemPrompt, String userMessage, String toolContextText) {
        if (!isReady()) {
            log.debug("OpenAI gateway skipped because assistant is in demo mode or missing API key");
            return "";
        }
        String mergedUserPrompt = userMessage + "\n\nDỮ LIỆU NỘI BỘ:\n" + toolContextText;
        return chat(systemPrompt, mergedUserPrompt, temperature);
    }

    public String askChatbot(String systemPrompt, String userMessage, String groundedContextText) {
        if (!isReady()) {
            log.debug("OpenAI chatbot skipped because assistant is in demo mode or missing API key");
            return "";
        }
        String mergedUserPrompt = userMessage + "\n\nNGỮ CẢNH THỰC TẾ:\n" + groundedContextText;
        return chat(systemPrompt, mergedUserPrompt, Math.min(0.5d, temperature + 0.1d));
    }



    public String askStructured(String systemPrompt, String userMessage) {
        if (!isReady()) {
            log.debug("OpenAI structured call skipped because assistant is in demo mode or missing API key");
            return "";
        }
        return chat(systemPrompt, userMessage == null ? "" : userMessage, 0.0d);
    }

    private String chat(String systemPrompt, String userPrompt, double requestTemperature) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", model);
            payload.put("temperature", requestTemperature);

            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userPrompt));
            payload.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.POST, request, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("OpenAI gateway returned non-success status: {}", response.getStatusCode());
                return "";
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            return contentNode.asText("").trim();
        } catch (Exception e) {
            log.error("OpenAI gateway call failed", e);
            return "";
        }
    }
}
