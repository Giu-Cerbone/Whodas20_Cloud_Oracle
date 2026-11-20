package it.whodas.gateway.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Lettura dell'header Authorization in HTTP Basic.
 *
 * L'unico canale ammesso per autenticare una richiesta e' l'header
 * Authorization con schema Basic, che trasporta base64(username:token) dove il
 * token e' quello restituito dalla risposta di login. Non esistono header
 * dedicati al token (ne' TOKEN_ACCESS ne' TOKEN).
 */
public final class BasicCredentials {

    private static final String BASIC_PREFIX = "Basic ";

    private BasicCredentials() {
    }

    /**
     * @param authorization valore grezzo dell'header Authorization
     * @return il token di accesso in esso contenuto, oppure null se l'header e'
     *         assente, non usa lo schema Basic, non e' un base64 valido oppure
     *         non contiene la coppia username:token
     */
    public static String extractToken(String authorization) {
        if (authorization == null
                || !authorization.regionMatches(true, 0, BASIC_PREFIX, 0, BASIC_PREFIX.length())) {
            return null;
        }

        String credential = authorization.substring(BASIC_PREFIX.length()).trim();
        if (credential.isEmpty()) {
            return null;
        }

        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(credential), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }

        int separator = decoded.indexOf(':');
        if (separator < 0) {
            return null;
        }

        String token = decoded.substring(separator + 1).trim();
        return token.isEmpty() ? null : token;
    }
}