package org.example.datnnhom03.assistant.controller;

import org.example.datnnhom03.assistant.dto.AssistantChatRequest;
import org.example.datnnhom03.assistant.dto.AssistantChatResponse;
import org.example.datnnhom03.assistant.service.AssistantService;
import org.example.datnnhom03.security.AuthenticatedActorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"}, allowCredentials = "true")
public class AssistantApiController {

    private final AssistantService assistantService;
    private final AuthenticatedActorService authenticatedActorService;

    public AssistantApiController(AssistantService assistantService,
                                  AuthenticatedActorService authenticatedActorService) {
        this.assistantService = assistantService;
        this.authenticatedActorService = authenticatedActorService;
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok().body(Map.of(
                "status", "ok",
                "service", "DirtyWave AI Assistant"
        ));
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody AssistantChatRequest request,
                                  Authentication authentication) {
        try {
            request.setRole(authenticatedActorService.getNormalizedRole(authentication));
            if (request.getContext() == null) {
                request.setContext(new LinkedHashMap<>());
            }
            request.getContext().putIfAbsent("actorName", authenticatedActorService.getDisplayName(authentication));
            request.getContext().putIfAbsent("actorRole", authenticatedActorService.getNormalizedRole(authentication));
            authenticatedActorService.getNhanVienId(authentication)
                    .ifPresent(id -> request.getContext().putIfAbsent("actorEmployeeId", id));
            authenticatedActorService.getEmail(authentication)
                    .ifPresent(email -> request.getContext().putIfAbsent("actorEmail", email));
            return ResponseEntity.ok(assistantService.chat(request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(403).body(Map.of("message", ex.getMessage()));
        }
    }
}
