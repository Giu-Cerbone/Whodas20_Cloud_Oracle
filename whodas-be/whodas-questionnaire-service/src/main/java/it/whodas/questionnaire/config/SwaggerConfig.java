package it.whodas.questionnaire.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI questionnaireServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WHODAS Questionnaire Service API")
                        .description("Questionnaire items and domains management API for WHODAS 2.0 platform")
                        .version("1.0.0"));
    }
}