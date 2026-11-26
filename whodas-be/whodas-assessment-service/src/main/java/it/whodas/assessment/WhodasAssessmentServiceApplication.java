package it.whodas.assessment;

import it.whodas.common.config.CommonConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@Import(CommonConfig.class)
@EnableAsync
public class WhodasAssessmentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WhodasAssessmentServiceApplication.class, args);
    }
}