package it.whodas.patient.rest;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import it.whodas.common.dto.PatientDto;
import it.whodas.common.exception.WhodasException;
import it.whodas.patient.service.PatientService;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientService service;

    public PatientController(PatientService service) {
        this.service = service;
    }

    private String resolveUserUid(@RequestHeader(name = "X-Username", required = false) String xUsername) {
        if (xUsername == null || xUsername.isBlank()) {
            return null;
        }
        return xUsername;
    }

    @GetMapping
    public ResponseEntity<List<PatientDto>> getAllPatients(
            @RequestHeader(name = "X-Username", required = false) String xUsername) {
        String uid = resolveUserUid(xUsername);
        return ResponseEntity.ok(service.findAll(uid));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientDto> getPatient(@PathVariable Long id,
            @RequestHeader(name = "X-Username", required = false) String xUsername) {
        String uid = resolveUserUid(xUsername);
        return ResponseEntity.ok(service.findById(id, uid));
    }

    @GetMapping("/fiscal-code/{fiscalCode}")
    public ResponseEntity<PatientDto> getPatientByFiscalCode(@PathVariable String fiscalCode,
            @RequestHeader(name = "X-Username", required = false) String xUsername) {
        String uid = resolveUserUid(xUsername);
        return ResponseEntity.ok(service.findByFiscalCode(fiscalCode, uid));
    }

    @GetMapping("/search/last-name")
    public ResponseEntity<List<PatientDto>> searchByLastName(@RequestParam String lastName,
            @RequestHeader(name = "X-Username", required = false) String xUsername) {
        String uid = resolveUserUid(xUsername);
        return ResponseEntity.ok(service.searchByLastName(lastName, uid));
    }

    @GetMapping("/search/first-name")
    public ResponseEntity<List<PatientDto>> searchByFirstName(@RequestParam String firstName,
            @RequestHeader(name = "X-Username", required = false) String xUsername) {
        String uid = resolveUserUid(xUsername);
        return ResponseEntity.ok(service.searchByFirstName(firstName, uid));
    }

    @PostMapping
    public ResponseEntity<PatientDto> createPatient(@Valid @RequestBody PatientDto dto,
            @RequestHeader(name = "X-Username", required = false) String xUsername) {
        String uid = resolveUserUid(xUsername);
        return ResponseEntity.status(201).body(service.create(dto, uid));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PatientDto> updatePatient(@PathVariable Long id, @Valid @RequestBody PatientDto dto,
            @RequestHeader(name = "X-Username", required = false) String xUsername) {
        String uid = resolveUserUid(xUsername);
        return ResponseEntity.ok(service.update(id, dto, uid));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePatient(@PathVariable Long id,
            @RequestHeader(name = "X-Username", required = false) String xUsername) {
        String uid = resolveUserUid(xUsername);
        service.delete(id, uid);
        return ResponseEntity.noContent().build();
    }
}
