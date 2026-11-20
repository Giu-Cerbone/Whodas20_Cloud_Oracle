package it.whodas.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Nessuna autenticazione Spring Security e nessun CORS.
 *
 * L'autenticazione delle richieste e' affidata a TokenValidationGlobalFilter,
 * che legge il token dall'header Authorization: Basic base64("username:token").
 * Spring Security resta deliberatamente disattivato (httpBasic incluso) perche'
 * non deve interpretare quell'header.
 *
 * Vincoli di progetto:
 * - nessuna logica CORS (niente @CrossOrigin, niente spring.cloud.gateway.globalcors,
 *   niente CorsConfigurationSource): il front-end Angular parla al gateway attraverso
 *   il proxy di sviluppo (src/proxy.conf.json -> /api verso localhost:7001), quindi le
 *   richieste sono same-origin e il browser non richiede header CORS.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .logout(ServerHttpSecurity.LogoutSpec::disable)
            .authorizeExchange(exchange -> exchange.anyExchange().permitAll());
        return http.build();
    }
}