package it.whodas.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Unica eccezione di dominio dell'applicazione.
 * Le istanze vengono intercettate da {@link GlobalExceptionHandler}, che
 * restituisce il body {timestamp, status, error, message, code, path}.
 */
public class WhodasException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final String CODE_NOT_FOUND_SUFFIX = "_NOT_FOUND";
    public static final String CODE_EXISTS_SUFFIX = "_EXISTS";

    private final String code;
    private final HttpStatus httpStatus;

    public WhodasException(String message) {
        this(message, "WHODAS_ERROR");
    }

    /**
     * Lo stato HTTP viene dedotto dal codice, cosi' i servizi possono
     * limitarsi a descrivere l'errore senza ripetere lo status:
     *   *_NOT_FOUND -> 404, *_EXISTS -> 409, altrimenti 400.
     */
    public WhodasException(String message, String code) {
        this(message, code, statusFor(code));
    }

    public WhodasException(String message, String code, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    private static HttpStatus statusFor(String code) {
        if (code == null) {
            return HttpStatus.BAD_REQUEST;
        }
        if (code.endsWith(CODE_NOT_FOUND_SUFFIX)) {
            return HttpStatus.NOT_FOUND;
        }
        if (code.endsWith(CODE_EXISTS_SUFFIX)) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.BAD_REQUEST;
    }

    public String getCode() { return code; }
    public HttpStatus getHttpStatus() { return httpStatus; }
}
