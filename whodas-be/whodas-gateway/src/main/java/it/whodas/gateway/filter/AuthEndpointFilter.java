package it.whodas.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.whodas.gateway.client.KeycloakClient;
import it.whodas.gateway.client.TokenResponse;
import it.whodas.gateway.dto.LoginRequest;
import it.whodas.gateway.dto.RefreshRequest;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class AuthEndpointFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthEndpointFilter.class);

    @PostConstruct
    public void init() {
        log.info("AuthEndpointFilter initialized!");
    }

    private static final Set<String> AUTH_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/client-token",
            "/api/v1/auth/validate"
    );

    private final KeycloakClient keycloakClient;
    private final ObjectMapper objectMapper;

    public AuthEndpointFilter(KeycloakClient keycloakClient, ObjectMapper objectMapper) {
        this.keycloakClient = keycloakClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        log.debug("AuthEndpointFilter checking path: {}", path);

        if (!AUTH_PATHS.contains(path)) {
            log.debug("Path not in auth paths, continuing chain");
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        log.info("Handling auth endpoint: {}", path);
        log.info("Content-Type: {}", request.getHeaders().getContentType());
        log.info("Content-Length: {}", request.getHeaders().getContentLength());

        return DataBufferUtils.join(request.getBody())
                .map(dataBuffer -> {
                    try {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        return new String(bytes, StandardCharsets.UTF_8);
                    } finally {
                        DataBufferUtils.release(dataBuffer);
                    }
                })
                .defaultIfEmpty("")
                .flatMap(body -> {
                    log.debug("Received body: '{}'", body);
                    try {
                        if ("/api/v1/auth/login".equals(path)) {
                            return handleLogin(body, response);
                        } else if ("/api/v1/auth/refresh".equals(path)) {
                            return handleRefresh(body, response);
                        } else if ("/api/v1/auth/client-token".equals(path)) {
                            return handleClientToken(response);
                        } else if ("/api/v1/auth/validate".equals(path)) {
                            return handleValidate(body, response);
                        }
                        return chain.filter(exchange);
                    } catch (Exception e) {
                        return writeError(response, HttpStatus.BAD_REQUEST, "Richiesta non valida");
                    }
                });
    }

    private Mono<Void> handleLogin(String body, ServerHttpResponse response) {
        try {
            LoginRequest loginRequest = objectMapper.readValue(body, LoginRequest.class);
            if (loginRequest.getUsername() == null || loginRequest.getPassword() == null
                    || loginRequest.getUsername().isBlank() || loginRequest.getPassword().isBlank()) {
                return writeError(response, HttpStatus.BAD_REQUEST, "Username e password sono obbligatori");
            }

            return keycloakClient.requestAccessToken(loginRequest.getUsername(), loginRequest.getPassword())
                    .flatMap(token -> {
                        if (!token.isValid()) {
                            String errorMsg = token.errorDescription() != null ? token.errorDescription() : "Credenziali non valide";
                            return writeError(response, HttpStatus.UNAUTHORIZED, errorMsg);
                        }
                        return writeSuccess(response, token);
                    });
        } catch (Exception e) {
            return writeError(response, HttpStatus.BAD_REQUEST, "Richiesta non valida");
        }
    }

    private Mono<Void> handleRefresh(String body, ServerHttpResponse response) {
        try {
            RefreshRequest refreshRequest = objectMapper.readValue(body, RefreshRequest.class);
            if (refreshRequest.getRefreshToken() == null || refreshRequest.getRefreshToken().isBlank()) {
                return writeError(response, HttpStatus.BAD_REQUEST, "refreshToken mancante");
            }

            return keycloakClient.refreshAccessToken(refreshRequest.getRefreshToken())
                    .flatMap(token -> {
                        if (!token.isValid()) {
                            return writeError(response, HttpStatus.UNAUTHORIZED, "Refresh token non valido");
                        }
                        return writeSuccess(response, token);
                    });
        } catch (Exception e) {
            return writeError(response, HttpStatus.BAD_REQUEST, "Richiesta non valida");
        }
    }

    private Mono<Void> handleClientToken(ServerHttpResponse response) {
        return keycloakClient.requestClientCredentialsToken()
                .flatMap(token -> {
                    if (!token.isValid()) {
                        String errorMsg = token.errorDescription() != null ? token.errorDescription() : "Client credentials non validi";
                        return writeError(response, HttpStatus.UNAUTHORIZED, errorMsg);
                    }
                    return writeSuccess(response, token);
                });
    }

    private Mono<Void> handleValidate(String body, ServerHttpResponse response) {
        // TODO: implement token validation
        return writeError(response, HttpStatus.NOT_IMPLEMENTED, "Non implementato");
    }

    private Mono<Void> writeSuccess(ServerHttpResponse response, TokenResponse token) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("message", "Login successful");
        body.put("access_token", token.accessToken());
        body.put("refresh_token", token.refreshToken());
        body.put("token_type", token.tokenType());
        body.put("expires_in", token.expiresIn());
        return writeResponse(response, HttpStatus.OK, body);
    }

    private Mono<Void> writeError(ServerHttpResponse response, HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", message);
        return writeResponse(response, status, body);
    }

    private Mono<Void> writeResponse(ServerHttpResponse response, HttpStatus status, Map<String, Object> body) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            bytes = "{}".getBytes(StandardCharsets.UTF_8);
        }
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}