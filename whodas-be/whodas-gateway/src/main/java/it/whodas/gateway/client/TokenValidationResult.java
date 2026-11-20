package it.whodas.gateway.client;

public record TokenValidationResult(boolean active, String username, long exp, long iat, String sub) {
}