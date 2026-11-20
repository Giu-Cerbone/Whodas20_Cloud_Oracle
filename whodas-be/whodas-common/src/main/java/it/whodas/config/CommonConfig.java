package it.whodas.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class CommonConfig {

    /**
     * Nessuna configurazione CORS nei servizi downstream: il front-end accetta
     * tutte le API in modalita' pubblica e senza restrizioni, anche passando dal
     * gateway, che resta l'unico punto di ingresso (porta 7001).
     *
     * Nota: il bean ModelMapper e' stato rimosso. Con MatchingStrategies.STRICT
     * scartava silenziosamente i campi con nomi diversi tra DTO ed entita'
     * (code/text vs itemCode/questionText, birthDate String vs LocalDate,
     * issueDate String vs LocalDateTime) provocando dati persi e POST che
     * fallivano sui vincoli NOT NULL. La conversione ora e' esplicita nei
     * service e nei mapper MapStruct.
     */

    @Bean
    @Primary
    public OpenAPI commonOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Authorization header using the Bearer scheme. Example: \"Authorization: Bearer {token}\"")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .info(new Info()
                        .title("WHODAS 2.0 Platform API")
                        .description("API documentation for WHODAS 2.0 microservices platform")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("WHODAS Team")
                                .email("support@whodas.example.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://whodas.example.com/license")));
    }
}