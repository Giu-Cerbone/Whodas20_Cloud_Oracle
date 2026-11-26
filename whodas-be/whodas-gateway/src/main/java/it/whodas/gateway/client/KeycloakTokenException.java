package it.whodas.gateway.client;

public class KeycloakTokenException extends RuntimeException {

    public KeycloakTokenException(String message) {
        super(message);
    }
}