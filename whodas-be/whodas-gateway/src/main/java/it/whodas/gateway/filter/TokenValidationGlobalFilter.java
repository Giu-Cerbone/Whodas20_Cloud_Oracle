package it.whodas.gateway.filter;

import it.whodas.gateway.auth.BasicCredentials;
import it.whodas.gateway.client.KeycloakClient;
import it.whodas.gateway.client.TokenValidationResult;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class TokenValidationGlobalFilter implements GlobalFilter, Ordered {

    private static final Set<String> EXCLUDED_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/client-token",
            "/api/v1/auth/validate",
            "/api/v1/registration",
            "/api/v1/registration/**",
            "/actuator",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-ui",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars"
    );

    private final KeycloakClient keycloakClient;

    public TokenValidationGlobalFilter(KeycloakClient keycloakClient) {
        this.keycloakClient = keycloakClient;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        for (String excluded : EXCLUDED_PATHS) {
            if (path.equals(excluded) || path.startsWith(excluded + "/") || path.startsWith(excluded)) {
                return chain.filter(exchange);
            }
        }

        String token = extractToken(exchange);

        if (token == null) {
            return unauthorized(exchange.getResponse());
        }

        return keycloakClient.introspectToken(token)
                .flatMap(result -> {
                    if (!result.active()) {
                        return unauthorized(exchange.getResponse());
                    }
                    String username = result.username();
                     if (username != null && !username.isBlank()) {
                        ServerHttpRequest mutated = exchange.getRequest().mutate()
                                .header("X-Username", username)
                                .build();
                        ServerWebExchange mutatedExchange = exchange.mutate().request(mutated).build();
                        return chain.filter(mutatedExchange);
                    }
                    return chain.filter(exchange);
                });
    }

    /**
     * Estrae il token dall'header Authorization: Basic, oppure dal query
     * parameter 'token' (necessario per le connessioni SSE, dove il browser
     * non invia header personalizzati tramite EventSource).
     */
    private String extractToken(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && !authHeader.isBlank()) {
            String token = BasicCredentials.extractToken(authHeader);
            if (token != null) {
                return token;
            }
            // Accetta anche token Bearer puro (per compatibilità con SSE)
            if (authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
                String bearerToken = authHeader.substring(7).trim();
                if (!bearerToken.isBlank()) {
                    return bearerToken;
                }
            }
        }
        String tokenParam = exchange.getRequest().getQueryParams().getFirst("token");
        if (tokenParam != null && !tokenParam.isBlank()) {
            return tokenParam;
        }
        return null;
    }

    private Mono<Void> unauthorized(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = "{\"error\":\"Token non valido o mancante\"}".getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
