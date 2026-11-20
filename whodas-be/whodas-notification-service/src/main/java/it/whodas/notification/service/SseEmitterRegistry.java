package it.whodas.notification.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Gestione degli emitter SSE connessi, indicizzati per username.
 *
 * Ogni volta che il NotificationService salva una notifica nel DB, chiama
 * {@link #push(String, Object)} per inviarla in tempo reale a tutti i client
 * SSE connessi per lo stesso username.
 */
@Component
public class SseEmitterRegistry {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterRegistry.class);

    private final Set<SseEmitter> allEmitters = ConcurrentHashMap.newKeySet();

    private final ConcurrentHashMap<String, Set<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    public SseEmitter register(String username) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitter.onCompletion(() -> {
            allEmitters.remove(emitter);
            removeEmitter(username, emitter);
            log.debug("SSE emitter completed for user: {}", username);
        });

        emitter.onTimeout(() -> {
            allEmitters.remove(emitter);
            removeEmitter(username, emitter);
            log.debug("SSE emitter timeout for user: {}", username);
        });

        emitter.onError((Throwable t) -> {
            allEmitters.remove(emitter);
            removeEmitter(username, emitter);
            log.warn("SSE emitter error for user {}: {}", username, t.getMessage());
        });

        allEmitters.add(emitter);
        emittersByUser.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet()).add(emitter);
        log.info("SSE emitter registered for user: {} (total connected: {})", username, allEmitters.size());
        return emitter;
    }

    public void push(String username, Object payload) {
        Set<SseEmitter> set = emittersByUser.get(username);
        if (set == null || set.isEmpty()) {
            log.debug("No SSE emitters connected for user: {}, notification stored in DB only", username);
            return;
        }
        log.debug("Pushing notification to {} emitter(s) for user: {}", set.size(), username);
        for (SseEmitter emitter : Set.copyOf(set)) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(payload));
            } catch (IOException | IllegalStateException e) {
                log.warn("Failed to send SSE to user {}: {}", username, e.getMessage());
                set.remove(emitter);
                allEmitters.remove(emitter);
            }
        }
    }

    public int countEmitters(String username) {
        Set<SseEmitter> set = emittersByUser.get(username);
        return set == null ? 0 : set.size();
    }

    public int countAllEmitters() {
        return allEmitters.size();
    }

    private void removeEmitter(String username, SseEmitter emitter) {
        Set<SseEmitter> set = emittersByUser.get(username);
        if (set != null) {
            set.remove(emitter);
            if (set.isEmpty()) {
                emittersByUser.remove(username);
            }
        }
    }
}
