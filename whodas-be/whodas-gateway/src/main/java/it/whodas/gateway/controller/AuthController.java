package it.whodas.gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final KeycloakClient keycloakClient;
    private final ObjectMapper objectMapper;

    public AuthController(KeycloakClient keycloakClient, ObjectMapper objectMapper) {
        this.keycloakClient = keycloakClient;
        this.objectMapper = objectMapper;
    }

@PostMapping("/login")
    public Mono<ResponseEntity<Map<String, Object>>> login(ServerHttpRequest request) {
        return request.getBody()
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    return new String(bytes, StandardCharsets.UTF_8);
                })
                .defaultIfEmpty("")
                .next()
                .flatMap(body -> {
                    try {
                        LoginRequest loginRequest = objectMapper.readValue(body, LoginRequest.class);
                        if (loginRequest.getUsername() == null || loginRequest.getPassword() == null
                                || loginRequest.getUsername().isBlank() || loginRequest.getPassword().isBlank()) {
                            return Mono.just(error("Username e password sono obbligatori"));
                        }
                        return keycloakClient.requestAccessToken(loginRequest.getUsername(), loginRequest.getPassword())
                                .map(token -> {
                                    if (!token.isValid()) {
                                        String errorMsg = token.errorDescription() != null ? token.errorDescription() : "Credenziali non valide ClientID e Client Secret";
                                        return ResponseEntity.status(401).body(errorBody(errorMsg));
                                    }
                                    return ResponseEntity.ok(successBody("Login successful", token));
                                })
                                .defaultIfEmpty(ResponseEntity.status(401).body(errorBody("Credenziali non valide ClientID e Client Secret -- ")));
                    } catch (Exception e) {
                        log.error("Errore parsing login request: {}", e.getMessage());
                        return Mono.just(error("Richiesta non valida"));
                    }
                });
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
    public Mono<ResponseEntity<Map<String, Object>>> validateToken(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String token = BasicCredentials.extractToken(authorization);
        
        // If no token in Authorization header, try to parse from form-urlencoded body
        if (token == null) {
            return request.getBody()
                    .map(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        return new String(bytes, StandardCharsets.UTF_8);
                    })
                    .defaultIfEmpty("")
                    .next()
                    .flatMap(body -> {
                        String extractedToken = extractTokenFromForm(body);
                        if (extractedToken == null) {
                            return Mono.just(ResponseEntity.status(401).body(errorBody("Token mancante")));
                        }
                        return keycloakClient.introspectToken(extractedToken)
                                .map(result -> buildValidationResponse(result))
                                .defaultIfEmpty(ResponseEntity.status(401).body(errorBody("Token non valido")));
                    });
        }
        
        // Token found in Authorization header
        return keycloakClient.introspectToken(token)
                .map(result -> buildValidationResponse(result))
                .defaultIfEmpty(ResponseEntity.status(401).body(errorBody("Token non valido")));
    }

    private String extractTokenFromForm(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        // Parse simple form-urlencoded: token=value
        // Do NOT URL-decode the token - it contains base64 characters that
        // should remain intact. The browser/Invoke-RestMethod already handles
        // URL encoding, so the raw value from the body is the correct token.
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && "token".equals(parts[0])) {
                return parts[1];
            }
        }
        return null;
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