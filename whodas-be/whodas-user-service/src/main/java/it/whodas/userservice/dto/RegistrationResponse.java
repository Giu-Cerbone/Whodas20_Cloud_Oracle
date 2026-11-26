package it.whodas.userservice.dto;

public record RegistrationResponse(boolean success, String message, String username) {
}
