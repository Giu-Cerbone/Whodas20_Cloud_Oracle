package it.whodas.assessment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI assessmentServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WHODAS Assessment Service API")
                        .description("Assessment management and scoring API for WHODAS 2.0 platform")
                        .version("1.0.0"));
    }
}