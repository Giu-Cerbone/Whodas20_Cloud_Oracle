package it.whodas.common.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

/**
 * Traduce le eccezioni in risposte HTTP coerenti.
 * Senza questo advice ogni errore di dominio finiva nel gestore generico e
 * veniva risposto con 500 INTERNAL_ERROR.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WhodasException.class)
    public ResponseEntity<ErrorResponse> handleWhodasException(WhodasException ex, HttpServletRequest request) {
        return buildResponse(ex.getMessage(), ex.getCode(), ex.getHttpStatus(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "Validation error";
        }
        return buildResponse(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST, request.getRequestURI());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        return buildResponse(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST, request.getRequestURI());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildResponse("Malformed request body", "MALFORMED_REQUEST", HttpStatus.BAD_REQUEST, request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        return buildResponse("Internal server error: " + ex.getMessage(), "INTERNAL_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR, request.getRequestURI());
    }

    private ResponseEntity<ErrorResponse> buildResponse(String message, String code, HttpStatus status, String path) {
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, code, path),
                status);
    }
}
