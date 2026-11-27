package it.whodas.gateway.controller;

import it.whodas.gateway.auth.BasicCredentials;
import it.whodas.gateway.client.KeycloakClient;
import it.whodas.gateway.client.TokenResponse;
import it.whodas.gateway.client.TokenValidationResult;
import it.whodas.gateway.dto.LoginRequest;
import it.whodas.gateway.dto.RefreshRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final KeycloakClient keycloakClient;

    public AuthController(KeycloakClient keycloakClient) {
        this.keycloakClient = keycloakClient;
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<Map<String, Object>>> login(@RequestBody LoginRequest loginRequest) {
        log.debug("Login request received: username={}", loginRequest.getUsername());
        if (loginRequest.getUsername() == null || loginRequest.getPassword() == null
                || loginRequest.getUsername().isBlank() || loginRequest.getPassword().isBlank()) {
            return Mono.just(error("Username e password sono obbligatori"));
        }
        return keycloakClient.requestAccessToken(loginRequest.getUsername(), loginRequest.getPassword())
                .map(token -> {
                    if (!token.isValid()) {
                        String errorMsg = token.errorDescription() != null ? token.errorDescription() : "Credenziali non valide ClientID e Client Secret";
                        log.debug("Keycloak error: {}", errorMsg);
                        return ResponseEntity.status(401).body(errorBody(errorMsg));
                    }
                    log.debug("Login successful for user: {}", loginRequest.getUsername());
                    return ResponseEntity.ok(successBody("Login successful", token));
                })
                .defaultIfEmpty(ResponseEntity.status(401).body(errorBody("Credenziali non valide ClientID e Client Secret")));
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<Map<String, Object>>> refresh(@RequestBody RefreshRequest request) {
        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            return Mono.just(error("refreshToken mancante"));
        }

        return keycloakClient.refreshAccessToken(request.getRefreshToken())
                .map(token -> {
                    if (!token.isValid()) {
                        return ResponseEntity.status(401).body(errorBody("Refresh token non valido"));
                    }
                    return ResponseEntity.ok(successBody("Token refreshed", token));
                })
                .defaultIfEmpty(ResponseEntity.status(401).body(errorBody("Refresh token non valido")));
    }

    @PostMapping("/client-token")
    public Mono<ResponseEntity<Map<String, Object>>> clientToken() {
        return keycloakClient.requestClientCredentialsToken()
                .map(token -> {
                    if (!token.isValid()) {
                        String errorMsg = token.errorDescription() != null ? token.errorDescription() : "Client credentials non validi";
                        return ResponseEntity.status(401).body(errorBody(errorMsg));
                    }
                    return ResponseEntity.ok(successBody("Client token obtained", token));
                })
                .defaultIfEmpty(ResponseEntity.status(401).body(errorBody("Client credentials non validi")));
    }

    @PostMapping(value = "/validate")
    public Mono<ResponseEntity<Map<String, Object>>> validateToken(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        String token = BasicCredentials.extractToken(authorization);

        if (token == null) {
            return Mono.just(ResponseEntity.status(401).body(errorBody("Token mancante")));
        }

        return keycloakClient.introspectToken(token)
                .map(result -> buildValidationResponse(result))
                .defaultIfEmpty(ResponseEntity.status(401).body(errorBody("Token non valido")));
    }

    private ResponseEntity<Map<String, Object>> buildValidationResponse(TokenValidationResult result) {
        Map<String, Object> response = new HashMap<>();
        if (!result.active()) {
            response.put("valid", false);
            response.put("message", "Token non valido o scaduto");
            return ResponseEntity.status(401).body(response);
        }
        response.put("valid", true);
        response.put("message", "Token valido");
        response.put("username", result.username());
        response.put("sub", result.sub());
        response.put("exp", result.exp());
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> error(String message) {
        return ResponseEntity.badRequest().body(errorBody(message));
    }

    private Map<String, Object> successBody(String message, TokenResponse token) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("access_token", token.accessToken());
        response.put("refresh_token", token.refreshToken());
        response.put("token_type", token.tokenType());
        response.put("expires_in", token.expiresIn());
        return response;
    }

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}