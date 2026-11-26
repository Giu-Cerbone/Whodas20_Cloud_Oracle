package it.whodas.document.rest;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import it.whodas.common.dto.DocumentImportResultDto;
import it.whodas.common.dto.DocumentImportSummaryDto;
import it.whodas.common.exception.WhodasException;
import it.whodas.document.config.DocumentServiceProperties;
import it.whodas.document.extractor.DocumentFormat;
import it.whodas.document.service.DocumentImportService;

/**
 * Import di un documento WHODAS 2.0 compilato (PDF, Word, Excel) e storico
 * degli import effettuati.
 */
@RestController
@RequestMapping("/api/v1/documents")
public class DocumentImportController {

    private static final Logger log = LoggerFactory.getLogger(DocumentImportController.class);

    private final DocumentImportService importService;
    private final DocumentServiceProperties properties;

    public DocumentImportController(DocumentImportService importService, DocumentServiceProperties properties) {
        this.importService = importService;
        this.properties = properties;
    }

    /**
     * Carica un documento compilato: ne estrae il contenuto, riconosce i dati
     * del paziente e le risposte ai 36 item e li scrive in modo relazionale
     * chiamando i servizi proprietari dei dati.
     */
    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentImportResultDto> importDocument(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long compileTypeId,
            @RequestParam(required = false) String interviewer,
            @RequestParam(required = false) Boolean createPatientIfMissing,
            @RequestParam(required = false) Boolean saveAssessment,
            @RequestParam(required = false) Boolean submitAndScore) {

        var defaults = properties.getImportDefaults();
        DocumentImportService.ImportCommand command = new DocumentImportService.ImportCommand(
                patientId,
                compileTypeId,
                interviewer,
                createPatientIfMissing == null ? defaults.isCreatePatientIfMissing() : createPatientIfMissing,
                saveAssessment == null ? defaults.isSaveAssessment() : saveAssessment,
                submitAndScore == null ? defaults.isSubmitAndScore() : submitAndScore);

        DocumentImportResultDto result = importService.importDocument(file, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /** Storico degli import, dal piu' recente. */
    @GetMapping("/imports")
    public ResponseEntity<List<DocumentImportSummaryDto>> imports(
            @RequestParam(required = false, defaultValue = "20") int limit,
            @RequestParam(required = false, defaultValue = "0") int offset) {
        return ResponseEntity.ok(importService.summary(limit, offset));
    }

    /** Dettaglio di un import: valori riconosciuti con confidenza e sorgente. */
    @GetMapping("/imports/{id}")
    public ResponseEntity<DocumentImportResultDto> detail(@PathVariable Long id) {
        return ResponseEntity.ok(importService.detail(id));
    }

    /** Documento originale, per rivederlo o stamparlo. */
    @GetMapping("/imports/{id}/file")
    public ResponseEntity<byte[]> originalFile(@PathVariable Long id) {
        DocumentImportService.StoredFile file = importService.storedFile(id)
                .orElseThrow(() -> new WhodasException("Documento originale non disponibile per l'import " + id,
                        "DOCUMENT_FILE_NOT_AVAILABLE", HttpStatus.NOT_FOUND));
        MediaType mediaType = file.contentType() == null || file.contentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(file.contentType());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(mediaType)
                .body(file.content());
    }

    @DeleteMapping("/imports/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        importService.delete(id);
        log.debug("Import {} eliminato", id);
        return ResponseEntity.noContent().build();
    }

    /** Formati accettati: utile al front-end per validare il file lato client. */
    @GetMapping("/formats")
    public ResponseEntity<List<String>> formats() {
        return ResponseEntity.ok(java.util.Arrays.stream(DocumentFormat.values())
                .map(DocumentFormat::extension)
                .toList());
    }
}