package it.whodas.certificate.rest;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import it.whodas.certificate.client.NotificationClient;
import it.whodas.certificate.service.CertificateService;
import it.whodas.common.dto.CertificateDto;

@RestController
@RequestMapping("/api/v1/certificates")
public class CertificateController {

    private static final String USERNAME_HEADER = "X-Username";

    private final CertificateService service;
    private final NotificationClient notificationClient;

    public CertificateController(CertificateService service, NotificationClient notificationClient) {
        this.service = service;
        this.notificationClient = notificationClient;
    }

    @GetMapping
    public ResponseEntity<List<CertificateDto>> getAllCertificates(
            @RequestHeader(value = USERNAME_HEADER, required = false) String username) {
        List<CertificateDto> certificates = service.findAll();
        notificationClient.sendDataLoadedNotification(username, "GET /api/v1/certificates");
        return ResponseEntity.ok(certificates);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CertificateDto> getCertificate(@PathVariable Long id,
            @RequestHeader(value = USERNAME_HEADER, required = false) String username) {
        CertificateDto certificate = service.findById(id);
        notificationClient.sendDataLoadedNotification(username, "GET /api/v1/certificates/{id}");
        return ResponseEntity.ok(certificate);
    }

    @GetMapping("/number/{certificateNumber}")
    public ResponseEntity<CertificateDto> getCertificateByNumber(@PathVariable String certificateNumber,
            @RequestHeader(value = USERNAME_HEADER, required = false) String username) {
        CertificateDto certificate = service.findByCertificateNumber(certificateNumber);
        notificationClient.sendDataLoadedNotification(username, "GET /api/v1/certificates/number/{certificateNumber}");
        return ResponseEntity.ok(certificate);
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<CertificateDto>> getCertificatesByPatient(@PathVariable String patientId,
            @RequestHeader(value = USERNAME_HEADER, required = false) String username) {
        List<CertificateDto> certificates = service.findByPatientId(patientId);
        notificationClient.sendDataLoadedNotification(username, "GET /api/v1/certificates/patient/{patientId}");
        return ResponseEntity.ok(certificates);
    }

    @GetMapping("/assessment/{assessmentId}")
    public ResponseEntity<List<CertificateDto>> getCertificatesByAssessment(@PathVariable Long assessmentId,
            @RequestHeader(value = USERNAME_HEADER, required = false) String username) {
        List<CertificateDto> certificates = service.findByAssessmentId(assessmentId);
        notificationClient.sendDataLoadedNotification(username, "GET /api/v1/certificates/assessment/{assessmentId}");
        return ResponseEntity.ok(certificates);
    }

    @PostMapping
    public ResponseEntity<CertificateDto> createCertificate(@Valid @RequestBody CertificateDto dto,
            @RequestHeader(value = USERNAME_HEADER, required = false) String username) {
        CertificateDto created = service.create(dto);
        notificationClient.sendDataLoadedNotification(username, "POST /api/v1/certificates");
        return ResponseEntity.status(201).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCertificate(@PathVariable Long id,
            @RequestHeader(value = USERNAME_HEADER, required = false) String username) {
        service.delete(id);
        notificationClient.sendDataLoadedNotification(username, "DELETE /api/v1/certificates/{id}");
        return ResponseEntity.noContent().build();
    }
}
