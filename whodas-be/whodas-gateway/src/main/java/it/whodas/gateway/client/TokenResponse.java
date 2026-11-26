package it.whodas.gateway.client;

public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn, String errorDescription) {

    public TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
        this(accessToken, refreshToken, tokenType, expiresIn, null);
    }

    public boolean isValid() {
        return accessToken != null && !accessToken.isBlank();
    }
}