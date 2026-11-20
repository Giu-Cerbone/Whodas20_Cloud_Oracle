package it.whodas.document.config;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Clienti REST verso i servizi interni. Il document service non scrive mai
 * direttamente sulle tabelle degli altri servizi: crea paziente e assessment
 * chiamando le loro API, cosi' le regole di validazione e punteggio restano
 * nel servizio proprietario del dato.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient patientRestClient(RestClient.Builder builder, DocumentServiceProperties properties) {
        return build(builder, properties.getClients().getPatientUrl(), properties);
    }

    @Bean
    public RestClient questionnaireRestClient(RestClient.Builder builder, DocumentServiceProperties properties) {
        return build(builder, properties.getClients().getQuestionnaireUrl(), properties);
    }

    @Bean
    public RestClient assessmentRestClient(RestClient.Builder builder, DocumentServiceProperties properties) {
        return build(builder, properties.getClients().getAssessmentUrl(), properties);
    }

    private RestClient build(RestClient.Builder builder, String baseUrl, DocumentServiceProperties properties) {
        DocumentServiceProperties.Clients clients = properties.getClients();
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(java.time.Duration.ofMillis(clients.getConnectTimeoutMs()))
                .withReadTimeout(java.time.Duration.ofMillis(clients.getReadTimeoutMs()));
        return builder
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();
    }
}
