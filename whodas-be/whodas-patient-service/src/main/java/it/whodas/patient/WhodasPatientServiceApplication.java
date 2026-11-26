package it.whodas.patient;

import it.whodas.common.config.CommonConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@Import(CommonConfig.class)
@EnableAsync
public class WhodasPatientServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WhodasPatientServiceApplication.class, args);
    }
}