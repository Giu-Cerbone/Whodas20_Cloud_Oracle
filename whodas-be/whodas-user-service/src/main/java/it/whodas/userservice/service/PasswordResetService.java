package it.whodas.userservice.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.whodas.common.exception.WhodasException;
import it.whodas.userservice.client.LdapClient;
import it.whodas.userservice.domain.PasswordResetToken;
import it.whodas.userservice.domain.RegisteredUser;
import it.whodas.userservice.dto.ForgotPasswordRequest;
import it.whodas.userservice.dto.ForgotPasswordResponse;
import it.whodas.userservice.dto.ResetPasswordRequest;
import it.whodas.userservice.dto.TemporaryPasswordResponse;
import it.whodas.userservice.dto.ResetPasswordResponse;
import it.whodas.userservice.repository.PasswordResetTokenRepository;
import it.whodas.userservice.repository.RegisteredUserRepository;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int TOKEN_LENGTH = 32;
    private static final int TOKEN_EXPIRY_HOURS = 24;
    private static final int TEMP_PASSWORD_LENGTH = 12;

    private final PasswordResetTokenRepository tokenRepository;
    private final RegisteredUserRepository userRepository;
    private final LdapClient ldapClient;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;

    @Value("${app.frontend.url:http://localhost}")
    private String frontendUrl;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                RegisteredUserRepository userRepository,
                                LdapClient ldapClient,
                                EmailService emailService,
                                PasswordEncoder passwordEncoder) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.ldapClient = ldapClient;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = new SecureRandom();
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        Optional<RegisteredUser> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.warn("Richiesta reset password per email inesistente: {}", email);
            return ForgotPasswordResponse.builder()
                    .success(true)
                    .message("Se l'email è registrata, riceverai le istruzioni per il reset")
                    .build();
        }

        RegisteredUser user = userOpt.get();

        if ("whodas_user".equalsIgnoreCase(user.getUsername())
                || "whodas_user@prava.lin".equalsIgnoreCase(user.getEmail())
                || "whodas_user@example.com".equalsIgnoreCase(user.getEmail())) {
            log.warn("Reset password rifiutato per utenza di sistema: {}", user.getUsername());
            return ForgotPasswordResponse.builder()
                    .success(false)
                    .message("Per l'utenza whodas_user non è possibile reimpostare la password.")
                    .build();
        }

        tokenRepository.deleteByUsernameAndUsedFalseAndExpiryDateAfter(user.getUsername(), LocalDateTime.now());

        String token = generateToken();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .expiryDate(expiryDate)
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
        tokenRepository.save(resetToken);

        String resetLink = frontendUrl + "/reset-password?token=" + token;

        if (emailEnabled && emailService != null) {
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink, TOKEN_EXPIRY_HOURS);
        } else {
            log.info("=== PASSWORD RESET LINK (email disabilitata) ===");
            log.info("Utente: {}", user.getUsername());
            log.info("Email: {}", user.getEmail());
            log.info("Link reset: {}", resetLink);
            log.info("================================================");
        }

        return ForgotPasswordResponse.builder()
                .success(true)
                .message("Se l'email è registrata, riceverai le istruzioni per il reset")
                .build();
    }

    @Transactional
    public TemporaryPasswordResponse generateTemporaryPassword(String token) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return TemporaryPasswordResponse.builder()
                    .success(false)
                    .message("Token non valido o scaduto")
                    .build();
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.getUsed()) {
            return TemporaryPasswordResponse.builder()
                    .success(false)
                    .message("Token gia' utilizzato")
                    .build();
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return TemporaryPasswordResponse.builder()
                    .success(false)
                    .message("Token scaduto")
                    .build();
        }

        Optional<RegisteredUser> userOpt = userRepository.findByUsername(resetToken.getUsername());
        if (userOpt.isEmpty()) {
            return TemporaryPasswordResponse.builder()
                    .success(false)
                    .message("Utente non trovato")
                    .build();
        }

        RegisteredUser user = userOpt.get();

        // Genera password temporanea
        String tempPassword = generateTempPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setPasswordTemporary(true);
        user.setModifiedAt(LocalDateTime.now());
        userRepository.save(user);

        // Aggiorna LDAP
        try {
            ldapClient.updatePassword(user.getUsername(), tempPassword);
        } catch (Exception e) {
            log.warn("Impossibile aggiornare password LDAP per {}: {}", user.getUsername(), e.getMessage());
        }

        // Marca token come usato
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("Password temporanea generata per utente: {}", user.getUsername());

        return TemporaryPasswordResponse.builder()
                .success(true)
                .temporaryPassword(tempPassword)
                .message("Password temporanea generata. Copiala e conservala.")
                .build();
    }

    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResetPasswordResponse.builder()
                    .success(false)
                    .message("Le password non coincidono")
                    .build();
        }

        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(request.getToken());
        if (tokenOpt.isEmpty()) {
            return ResetPasswordResponse.builder()
                    .success(false)
                    .message("Token non valido o scaduto")
                    .build();
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.getUsed()) {
            return ResetPasswordResponse.builder()
                    .success(false)
                    .message("Token già utilizzato")
                    .build();
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return ResetPasswordResponse.builder()
                    .success(false)
                    .message("Token scaduto")
                    .build();
        }

        Optional<RegisteredUser> userOpt = userRepository.findByUsername(resetToken.getUsername());
        if (userOpt.isEmpty()) {
            return ResetPasswordResponse.builder()
                    .success(false)
                    .message("Utente non trovato")
                    .build();
        }

        RegisteredUser user = userOpt.get();

        // La nuova password non deve coincidere con quella temporanea corrente
        if (user.getPasswordTemporary() != null && user.getPasswordTemporary()
                && passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            return ResetPasswordResponse.builder()
                    .success(false)
                    .message("La nuova password non puo' essere uguale a quella temporanea")
                    .build();
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordTemporary(false);
        user.setModifiedAt(LocalDateTime.now());
        userRepository.save(user);

        // Aggiorna anche LDAP
        try {
            ldapClient.updatePassword(user.getUsername(), request.getNewPassword());
        } catch (Exception e) {
            log.error("Errore aggiornamento password LDAP per utente {}: {}", user.getUsername(), e.getMessage());
        }

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("Password reset completato per utente: {}", user.getUsername());

        return ResetPasswordResponse.builder()
                .success(true)
                .message("Password reimpostata con successo")
                .build();
    }

    /**
     * Genera una password temporanea casuale alfanumerica.
     */
    private String generateTempPassword() {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String all = upper + lower + digits;
        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        // Almeno una maiuscola, una minuscola, una cifra
        sb.append(upper.charAt(secureRandom.nextInt(upper.length())));
        sb.append(lower.charAt(secureRandom.nextInt(lower.length())));
        sb.append(digits.charAt(secureRandom.nextInt(digits.length())));
        for (int i = 3; i < TEMP_PASSWORD_LENGTH; i++) {
            sb.append(all.charAt(secureRandom.nextInt(all.length())));
        }
        return sb.toString();
    }

    private String generateToken() {
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public String resolveUsernameByToken(String token) {
        return tokenRepository.findByToken(token)
            .filter(t -> !t.getUsed() && t.getExpiryDate().isAfter(LocalDateTime.now()))
            .flatMap(t -> userRepository.findByUsername(t.getUsername()))
            .map(u -> u.getUsername())
            .orElse("");
    }

    @Transactional
    public String createResetTokenForUsername(String username) {
        tokenRepository.deleteByUsernameAndUsedFalseAndExpiryDateAfter(username, LocalDateTime.now());
        String token = generateToken();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS);
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .username(username)
                .email("")
                .expiryDate(expiryDate)
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
        tokenRepository.save(resetToken);
        return token;
    }
}