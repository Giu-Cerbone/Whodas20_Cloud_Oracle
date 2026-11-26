package it.whodas.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// scanBasePackages e' piu' ampio del package del servizio (it.whodas) perche'
// deve raccogliere anche i bean di whodas-common, in particolare
// GlobalExceptionHandler (@RestControllerAdvice in it.whodas.common.exception),
// che i servizi fratelli non scansionano (solo @Import(CommonConfig.class)).
@SpringBootApplication(scanBasePackages = "it.whodas")
@EnableAsync
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
