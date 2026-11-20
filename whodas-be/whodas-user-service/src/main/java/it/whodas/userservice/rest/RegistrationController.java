package it.whodas.userservice.rest;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import it.whodas.userservice.dto.CheckResponse;
import it.whodas.userservice.dto.ForgotPasswordRequest;
import it.whodas.userservice.dto.ForgotPasswordResponse;
import it.whodas.userservice.dto.RegistrationRequest;
import it.whodas.userservice.dto.RegistrationResponse;
import it.whodas.userservice.dto.ResetPasswordRequest;
import it.whodas.userservice.dto.ResetPasswordResponse;
import it.whodas.userservice.dto.UserListItem;
import it.whodas.userservice.service.PasswordResetService;
import it.whodas.userservice.service.RegistrationService;

@RestController
@RequestMapping("/api/v1/registration")
public class RegistrationController {

    private final RegistrationService service;
    private final PasswordResetService passwordResetService;

    public RegistrationController(RegistrationService service, PasswordResetService passwordResetService) {
        this.service = service;
        this.passwordResetService = passwordResetService;
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
