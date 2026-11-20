package it.whodas.document.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import it.whodas.common.exception.WhodasException;

/**
 * Errori specifici del caricamento dei documenti.
 *
 * Il gestore globale di whodas-common traduce ogni eccezione in una risposta
 * 400: per l'upload un file troppo grande o un formato non valido sono errori
 * della richiesta, ma 413 e 415 descrivono molto meglio il problema all'utente.
 * Le altre eccezioni restano al gestore globale.
 */
@RestControllerAdvice(assignableTypes = {DocumentImportController.class, DocumentTemplateController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DocumentExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(DocumentExceptionHandler.class);

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> tooLarge(MaxUploadSizeExceededException ex) {
        log.info("Documento rifiutato: supera la dimensione massima consentita");
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "DOCUMENT_TOO_LARGE",
                "Il file supera la dimensione massima consentita (20 MB).");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> missingPart(MissingServletRequestPartException ex) {
        return build(HttpStatus.BAD_REQUEST, "MISSING_FILE",
                "Il documento da caricare non e' stato ricevuto: il campo 'file' e' obbligatorio.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> missingParameter(
            MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER",
                "Parametro obbligatorio mancante: " + ex.getParameterName());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> wrongMediaType(
            HttpMediaTypeNotSupportedException ex) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "Tipo di contenuto non ammesso: il documento deve essere un PDF, un Word o un Excel.");
    }

    @ExceptionHandler(MultipartException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> multipart(MultipartException ex) {
        log.info("Richiesta multipart non leggibile: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "MULTIPART_ERROR",
                "Richiesta di caricamento non valida: " + ex.getMessage());
    }

    @ExceptionHandler(WhodasException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> document(WhodasException ex) {
        HttpStatus status = ex.getHttpStatus() == null ? HttpStatus.BAD_REQUEST : ex.getHttpStatus();
        return build(status, ex.getCode(), ex.getMessage());
    }

    private org.springframework.http.ResponseEntity<ErrorResponse> build(HttpStatus status, String code,
                                                                        String message) {
        return org.springframework.http.ResponseEntity.status(status)
                .body(new ErrorResponse(java.time.Instant.now(), status.value(), status.getReasonPhrase(),
                        message, code, "/api/v1/documents"));
    }

    /** Corpo della risposta di errore, allineato al formato del progetto. */
    public record ErrorResponse(java.time.Instant timestamp, int status, String error, String message, String code,
                                String path) {
    }
}