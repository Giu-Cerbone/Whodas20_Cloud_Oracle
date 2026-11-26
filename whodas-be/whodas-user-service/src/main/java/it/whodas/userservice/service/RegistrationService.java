package it.whodas.userservice.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import it.whodas.common.exception.WhodasException;
import it.whodas.userservice.client.KeycloakAdminClient;
import it.whodas.userservice.client.LdapClient;
import it.whodas.userservice.client.LdapClient.LdapUserAttributes;
import it.whodas.userservice.client.LdapClient.LdapUserSpec;
import it.whodas.userservice.domain.RegisteredUser;
import it.whodas.userservice.dto.ChangePasswordRequest;
import it.whodas.userservice.dto.CheckResponse;
import it.whodas.userservice.dto.RegistrationRequest;
import it.whodas.userservice.dto.RegistrationResponse;
import it.whodas.userservice.dto.ResetPasswordResponse;
import it.whodas.userservice.dto.UserListItem;
import it.whodas.userservice.dto.UserProfileResponse;
import it.whodas.userservice.repository.RegisteredUserRepository;

@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private static final String ROLE_USER = "user";

    private final RegisteredUserRepository repository;
    private final LdapClient ldapClient;
    private final KeycloakAdminClient keycloakAdminClient;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(RegisteredUserRepository repository, LdapClient ldapClient,
                               KeycloakAdminClient keycloakAdminClient, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.ldapClient = ldapClient;
        this.keycloakAdminClient = keycloakAdminClient;
        this.passwordEncoder = passwordEncoder;
    }

    public RegistrationResponse register(RegistrationRequest request) {
        String username = request.getUsername();

        if (repository.existsByUsername(username)) {
            throw new WhodasException("Username gia' esistente", "USERNAME_EXISTS");
        }
        if (repository.existsByFiscalCode(request.getFiscalCode())) {
            throw new WhodasException("Codice Fiscale gia' esistente", "FISCAL_CODE_EXISTS");
        }
        if (repository.existsByEmail(request.getEmail())) {
            throw new WhodasException("Email gia' associata ad una utenza", "EMAIL_EXISTS");
        }

        ldapClient.createUser(new LdapUserSpec(
                username,
                request.getPassword(),
                request.getFirstName(),
                request.getLastName(),
                request.getEmail()));

        try {
            String keycloakUserId = keycloakAdminClient.triggerLdapFullSyncAndWaitForUser(username);
            keycloakAdminClient.assignRealmRole(keycloakUserId, ROLE_USER);

            RegisteredUser entity = RegisteredUser.builder()
                    .username(username)
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .birthDate(request.getBirthDate())
                    .fiscalCode(request.getFiscalCode())
                    .address(request.getAddress())
                    .postalCode(request.getPostalCode())
                    .comune(request.getComune())
                    .city(request.getCity())
                    .phone(request.getPhone())
                    .email(request.getEmail())
                    .status(RegisteredUser.STATUS_ACTIVE)
                    .ldapUid(username)
                    .keycloakUserId(keycloakUserId)
                    .registeredAt(LocalDateTime.now())
                    .build();
            repository.save(entity);

            return new RegistrationResponse(true, "Registrazione completata con successo", username);
        } catch (RuntimeException ex) {
            log.error("Errore durante registrazione, rimuovo utente LDAP per rollback: {}", ex.getMessage());
            ldapClient.deleteUser(username);
            throw ex;
        }
    }

    public List<UserListItem> getAllUsers() {
        return repository.findAll().stream()
                .map(u -> new UserListItem(u.getId(), u.getUsername(), u.getFirstName(), u.getLastName(),
                        u.getEmail(), u.getFiscalCode(), u.getStatus(), u.getRegisteredAt()))
                .toList();
    }

    public void syncLdap() {
        keycloakAdminClient.triggerLdapFullSync();
    }

    public CheckResponse checkFiscalCode(String fiscalCode) {
        if (repository.existsByFiscalCode(fiscalCode)) {
            return CheckResponse.duplicate("Codice Fiscale gia' esistente");
        }
        return CheckResponse.available();
    }

    public CheckResponse checkEmail(String email) {
        if (repository.existsByEmail(email)) {
            return CheckResponse.duplicate("Email gia' associata ad una utenza");
        }
        return CheckResponse.available();
    }

    /**
     * Risolve l'indirizzo email di destinatario per una richiesta di reset password
     * partendo da un username o da un'email inseriti dall'utente.
     *
     * <p>L'utenza deve risultare presente su LDAP (o nel DB locale) prima di
     * procedere con l'invio dell'email.
     *
     * @return l'email di destinatario se l'utente esiste e ha un'email associata,
     *         {@link Optional#empty()} altrimenti
     */
    public Optional<ResetRecipient> resolveResetRecipient(String usernameOrEmail) {
        String value = (usernameOrEmail == null) ? "" : usernameOrEmail.trim();
        if (value.isEmpty()) {
            return Optional.empty();
        }

        if (value.contains("@")) {
            // L'utente ha inserito un'email.
            if (repository.existsByEmail(value)) {
                return repository.findByEmail(value)
                        .map(u -> new ResetRecipient(u.getEmail(),
                                u.getFirstName(), u.getLastName(), u.getUsername()));
            }
            // Fallback: l'utente esiste solo su LDAP con quell'email.
            return ldapClient.getUserAttributesByEmail(value)
                    .map(attr -> new ResetRecipient(value,
                            attr.firstName() != null ? attr.firstName() : "",
                            attr.lastName() != null ? attr.lastName() : "",
                            attr.uid() != null ? attr.uid() : value));
        }

        // L'utente ha inserito uno username.
        if (repository.existsByUsername(value)) {
            return repository.findByUsername(value)
                    .map(u -> new ResetRecipient(u.getEmail(),
                            u.getFirstName(), u.getLastName(), u.getUsername()));
        }
        // Fallback: l'utente esiste su LDAP con quello username.
        return ldapClient.getUserAttributes(value)
                .map(attr -> new ResetRecipient(
                        value.contains("@") ? value : (attr.uid() != null ? attr.uid() + "@whodas.local" : value),
                        attr.firstName() != null ? attr.firstName() : "",
                        attr.lastName() != null ? attr.lastName() : "",
                        attr.uid() != null ? attr.uid() : value));
    }

    /**
     * Destinatario di una richiesta di reset password.
     */
    public record ResetRecipient(String email, String firstName, String lastName, String username) {
    }

    /**
     * Profilo completo dell'utente (tutti i campi anagrafici, senza password).
     */
    public UserProfileResponse getProfile(String username) {
        RegisteredUser user = repository.findByUsername(username)
                .orElseThrow(() -> new WhodasException("Utente non trovato", "USER_NOT_FOUND"));
        return UserProfileResponse.builder()
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .birthDate(user.getBirthDate())
                .fiscalCode(user.getFiscalCode())
                .address(user.getAddress())
                .postalCode(user.getPostalCode())
                .comune(user.getComune())
                .city(user.getCity())
                .phone(user.getPhone())
                .email(user.getEmail())
                .status(user.getStatus())
                .registeredAt(user.getRegisteredAt())
                .passwordTemporary(user.getPasswordTemporary())
                .build();
    }

    /**
     * Cambio password dell'utente loggato: verifica la password attuale via bind
     * LDAP, valida la nuova password secondo lo standard di sicurezza e la
     * aggiorna in LDAP (e nel DB locale se presente).
     */
    public ResetPasswordResponse changePassword(ChangePasswordRequest request) {
        String username = request.getUsername();

        // 1. Verifica la password attuale tramite Keycloak (fonte di autenticazione
        //    reale: copre sia gli utenti LDAP sia gli utenti seed del realm).
        if (!keycloakAdminClient.verifyUserPassword(username, request.getCurrentPassword())) {
            return ResetPasswordResponse.builder()
                    .success(false)
                    .message("Password attuale non corretta")
                    .build();
        }

        String newPassword = request.getNewPassword();
        if (newPassword.equals(request.getCurrentPassword())) {
            return ResetPasswordResponse.builder()
                    .success(false)
                    .message("La nuova password deve essere diversa dalla password attuale")
                    .build();
        }
        if (!isPasswordStrong(newPassword)) {
            return ResetPasswordResponse.builder()
                    .success(false)
                    .message("La nuova password non rispetta i requisiti di sicurezza "
                            + "(minimo 10 caratteri, numeri, maiuscole, minuscole e caratteri speciali)")
                    .build();
        }

        // 2. Aggiorna la password.
        //    - Utente LDAP (registrato): aggiornamento diretto in LDAP (il provider
        //      LDAP di Keycloak e' in sola lettura, quindi l'Admin API Keycloak
        //      restituirebbe HTTP 400).
        //    - Utente nativo Keycloak (seed del realm): aggiornamento via Admin API.
        if (ldapClient.userExists(username)) {
            ldapClient.updatePassword(username, newPassword);
        } else {
            String keycloakUserId = keycloakAdminClient.findUserIdByUserName(username);
            if (keycloakUserId == null) {
                return ResetPasswordResponse.builder()
                        .success(false)
                        .message("Utente non trovato")
                        .build();
            }
            keycloakAdminClient.updatePassword(keycloakUserId, newPassword);
        }

        // 3. Allinea il DB locale se l'utente e' presente.
        Optional<RegisteredUser> userOpt = repository.findByUsername(username);
        if (userOpt.isPresent()) {
            RegisteredUser user = userOpt.get();
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            user.setPasswordTemporary(false);
            user.setModifiedAt(LocalDateTime.now());
            repository.save(user);
        }

        log.info("Password cambiata per utente '{}'", username);
        return ResetPasswordResponse.builder()
                .success(true)
                .message("Password aggiornata con successo")
                .build();
    }

    private boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 10) {
            return false;
        }
        boolean digit = false;
        boolean lower = false;
        boolean upper = false;
        boolean special = false;
        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) {
                digit = true;
            } else if (Character.isLowerCase(c)) {
                lower = true;
            } else if (Character.isUpperCase(c)) {
                upper = true;
            } else {
                special = true;
            }
        }
        return digit && lower && upper && special;
    }
}
