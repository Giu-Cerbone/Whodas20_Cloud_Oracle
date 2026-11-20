package it.whodas.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class WhodasGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(WhodasGatewayApplication.class, args);
    }
}