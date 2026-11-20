package it.whodas.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NotificationProperties {

    private final String baseUrl;

    public NotificationProperties(@Value("${NOTIFICATION_SERVICE_URL:http://localhost:7007}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
