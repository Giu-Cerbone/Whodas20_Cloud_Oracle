package it.whodas.patient.rest;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import it.whodas.common.dto.PatientDto;
import it.whodas.patient.service.PatientService;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientService service;

    public PatientController(PatientService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<PatientDto>> getAllPatients() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientDto> getPatient(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/fiscal-code/{fiscalCode}")
    public ResponseEntity<PatientDto> getPatientByFiscalCode(@PathVariable String fiscalCode) {
        return ResponseEntity.ok(service.findByFiscalCode(fiscalCode));
    }

    @GetMapping("/search/last-name")
    public ResponseEntity<List<PatientDto>> searchByLastName(@RequestParam String lastName) {
        return ResponseEntity.ok(service.searchByLastName(lastName));
    }

    @GetMapping("/search/first-name")
    public ResponseEntity<List<PatientDto>> searchByFirstName(@RequestParam String firstName) {
        return ResponseEntity.ok(service.searchByFirstName(firstName));
    }

    @PostMapping
    public ResponseEntity<PatientDto> createPatient(@Valid @RequestBody PatientDto dto) {
        return ResponseEntity.status(201).body(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PatientDto> updatePatient(@PathVariable Long id, @Valid @RequestBody PatientDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePatient(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}