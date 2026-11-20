package it.whodas.userservice.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import it.whodas.common.exception.WhodasException;
import it.whodas.userservice.client.KeycloakAdminClient;
import it.whodas.userservice.client.LdapClient;
import it.whodas.userservice.client.LdapClient.LdapUserSpec;
import it.whodas.userservice.domain.RegisteredUser;
import it.whodas.userservice.dto.CheckResponse;
import it.whodas.userservice.dto.RegistrationRequest;
import it.whodas.userservice.dto.RegistrationResponse;
import it.whodas.userservice.dto.UserListItem;
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
}
