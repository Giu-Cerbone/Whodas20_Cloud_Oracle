package it.whodas.userservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "keycloak.admin")
public class KeycloakAdminProperties {

    private String baseUrl = "http://localhost:8180";
    private String realm = "whodas-realm";
    private String adminClientId = "admin-cli";
    private String username = "admin";
    private String password = "admin";
    private String ldapProviderId = "a81a47e5-121c-4d8b-b480-3b6e1029f267";

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

    public String getAdminClientId() {
        return adminClientId;
    }

    public void setAdminClientId(String adminClientId) {
        this.adminClientId = adminClientId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLdapProviderId() {
        return ldapProviderId;
    }

    public void setLdapProviderId(String ldapProviderId) {
        this.ldapProviderId = ldapProviderId;
    }

    public String adminBaseUrl() {
        return baseUrl + "/admin/realms/" + realm;
    }

    public String masterTokenEndpoint() {
        return baseUrl + "/realms/master/protocol/openid-connect/token";
    }
}
