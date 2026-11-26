package it.whodas.document.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI documentServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WHODAS Document Service API")
                        .description("Document generation, import and extraction API for WHODAS 2.0 platform")
                        .version("1.0.0"));
    }
}