package it.whodas.gateway.client;

import com.fasterxml.jackson.databind.JsonNode;
import it.whodas.gateway.config.KeycloakProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class KeycloakClient {

    private static final Logger log = LoggerFactory.getLogger(KeycloakClient.class);

    private final WebClient webClient;
    private final KeycloakProperties properties;

    public KeycloakClient(WebClient.Builder webClientBuilder, KeycloakProperties properties) {
        this.webClient = webClientBuilder.build();
        this.properties = properties;
    }

    public String clientId() {
        return properties.getClientId();
    }

    public Mono<TokenResponse> requestAccessToken(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", properties.getClientId());
        form.add("username", username);
        form.add("password", password);
        form.add("scope", properties.scope());
        addSecret(form);
        return callTokenEndpoint(form);
    }

    public Mono<TokenResponse> refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", properties.getClientId());
        form.add("refresh_token", refreshToken);
        addSecret(form);
        return callTokenEndpoint(form);
    }

    public Mono<TokenResponse> requestClientCredentialsToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.getClientId());
        addSecret(form);
        return callTokenEndpoint(form);
    }

    public Mono<TokenValidationResult> introspectToken(String token) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.getClientId());
        form.add("token", token);
        form.add("token_type_hint", "access_token");
        addSecret(form);
        return webClient.post()
                .uri(properties.introspectionEndpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse
                        .bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> {
                            log.warn("Keycloak ha rifiutato l'introspezione: status={} body={}",
                                    clientResponse.statusCode().value(), body);
                            return Mono.error(new KeycloakTokenException(body));
                        }))
                .bodyToMono(JsonNode.class)
                .map(body -> new TokenValidationResult(
                        body.path("active").asBoolean(false),
                        body.path("username").asText(null),
                        body.path("exp").asLong(0),
                        body.path("iat").asLong(0),
                        body.path("sub").asText(null)))
                .defaultIfEmpty(new TokenValidationResult(false, null, 0, 0, null))
                .onErrorResume(KeycloakTokenException.class, e -> Mono.just(new TokenValidationResult(false, null, 0, 0, null)))
                .onErrorResume(e -> {
                    log.error("Errore di comunicazione con Keycloak per l'introspezione", e);
                    return Mono.just(new TokenValidationResult(false, null, 0, 0, null));
                });
    }

    private void addSecret(MultiValueMap<String, String> form) {
        String secret = properties.getClientSecret();
        if (secret != null && !secret.isBlank()) {
            form.add("client_secret", secret);
        }
    }

    private Mono<TokenResponse> callTokenEndpoint(MultiValueMap<String, String> form) {
        String url = properties.tokenEndpoint();
        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse
                        .bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> {
                            log.warn("Keycloak ha rifiutato la richiesta del token: status={} body={}",
                                    clientResponse.statusCode().value(), body);
                            return Mono.error(new KeycloakTokenException(body));
                        }))
                .bodyToMono(JsonNode.class)
                .map(body -> {
                    if (!body.hasNonNull("access_token")) {
                        log.warn("Keycloak ha risposto senza access_token: {}", body);
                        return null;
                    }
                    return new TokenResponse(
                            body.path("access_token").asText(),
                            body.path("refresh_token").asText(null),
                            body.path("token_type").asText("Bearer"),
                            body.path("expires_in").asInt(300));
                })
                .defaultIfEmpty(new TokenResponse(null, null, null, 0))
                .onErrorResume(KeycloakTokenException.class, e -> Mono.just(new TokenResponse(null, null, null, 0, e.getMessage())))
                .onErrorResume(e -> {
                    log.error("Errore di comunicazione con Keycloak su {}", url, e);
                    return Mono.just(new TokenResponse(null, null, null, 0));
                });
    }
}
