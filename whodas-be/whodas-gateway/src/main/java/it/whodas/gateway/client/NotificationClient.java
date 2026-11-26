package it.whodas.gateway.client;

import it.whodas.common.dto.NotificationDto;
import it.whodas.gateway.config.NotificationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final WebClient webClient;
    private final String baseUrl;

    public NotificationClient(WebClient.Builder webClientBuilder, NotificationProperties properties) {
        this.webClient = webClientBuilder.baseUrl(properties.getBaseUrl()).build();
        this.baseUrl = properties.getBaseUrl();
    }

    public Mono<NotificationDto> sendLoginNotification(String username) {
        NotificationDto dto = NotificationDto.builder()
                .tipoObj("notifiche")
                .typeMessage("INFO")
                .oggetto("Login effettuato")
                .description("L'utente " + username + " ha effettuato il login con successo")
                .dataSpedizione(java.time.LocalDateTime.now())
                .stato("SPEDITO")
                .username(username)
                .build();

        return webClient.post()
                .uri("/api/v1/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse
                        .bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> {
                            log.warn("Notification service ha rifiutato la richiesta: status={} body={}",
                                    clientResponse.statusCode().value(), body);
                            return Mono.error(new NotificationException(body));
                        }))
                .bodyToMono(NotificationDto.class)
                .defaultIfEmpty(null)
                .onErrorResume(NotificationException.class, e -> {
                    log.warn("Errore invio notifica login per username={}: {}", username, e.getMessage());
                    return Mono.empty();
                })
                .onErrorResume(e -> {
                    log.error("Errore di comunicazione con Notification Service per username={}", username, e);
                    return Mono.empty();
                });
    }

    private static class NotificationException extends RuntimeException {
        public NotificationException(String message) {
            super(message);
        }
    }
}