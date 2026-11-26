package it.whodas.document;

import it.whodas.common.config.CommonConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@Import(CommonConfig.class)
@ConfigurationPropertiesScan
@EnableAsync
public class WhodasDocumentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WhodasDocumentServiceApplication.class, args);
    }
}
