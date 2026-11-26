package it.whodas.document.rest;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.whodas.common.dto.DocumentTemplateDto;
import it.whodas.document.service.DocumentTemplateService;

/**
 * Modello WHODAS 2.0 compilabile: il front-end lo scarica, l'operatore lo
 * compila a schermo o lo stampa, e lo ricarica poi con l'API di import.
 */
@RestController
@RequestMapping("/api/v1/documents")
public class DocumentTemplateController {

    private static final Logger log = LoggerFactory.getLogger(DocumentTemplateController.class);

    private final DocumentTemplateService templateService;

    public DocumentTemplateController(DocumentTemplateService templateService) {
        this.templateService = templateService;
    }

    /** Modelli disponibili. */
    @GetMapping("/templates")
    public ResponseEntity<List<DocumentTemplateDto>> templates() {
        return ResponseEntity.ok(templateService.templates());
    }

    /**
     * Modello WHODAS 2.0 in PDF, come modulo compilabile (campi AcroForm).
     *
     * @param patientId     paziente da usare come riferimento per la testata
     * @param compileTypeId tipo di compilazione (tab_type_compile_quest)
     * @param interviewer   nome dell'intervistatore
     * @param assessmentDate data di compilazione da riportare sul modello
     * @param fillAnswers   riporta sul modello le risposte gia' registrate
     * @param assessmentId  assessment da cui prendere le risposte
     * @param disposition   attachment (scarica) oppure inline (anteprima)
     */
    @GetMapping("/template.pdf")
    public ResponseEntity<Resource> templatePdf(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long compileTypeId,
            @RequestParam(required = false) String interviewer,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate assessmentDate,
            @RequestParam(required = false, defaultValue = "false") boolean fillAnswers,
            @RequestParam(required = false) Long assessmentId,
            @RequestParam(required = false, defaultValue = "attachment") String disposition) {

        Resource pdf = templateService.generate(patientId, compileTypeId, interviewer, assessmentDate,
                fillAnswers, assessmentId);
        String fileName = fileNameOf(patientId, compileTypeId, assessmentDate);
        boolean inline = "inline".equalsIgnoreCase(disposition);

        log.debug("Modello generato: {} byte, disposition {}", pdf.getFilename(), disposition);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(fileName, inline).toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private String fileNameOf(Long patientId, Long compileTypeId, LocalDate assessmentDate) {
        LocalDate suffix = assessmentDate == null ? LocalDate.now() : assessmentDate;
        String scope = patientId != null ? "paziente_" + patientId
                : (compileTypeId != null ? "tipo_" + compileTypeId : "vuoto");
        return "whodas2_modello_" + scope + "_" + suffix + ".pdf";
    }

    private ContentDisposition contentDisposition(String fileName, boolean inline) {
        // filename UTF-8: Spring genera sia filename sia filename*, cosi' il nome
        // resta corretto anche su browser e sistemi non latini.
        return (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(fileName, StandardCharsets.UTF_8)
                .build();
    }
}