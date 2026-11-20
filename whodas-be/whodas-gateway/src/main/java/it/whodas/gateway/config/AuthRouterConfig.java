package it.whodas.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.whodas.gateway.client.KeycloakClient;
import it.whodas.gateway.client.TokenResponse;
import it.whodas.gateway.dto.LoginRequest;
import it.whodas.gateway.dto.RefreshRequest;
import jakarta.annotation.PostConstruct;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;

@Configuration
public class AuthRouterConfig {

    private static final Logger log = LoggerFactory.getLogger(AuthRouterConfig.class);

    @PostConstruct
    public void init() {
        log.info("AuthRouterConfig initialized!");
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE - 100)
    public RouterFunction<ServerResponse> authRouterFunction(KeycloakClient keycloakClient, ObjectMapper objectMapper) {
        AuthHandler handler = new AuthHandler(keycloakClient, objectMapper);
        return RouterFunctions.route()
                .POST("/internal/auth/login", handler::login)
                .POST("/internal/auth/refresh", handler::refresh)
                .POST("/internal/auth/client-token", handler::clientToken)
                .POST("/internal/auth/validate", handler::validate)
                .build();
    }

    static class AuthHandler {
        private final KeycloakClient keycloakClient;
        private final ObjectMapper objectMapper;

        AuthHandler(KeycloakClient keycloakClient, ObjectMapper objectMapper) {
            this.keycloakClient = keycloakClient;
            this.objectMapper = objectMapper;
        }

        public Mono<ServerResponse> login(ServerRequest request) {
            return request.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(body -> {
                        try {
                            LoginRequest loginRequest = objectMapper.readValue(body, LoginRequest.class);
                            if (loginRequest.getUsername() == null || loginRequest.getPassword() == null
                                    || loginRequest.getUsername().isBlank() || loginRequest.getPassword().isBlank()) {
                                return ServerResponse.badRequest().bodyValue(errorBody("Username e password sono obbligatori"));
                            }
                            return keycloakClient.requestAccessToken(loginRequest.getUsername(), loginRequest.getPassword())
                                    .flatMap(token -> {
                                        if (!token.isValid()) {
                                            String errorMsg = token.errorDescription() != null ? token.errorDescription() : "Credenziali non valide";
                                            return ServerResponse.status(HttpStatus.UNAUTHORIZED).bodyValue(errorBody(errorMsg));
                                        }
                                        return ServerResponse.ok().bodyValue(successBody(token));
                                    });
                        } catch (Exception e) {
                            return ServerResponse.badRequest().bodyValue(errorBody("Richiesta non valida"));
                        }
                    });
        }

        public Mono<ServerResponse> refresh(ServerRequest request) {
            return request.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(body -> {
                        try {
                            RefreshRequest refreshRequest = objectMapper.readValue(body, RefreshRequest.class);
                            if (refreshRequest.getRefreshToken() == null || refreshRequest.getRefreshToken().isBlank()) {
                                return ServerResponse.badRequest().bodyValue(errorBody("refreshToken mancante"));
                            }
                            return keycloakClient.refreshAccessToken(refreshRequest.getRefreshToken())
                                    .flatMap(token -> {
                                        if (!token.isValid()) {
                                            return ServerResponse.status(HttpStatus.UNAUTHORIZED).bodyValue(errorBody("Refresh token non valido"));
                                        }
                                        return ServerResponse.ok().bodyValue(successBody(token));
                                    });
                        } catch (Exception e) {
                            return ServerResponse.badRequest().bodyValue(errorBody("Richiesta non valida"));
                        }
                    });
        }

        public Mono<ServerResponse> clientToken(ServerRequest request) {
            return keycloakClient.requestClientCredentialsToken()
                    .flatMap(token -> {
                        if (!token.isValid()) {
                            String errorMsg = token.errorDescription() != null ? token.errorDescription() : "Client credentials non validi";
                            return ServerResponse.status(HttpStatus.UNAUTHORIZED).bodyValue(errorBody(errorMsg));
                        }
                        return ServerResponse.ok().bodyValue(successBody(token));
                    });
        }

        public Mono<ServerResponse> validate(ServerRequest request) {
            return ServerResponse.status(HttpStatus.NOT_IMPLEMENTED).bodyValue(errorBody("Non implementato"));
        }

        private Map<String, Object> successBody(TokenResponse token) {
            Map<String, Object> body = new HashMap<>();
            body.put("success", true);
            body.put("message", "Login successful");
            body.put("access_token", token.accessToken());
            body.put("refresh_token", token.refreshToken());
            body.put("token_type", token.tokenType());
            body.put("expires_in", token.expiresIn());
            return body;
        }

        private Map<String, Object> errorBody(String message) {
            Map<String, Object> body = new HashMap<>();
            body.put("success", false);
            body.put("message", message);
            return body;
        }
    }
}