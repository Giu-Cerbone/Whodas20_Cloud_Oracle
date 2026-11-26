package it.whodas.assessment.rest;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import it.whodas.assessment.service.AssessmentService;
import it.whodas.assessment.service.pdf.AssessmentPdfService;
import it.whodas.common.dto.AssessmentDto;
import it.whodas.common.dto.AssessmentResultDto;

@RestController
@RequestMapping("/api/v1/assessments")
public class AssessmentController {

    private static final String USERNAME_HEADER = "X-Username";

    private final AssessmentService service;
    private final AssessmentPdfService pdfService;

    public AssessmentController(AssessmentService service, AssessmentPdfService pdfService) {
        this.service = service;
        this.pdfService = pdfService;
    }

    private String resolveUserUid(@RequestHeader(value = USERNAME_HEADER, required = false) String xUsername) {
        if (xUsername == null || xUsername.isBlank()) {
            return null;
        }
        return xUsername;
    }

    @GetMapping
    public ResponseEntity<List<AssessmentDto>> getAllAssessments(
            @RequestHeader(value = USERNAME_HEADER, required = false) String xUsername) {
        String uid = resolveUserUid(xUsername);
        return ResponseEntity.ok(service.findAll(uid));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssessmentDto> getAssessment(@PathVariable Long id,
            @RequestHeader(value = USERNAME_HEADER, required = false) String xUsername) {
        String uid = resolveUserUid(xUsername);
        return ResponseEntity.ok(service.findById(id, uid));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<AssessmentDto>> getAssessmentsByPatient(@PathVariable String patientId,
            @RequestHeader(value = USERNAME_HEADER, required = false) String xUsername) {
        String uid = resolveUserUid(xUsername);
        return ResponseEntity.ok(service.findByPatientId(patientId, uid));
    }

    @PostMapping
    public ResponseEntity<AssessmentDto> createAssessment(@Valid @RequestBody AssessmentDto dto,
            @RequestHeader(value = USERNAME_HEADER, required = false) String xUsername) {
        String uid = resolveUserUid(xUsername);
        return ResponseEntity.status(201).body(service.create(dto, uid));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssessmentDto> updateAssessment(@PathVariable Long id, @Valid @RequestBody AssessmentDto dto,
            @RequestHeader(value = USERNAME_HEADER, required = false) String xUsername) {
        String uid = resolveUserUid(xUsername);
        return ResponseEntity.ok(service.update(id, dto, uid));
    }

    @PutMapping("/{id}/submit")
    public ResponseEntity<AssessmentResultDto> submitAndScore(@PathVariable Long id,
            @RequestHeader(value = USERNAME_HEADER, required = false) String xUsername) {
        String uid = resolveUserUid(xUsername);
        return ResponseEntity.ok(service.submitAndScore(id, uid));
    }

    @PutMapping("/{id}/score")
    public ResponseEntity<AssessmentResultDto> scoreAssessment(@PathVariable Long id,
            @RequestHeader(value = USERNAME_HEADER, required = false) String xUsername) {
        String uid = resolveUserUid(xUsername);
        return ResponseEntity.ok(service.submitAndScore(id, uid));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<Resource> downloadPdf(@PathVariable Long id,
            @RequestHeader(value = USERNAME_HEADER, required = false) String xUsername) {
        String uid = resolveUserUid(xUsername);
        Resource pdf = pdfService.generatePdf(id);
        AssessmentDto assessment = service.findById(id, uid);
        String filename = buildFilename(assessment);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/{id}/pdf/view")
    public ResponseEntity<Resource> viewPdf(@PathVariable Long id,
            @RequestHeader(value = USERNAME_HEADER, required = false) String xUsername) {
        String uid = resolveUserUid(xUsername);
        Resource pdf = pdfService.generatePdf(id);
        AssessmentDto assessment = service.findById(id, uid);
        String filename = buildFilename(assessment);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAssessment(@PathVariable Long id,
            @RequestHeader(value = USERNAME_HEADER, required = false) String xUsername) {
        String uid = resolveUserUid(xUsername);
        service.delete(id, uid);
        return ResponseEntity.noContent().build();
    }

    private String buildFilename(AssessmentDto assessment) {
        String fiscalCode = "NOCF";
        String dateStr = assessment.getUpdatedAt() != null
                ? assessment.getUpdatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                : java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String status = assessment.getStatus() != null ? assessment.getStatus().toLowerCase() : "unknown";
        return String.format("%s_questionario_%s_%s.pdf", fiscalCode, dateStr, status);
    }
}
