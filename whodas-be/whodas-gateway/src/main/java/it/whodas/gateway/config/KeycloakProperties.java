package it.whodas.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    private String baseUrl = "http://localhost:8080";
    private String realm = "whodas-realm";
    private String clientId = "whodas20-client";
    private String clientSecret = "whodas20-client-key";
    private String scope = "openid profile email";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String scope() {
        return scope;
    }

    public String realmUrl() {
        return baseUrl + "/realms/" + realm;
    }

    public String tokenEndpoint() {
        return realmUrl() + "/protocol/openid-connect/token";
    }

    public String issuer() {
        return realmUrl();
    }

    public String jwkSetUri() {
        return realmUrl() + "/protocol/openid-connect/certs";
    }

    public String introspectionEndpoint() {
        return realmUrl() + "/protocol/openid-connect/token/introspect";
    }
}
