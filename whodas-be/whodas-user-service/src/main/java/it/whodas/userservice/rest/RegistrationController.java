package it.whodas.userservice.rest;

import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import it.whodas.userservice.dto.CheckResponse;
import it.whodas.userservice.dto.ChangePasswordRequest;
import it.whodas.userservice.dto.EmailRequest;
import it.whodas.userservice.dto.ForgotPasswordRequest;
import it.whodas.userservice.dto.ForgotPasswordResponse;
import it.whodas.userservice.dto.RegistrationRequest;
import it.whodas.userservice.dto.RegistrationResponse;
import it.whodas.userservice.dto.ResetPasswordRequest;
import it.whodas.userservice.dto.ResetPasswordResponse;
import it.whodas.userservice.dto.UserListItem;
import it.whodas.userservice.dto.TemporaryPasswordResponse;
import it.whodas.userservice.dto.UserProfileResponse;
import it.whodas.userservice.service.PasswordResetService;
import it.whodas.userservice.client.LdapClient;
import it.whodas.userservice.service.RegistrationService;
import it.whodas.userservice.service.TemplateEmailService;

@RestController
@RequestMapping("/api/v1/registration")
public class RegistrationController {

    private final RegistrationService service;
    private final PasswordResetService passwordResetService;
    private final TemplateEmailService templateEmailService;

    private final LdapClient ldapClient;

    public RegistrationController(RegistrationService service, PasswordResetService passwordResetService,
            TemplateEmailService templateEmailService, LdapClient ldapClient) {
        this.service = service;
        this.passwordResetService = passwordResetService;
        this.templateEmailService = templateEmailService;
        this.ldapClient = ldapClient;
    }

    @GetMapping
    public ResponseEntity<List<UserListItem>> getAllUsers() {
        return ResponseEntity.ok(service.getAllUsers());
    }

    @PostMapping("/sync")
    public ResponseEntity<String> syncLdap() {
        service.syncLdap();
        return ResponseEntity.ok("Sincronizzazione LDAP avviata");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ResetPasswordResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.resetPassword(request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ResetPasswordResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(service.changePassword(request));
    }

    @PostMapping("/send-reset-email")
    public ResponseEntity<ForgotPasswordResponse> sendResetEmail(@Valid @RequestBody EmailRequest request) {
        String value = request.getUsernameOrEmail() == null ? "" : request.getUsernameOrEmail().trim().toLowerCase();
        if (value.equals("whodas_user") || value.equals("whodas_user@example.com") || value.equals("whodas_user@prava.lin")) {
            return ResponseEntity.ok(ForgotPasswordResponse.builder()
                    .success(false)
                    .message("Per l'utenza di sistema o utenza speciale non è possibile reimpostare la password.")
                    .build());
        }

        Optional<RegistrationService.ResetRecipient> recipient = service.resolveResetRecipient(request.getUsernameOrEmail());
        if (recipient.isEmpty()) {
            return ResponseEntity.ok(ForgotPasswordResponse.builder()
                    .success(false)
                    .message("Username/email non risultano registrati. Verifica i dati inseriti.")
                    .build());
        }
        RegistrationService.ResetRecipient r = recipient.get();
        try {
            String token = passwordResetService.createResetTokenForUsername(r.username());
            templateEmailService.sendResetPasswordEmail(r.email(), r.firstName(), r.lastName(), r.username(), token);
            return ResponseEntity.ok(ForgotPasswordResponse.builder()
                    .success(true)
                    .message("Email di reset password inviata a " + r.email())
                    .build());
        } catch (Exception e) {
            String detail = e.getMessage() != null ? e.getMessage() : "Errore sconosciuto";
            return ResponseEntity.ok(ForgotPasswordResponse.builder()
                    .success(false)
                    .message("Errore durante l'invio dell'email a " + r.email() + ": " + detail)
                    .build());
        }
    }

    @GetMapping("/reset-username")
    public ResponseEntity<String> resolveResetUsername(@RequestParam("token") String token) {
        return ResponseEntity.ok(passwordResetService.resolveUsernameByToken(token));
    }

    @PostMapping("/generate-temporary-password")
    public ResponseEntity<TemporaryPasswordResponse> generateTemporaryPassword(@RequestParam("token") String token) {
        return ResponseEntity.ok(passwordResetService.generateTemporaryPassword(token));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(@RequestParam("username") String username) {
        return ResponseEntity.ok(service.getProfile(username));
    }

    @GetMapping("/check/cf")
    public ResponseEntity<CheckResponse> checkFiscalCode(@RequestParam("cf") String cf) {
        return ResponseEntity.ok(service.checkFiscalCode(cf));
    }

    @GetMapping("/check/email")
    public ResponseEntity<CheckResponse> checkEmail(@RequestParam("email") String email) {
        return ResponseEntity.ok(service.checkEmail(email));
    }

    @PostMapping
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest request) {
        return ResponseEntity.status(201).body(service.register(request));
    }
}
