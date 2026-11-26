package it.whodas.notification.rest;

import java.util.List;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import it.whodas.common.dto.NotificationDto;
import it.whodas.notification.service.NotificationService;
import it.whodas.notification.service.SseEmitterRegistry;

/**
 * Controller REST e SSE per le notifiche.
 *
 * L'endpoint SSE (GET /stream) permette al front-end di ricevere le notifiche
 * in tempo reale senza chiamare il servizio: il servizio "spinge" le notifiche
 * tramite Server-Sent Events.
 *
 * L'autenticazione e' validata dal gateway (TokenValidationGlobalFilter),
 * che aggiunge l'header X-Username al request prima di inoltrarlo al servizio.
 * Per le connessioni SSE, il token viene passato come query parameter
 * 'token' (il browser non supporta header personalizzati su EventSource).
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private static final String USERNAME_HEADER = "X-Username";

    private final NotificationService service;
    private final SseEmitterRegistry registry;

    public NotificationController(NotificationService service, SseEmitterRegistry registry) {
        this.service = service;
        this.registry = registry;
    }

    @PostMapping
    public ResponseEntity<NotificationDto> createNotification(
            @Valid @RequestBody NotificationDto dto,
            @RequestHeader(value = USERNAME_HEADER, required = false) String authenticatedUsername) {
        log.info("POST /api/v1/notifications - creating notification for username={}", dto.getUsername());
        if (dto.getUsername() == null || dto.getUsername().isBlank()) {
            if (authenticatedUsername != null && !authenticatedUsername.isBlank()) {
                dto.setUsername(authenticatedUsername);
            } else {
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.status(201).body(service.create(dto));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(
            @RequestHeader(value = USERNAME_HEADER, required = false) String authenticatedUsername,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(value = "token", required = false) String tokenParam) {

        String username = resolveUsername(authenticatedUsername, authorization, tokenParam);
        if (username == null || username.isBlank()) {
            log.warn("SSE connection rejected: no authenticated username");
            return createErrorEmitter("Token non valido o mancante");
        }

        log.info("SSE connection registered for user: {}", username);
        SseEmitter emitter = registry.register(username);
        pushExistingUnread(username, emitter);
        return emitter;
    }

    @GetMapping
    public ResponseEntity<List<NotificationDto>> listNotifications(
            @RequestHeader(value = USERNAME_HEADER, required = false) String authenticatedUsername) {

        if (authenticatedUsername == null || authenticatedUsername.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.findByUsername(authenticatedUsername));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDto>> listUnread(
            @RequestHeader(value = USERNAME_HEADER, required = false) String authenticatedUsername) {

        if (authenticatedUsername == null || authenticatedUsername.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.findUnread(authenticatedUsername));
    }

    @GetMapping("/count/unread")
    public ResponseEntity<Long> countUnread(
            @RequestHeader(value = USERNAME_HEADER, required = false) String authenticatedUsername) {

        if (authenticatedUsername == null || authenticatedUsername.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.countUnread(authenticatedUsername));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationDto> markAsRead(
            @PathVariable Long id,
            @RequestHeader(value = USERNAME_HEADER, required = false) String authenticatedUsername) {

        if (authenticatedUsername == null || authenticatedUsername.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.markAsRead(id, authenticatedUsername));
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @RequestHeader(value = USERNAME_HEADER, required = false) String authenticatedUsername) {

        if (authenticatedUsername == null || authenticatedUsername.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        service.markAllAsRead(authenticatedUsername);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long id,
            @RequestHeader(value = USERNAME_HEADER, required = false) String authenticatedUsername) {

        if (authenticatedUsername == null || authenticatedUsername.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        service.deleteLogical(id, authenticatedUsername);
        return ResponseEntity.noContent().build();
    }

    /**
     * Risolve il nome utente dall'header aggiunto dal gateway (dopo validazione
     * del token), oppure decodifica il token JWT passato come query parameter
     * per le connessioni SSE dove il gateway non puo' aggiungere header.
     */
    private String resolveUsername(String authenticatedUsername, String authorization, String tokenParam) {
        if (authenticatedUsername != null && !authenticatedUsername.isBlank()) {
            return authenticatedUsername;
        }
        String token = extractTokenFromAuthorization(authorization);
        if (token == null) {
            token = tokenParam;
        }
        if (token == null || token.isBlank()) {
            return null;
        }
        return decodeUsernameFromJwt(token);
    }

    private String extractTokenFromAuthorization(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Basic ", 0, 6)) {
            return null;
        }
        String credential = authorization.substring(6).trim();
        if (credential.isEmpty()) {
            return null;
        }
        try {
            String decoded = new String(java.util.Base64.getDecoder().decode(credential), java.nio.charset.StandardCharsets.UTF_8);
            int separator = decoded.indexOf(':');
            if (separator < 0) {
                return null;
            }
            String token = decoded.substring(separator + 1).trim();
            return token.isEmpty() ? null : token;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String decodeUsernameFromJwt(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return null;
        }
        try {
            String payload = parts[1];
            String padded = payload.replace('-', '+').replace('_', '/');
            int padding = padded.length() % 4;
            if (padding > 0) {
                padded += "=".repeat(4 - padding);
            }
            byte[] decoded = java.util.Base64.getDecoder().decode(padded);
            String json = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
            int idx = json.indexOf("\"preferred_username\"");
            if (idx < 0) {
                idx = json.indexOf("\"username\"");
            }
            if (idx < 0) {
                idx = json.indexOf("\"sub\"");
            }
            if (idx >= 0) {
                int start = json.indexOf('"', idx + 1) + 1;
                int end = json.indexOf('"', start);
                return json.substring(start, end);
            }
        } catch (Exception e) {
            log.warn("Failed to decode token for username extraction: {}", e.getMessage());
        }
        return null;
    }

    private void pushExistingUnread(String username, SseEmitter emitter) {
        try {
            List<NotificationDto> unread = service.findUnread(username);
            for (NotificationDto dto : unread) {
                emitter.send(SseEmitter.event().name("notification").data(dto));
            }
        } catch (Exception e) {
            log.warn("Failed to push existing unread notifications to user {}: {}", username, e.getMessage());
        }
    }

    private SseEmitter createErrorEmitter(String message) {
        SseEmitter emitter = new SseEmitter(0L);
        try {
            emitter.send(SseEmitter.event().name("error").data(message));
        } catch (Exception ignored) {
        }
        emitter.complete();
        return emitter;
    }
}
