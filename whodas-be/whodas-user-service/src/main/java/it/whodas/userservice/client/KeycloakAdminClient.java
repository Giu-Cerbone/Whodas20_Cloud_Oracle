package it.whodas.userservice.client;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import it.whodas.userservice.config.KeycloakAdminProperties;

@Service
public class KeycloakAdminClient {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminClient.class);

    private final KeycloakAdminProperties properties;
    private final RestClient restClient;
    private volatile String adminToken;
    private volatile String ldapProviderId;

    public KeycloakAdminClient(KeycloakAdminProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().build();
    }

    /**
     * Esegue il full sync della user storage LDAP verso Keycloak, cosi' che le
     * nuove utenze LDAP diventino utenti Keycloak del realm.
     *
     * <p>POST {@code {baseUrl}/admin/realms/{realm}/user-storage/{ldapProviderId}/sync}
     * con query {@code direction=fetch&action=triggerFullSync} e header
     * {@code Authorization: Bearer <token admin>}. HTTP 409 (sync gia' in
     * corso) non e' un errore fatale: viene loggato come warning.
     */
    public void triggerLdapFullSync() {
        withAdminToken(token -> {
            String providerId = resolveLdapProviderId(token);
            try {
                postLdapFullSync(token, providerId);
            } catch (HttpClientErrorException.NotFound e) {
                log.warn("Provider LDAP '{}' non trovato in Keycloak, rileggo i componenti e riprovo",
                        providerId);
                this.ldapProviderId = null;
                String refreshedId = resolveLdapProviderId(token);
                if (refreshedId.equals(providerId)) {
                    throw new RuntimeException("Keycloak: LDAP full sync fallito (HTTP 404)", e);
                }
                try {
                    postLdapFullSync(token, refreshedId);
                } catch (HttpClientErrorException.NotFound retryFailure) {
                    throw new RuntimeException("Keycloak: LDAP full sync fallito (HTTP 404)", retryFailure);
                }
            }
            return null;
        });
    }

    private void postLdapFullSync(String token, String providerId) {
        String url = properties.adminBaseUrl() + "/user-storage/" + providerId + "/sync";
        try {
            restClient.post()
                    .uri(builder -> UriComponentsBuilder.fromUriString(url)
                            .queryParam("direction", "fetch")
                            .queryParam("action", "triggerFullSync")
                            .build()
                            .encode()
                            .toUri())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Full sync LDAP completato (provider '{}')", providerId);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw e;
        } catch (HttpClientErrorException.NotFound e) {
            throw e;
        } catch (HttpClientErrorException.Conflict e) {
            log.warn("Full sync LDAP gia' in corso (HTTP 409), procedo senza errore: {}",
                    e.getResponseBodyAsString());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Full sync LDAP fallito: HTTP {} - {}", e.getStatusCode(),
                    e.getResponseBodyAsString(), e);
            throw new RuntimeException("Keycloak: LDAP full sync fallito (HTTP "
                    + e.getStatusCode().value() + ")", e);
        }
    }

    /**
     * Risolve l'id del component LDAP del realm leggendolo dall'Admin API
     * ({@code GET {baseUrl}/components?type=org.keycloak.storage.UserStorageProvider}).
     * L'id generato da Keycloak cambia ogni volta che il realm viene
     * reimportato, quindi non puo' essere considerato statico: il valore
     * configurato resta solo come fallback.
     */
    private String resolveLdapProviderId(String token) {
        String cached = ldapProviderId;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (ldapProviderId != null) {
                return ldapProviderId;
            }
            String url = properties.adminBaseUrl() + "/components";
            List<Map<String, Object>> components;
            try {
                components = restClient.get()
                        .uri(builder -> UriComponentsBuilder.fromUriString(url)
                                .queryParam("type", "org.keycloak.storage.UserStorageProvider")
                                .build()
                                .encode()
                                .toUri())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                        });
            } catch (HttpClientErrorException.Unauthorized e) {
                throw e;
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                log.error("Lettura component LDAP da Keycloak fallita: HTTP {} - {}", e.getStatusCode(),
                        e.getResponseBodyAsString(), e);
                throw new RuntimeException("Keycloak: lettura component LDAP fallita (HTTP "
                        + e.getStatusCode().value() + ")", e);
            }
            String resolved = null;
            if (components != null) {
                for (Map<String, Object> component : components) {
                    if (component.get("id") == null) {
                        continue;
                    }
                    if (resolved == null) {
                        resolved = component.get("id").toString();
                    }
                    if ("ldap".equals(String.valueOf(component.get("providerId")))) {
                        resolved = component.get("id").toString();
                        break;
                    }
                }
            }
            if (resolved == null) {
                resolved = properties.getLdapProviderId();
                log.warn("Nessun UserStorageProvider LDAP nel realm '{}', uso l'id configurato '{}'",
                        properties.getRealm(), resolved);
            } else {
                log.info("Provider LDAP del realm '{}' risolto: {}", properties.getRealm(), resolved);
            }
            ldapProviderId = resolved;
            return resolved;
        }
    }

    /**
     * Esegue il full sync LDAP e attende che l'utente con lo username dato
     * sia disponibile in Keycloak (polling con timeout).
     */
    public String triggerLdapFullSyncAndWaitForUser(String username) {
        triggerLdapFullSync();
        
        int maxAttempts = 30;
        int delayMs = 1000;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            String userId = findUserIdByUserName(username);
            if (userId != null) {
                log.info("Utente Keycloak '{}' trovato dopo sync LDAP (tentativo {}/{}): {}", username, attempt, maxAttempts, userId);
                return userId;
            }
            log.debug("Utente Keycloak '{}' non ancora disponibile, attesa... (tentativo {}/{})", username, attempt, maxAttempts);
            try {
                TimeUnit.MILLISECONDS.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrotto durante attesa sync LDAP", e);
            }
        }
        throw new RuntimeException("Keycloak: utente '" + username + "' non disponibile dopo " + maxAttempts + " tentativi di sync LDAP");
    }

    /**
     * Ritorna l'id Keycloak dell'utente con il username dato.
     *
     * <p>GET {@code {baseUrl}/admin/realms/{realm}/users?username=<username>&exact=true}
     * con token admin; ritorna l'id del primo elemento della lista, oppure
     * {@code null} se la lista e' vuota.
     */
    public String findUserIdByUserName(String username) {
        return withAdminToken(token -> {
            String url = properties.adminBaseUrl() + "/users";
            try {
                List<Map<String, Object>> users = restClient.get()
                        .uri(builder -> UriComponentsBuilder.fromUriString(url)
                                .queryParam("username", username)
                                .queryParam("exact", "true")
                                .build()
                                .encode()
                                .toUri())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                        });
                if (users == null || users.isEmpty()) {
                    log.debug("Nessun utente Keycloak trovato per username '{}'", username);
                    return null;
                }
                Object id = users.get(0).get("id");
                String userId = id == null ? null : id.toString();
                log.debug("Utente Keycloak '{}' -> id {}", username, userId);
                return userId;
            } catch (HttpClientErrorException.Unauthorized e) {
                throw e;
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                log.error("Ricerca utente Keycloak '{}' fallita: HTTP {} - {}", username,
                        e.getStatusCode(), e.getResponseBodyAsString(), e);
                throw new RuntimeException("Keycloak: ricerca utente '" + username + "' fallita (HTTP "
                        + e.getStatusCode().value() + ")", e);
            }
        });
    }

    /**
     * Assegna un realm role all'utente Keycloak indicato.
     *
     * <p>POST {@code {baseUrl}/admin/realms/{realm}/users/{userId}/role-mappings/realm}
     * con token admin e body JSON {@code [{"id":"<roleId>", "name":"<roleName>"}]}. Su HTTP 404
     * (ruolo o utente inesistente) viene buttato un {@link RuntimeException}.
     */
    public void assignRealmRole(String userId, String roleName) {
        String roleId = getRealmRoleId(roleName);
        withAdminToken(token -> {
            String url = properties.adminBaseUrl() + "/users/" + userId + "/role-mappings/realm";
            try {
                restClient.post()
                        .uri(url)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(List.of(Map.of("id", roleId, "name", roleName)))
                        .retrieve()
                        .toBodilessEntity();
                log.info("Realm role '{}' (id: {}) assegnato all'utente Keycloak '{}'", roleName, roleId, userId);
            } catch (HttpClientErrorException.Unauthorized e) {
                throw e;
            } catch (HttpClientErrorException.NotFound e) {
                log.error("Assegnazione realm role '{}' all'utente '{}' fallita: ruolo o utente "
                        + "inesistente (HTTP 404)", roleName, userId);
                throw new RuntimeException("Keycloak: ruolo realm '" + roleName + "' inesistente o utente '"
                        + userId + "' non trovato (HTTP 404)", e);
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                log.error("Assegnazione realm role '{}' all'utente '{}' fallita: HTTP {} - {}",
                        roleName, userId, e.getStatusCode(), e.getResponseBodyAsString(), e);
                throw new RuntimeException("Keycloak: assegnazione ruolo '" + roleName + "' fallita (HTTP "
                        + e.getStatusCode().value() + ")", e);
            }
            return null;
        });
    }

    /**
     * Ritorna l'ID del realm role con il nome dato.
     */
    private String getRealmRoleId(String roleName) {
        return withAdminToken(token -> {
            String url = properties.adminBaseUrl() + "/roles/" + roleName;
            try {
                Map<String, Object> role = restClient.get()
                        .uri(url)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {
                        });
                if (role == null || role.get("id") == null) {
                    throw new RuntimeException("Keycloak: ruolo realm '" + roleName + "' non trovato");
                }
                return role.get("id").toString();
            } catch (HttpClientErrorException.NotFound e) {
                throw new RuntimeException("Keycloak: ruolo realm '" + roleName + "' inesistente (HTTP 404)", e);
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                log.error("Ricerca ruolo realm '{}' fallita: HTTP {} - {}", roleName,
                        e.getStatusCode(), e.getResponseBodyAsString(), e);
                throw new RuntimeException("Keycloak: ricerca ruolo '" + roleName + "' fallita (HTTP "
                        + e.getStatusCode().value() + ")", e);
            }
        });
    }

    /**
     * Verifica la password di un utente del realm tramite password grant sul
     * client pubblico (nessun token admin necessario).
     *
     * @return true se le credenziali sono valide, false altrimenti
     */
    public boolean verifyUserPassword(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("username", username);
        form.add("password", password);

        try {
            restClient.post()
                    .uri(properties.realmTokenEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.Unauthorized e) {
            return false;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Verifica password Keycloak per '{}' fallita: HTTP {} - {}", username,
                    e.getStatusCode(), e.getResponseBodyAsString());
            return false;
        } catch (ResourceAccessException e) {
            log.error("Keycloak non raggiungibile per verifica password: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Aggiorna la password di un utente Keycloak tramite Admin API
     * (PUT reset-password, non temporanea).
     */
    public void updatePassword(String userId, String newPassword) {
        withAdminToken(token -> {
            String url = properties.adminBaseUrl() + "/users/" + userId + "/reset-password";
            try {
                restClient.put()
                        .uri(url)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("type", "password", "value", newPassword, "temporary", false))
                        .retrieve()
                        .toBodilessEntity();
                log.info("Password Keycloak aggiornata per utente '{}'", userId);
            } catch (HttpClientErrorException.Unauthorized e) {
                throw e;
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                log.error("Aggiornamento password Keycloak per utente '{}' fallito: HTTP {} - {}",
                        userId, e.getStatusCode(), e.getResponseBodyAsString());
                throw new RuntimeException("Keycloak: aggiornamento password fallito (HTTP "
                        + e.getStatusCode().value() + ")", e);
            }
            return null;
        });
    }

    private <T> T withAdminToken(AdminCall<T> call) {
        return executeWithRetry(call, false);
    }

    private <T> T executeWithRetry(AdminCall<T> call, boolean retried) {
        try {
            return call.execute(currentAdminToken());
        } catch (HttpClientErrorException.Unauthorized e) {
            if (retried) {
                log.error("Token admin Keycloak ancora non valido dopo il rinnovo, aborto");
                throw new RuntimeException("Keycloak: token admin non valido (HTTP 401 anche dopo il rinnovo)", e);
            }
            log.warn("Token admin Keycloak rifiutato (HTTP 401), rinnovo il token e riprovo una volta");
            this.adminToken = null;
            return executeWithRetry(call, true);
        } catch (ResourceAccessException e) {
            log.error("Keycloak non raggiungibile su {}", properties.getBaseUrl(), e);
            throw new RuntimeException("Keycloak non raggiungibile su " + properties.getBaseUrl(), e);
        }
    }

    private String currentAdminToken() {
        String token = adminToken;
        if (token == null) {
            synchronized (this) {
                token = adminToken;
                if (token == null) {
                    token = fetchAdminToken();
                    adminToken = token;
                }
            }
        }
        return token;
    }

    private String fetchAdminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", properties.getAdminClientId());
        form.add("username", properties.getUsername());
        form.add("password", properties.getPassword());

        Map<String, Object> response;
        try {
            response = restClient.post()
                    .uri(properties.masterTokenEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
        } catch (HttpClientErrorException e) {
            log.error("Token admin Keycloak rifiutato: HTTP {} - {}", e.getStatusCode(),
                    e.getResponseBodyAsString());
            throw new RuntimeException("Keycloak: token admin non ottenuto (HTTP "
                    + e.getStatusCode().value() + ")", e);
        } catch (HttpServerErrorException e) {
            log.error("Token admin Keycloak non ottenuto: HTTP {} - {}", e.getStatusCode(),
                    e.getResponseBodyAsString(), e);
            throw new RuntimeException("Keycloak: token endpoint in errore (HTTP "
                    + e.getStatusCode().value() + ")", e);
        }
        if (response == null
                || !(response.get("access_token") instanceof String accessToken)
                || accessToken.isBlank()) {
            throw new RuntimeException("Keycloak: access_token assente nella risposta del token endpoint");
        }
        return accessToken;
    }

    @FunctionalInterface
    private interface AdminCall<T> {
        T execute(String token);
    }
}
