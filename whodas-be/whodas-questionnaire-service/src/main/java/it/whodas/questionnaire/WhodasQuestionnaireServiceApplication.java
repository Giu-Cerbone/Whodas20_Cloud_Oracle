package it.whodas.questionnaire;

import it.whodas.common.config.CommonConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@Import(CommonConfig.class)
@EnableAsync
public class WhodasQuestionnaireServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WhodasQuestionnaireServiceApplication.class, args);
    }
}