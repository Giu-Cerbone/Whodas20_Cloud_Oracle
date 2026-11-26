package it.whodas.certificate.client;

import java.time.Duration;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import it.whodas.common.dto.NotificationDto;

@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final RestClient restClient;

    public NotificationClient(RestClient.Builder builder,
            @Value("${notification-service.base-url:http://localhost:7007}") String baseUrl) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(3))
                .withReadTimeout(Duration.ofSeconds(5));
        this.restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();
    }

    public void sendDataLoadedNotification(String username, String endpoint) {
        if (username == null || username.isBlank()) {
            return;
        }
        try {
            NotificationDto dto = NotificationDto.builder()
                    .tipoObj("notifiche")
                    .typeMessage("INFO")
                    .oggetto("Informazioni caricate")
                    .description("Le informazioni richieste sono state caricate con successo. Endpoint: " + endpoint)
                    .dataSpedizione(LocalDateTime.now())
                    .stato("SPEDITO")
                    .username(username)
                    .build();

            restClient.post()
                    .uri("/api/v1/notifications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(dto)
                    .retrieve()
                    .toBodilessEntity();

            log.debug("Notifica caricamento spedita al front-end: username={} endpoint={}", username, endpoint);
        } catch (Exception e) {
            log.warn("Spedizione notifica caricamento fallita: username={} endpoint={} errore={}",
                    username, endpoint, e.getMessage());
        }
    }
}
