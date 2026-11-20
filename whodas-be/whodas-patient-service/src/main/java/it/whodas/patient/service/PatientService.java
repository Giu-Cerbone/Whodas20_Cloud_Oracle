package it.whodas.patient.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.whodas.common.dto.PatientDto;
import it.whodas.common.exception.WhodasException;
import it.whodas.patient.domain.Patient;
import it.whodas.patient.repository.PatientRepository;

@Service
@Transactional
public class PatientService {

    private final PatientRepository repository;

    public PatientService(PatientRepository repository) {
        this.repository = repository;
    }

    public PatientDto create(PatientDto dto) {
        requireNames(dto);
        if (dto.getFiscalCode() != null && !dto.getFiscalCode().isBlank()
                && repository.existsByFiscalCode(dto.getFiscalCode())) {
            throw new WhodasException(
                    "Patient with fiscal code already exists: " + dto.getFiscalCode(),
                    "PATIENT_FISCAL_CODE_EXISTS");
        }

        Patient entity = new Patient();
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public PatientDto update(Long id, PatientDto dto) {
        Patient existing = findEntity(id);
        if (dto.getFirstName() == null || dto.getFirstName().isBlank()) {
            throw new WhodasException("First name is required", "VALIDATION_ERROR");
        }
        if (dto.getLastName() == null || dto.getLastName().isBlank()) {
            throw new WhodasException("Last name is required", "VALIDATION_ERROR");
        }
        // Un cambio di codice fiscale deve restare unico.
        if (dto.getFiscalCode() != null && !dto.getFiscalCode().isBlank()
                && !dto.getFiscalCode().equals(existing.getFiscalCode())
                && repository.existsByFiscalCode(dto.getFiscalCode())) {
            throw new WhodasException(
                    "Patient with fiscal code already exists: " + dto.getFiscalCode(),
                    "PATIENT_FISCAL_CODE_EXISTS");
        }
        apply(dto, existing);
        return toDto(repository.save(existing));
    }

    public PatientDto findById(Long id) {
        return toDto(findEntity(id));
    }

    public PatientDto findByFiscalCode(String fiscalCode) {
        Patient patient = repository.findByFiscalCode(fiscalCode);
        if (patient == null) {
            throw new WhodasException("Patient not found with fiscal code: " + fiscalCode, "PATIENT_NOT_FOUND");
        }
        return toDto(patient);
    }

    public List<PatientDto> searchByLastName(String lastName) {
        return repository.findByLastNameContaining(lastName).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<PatientDto> searchByFirstName(String firstName) {
        return repository.findByFirstNameContaining(firstName).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<PatientDto> findAll() {
        return repository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new WhodasException("Patient not found: " + id, "PATIENT_NOT_FOUND");
        }
        repository.findById(id).ifPresent(repository::delete);
    }

    private void requireNames(PatientDto dto) {
        if (dto.getFirstName() == null || dto.getFirstName().isBlank()) {
            throw new WhodasException("First name is required", "VALIDATION_ERROR");
        }
        if (dto.getLastName() == null || dto.getLastName().isBlank()) {
            throw new WhodasException("Last name is required", "VALIDATION_ERROR");
        }
    }

    private void apply(PatientDto dto, Patient entity) {
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setFiscalCode(blankToNull(dto.getFiscalCode()));
        entity.setBirthDate(parseDate(dto.getBirthDate()));
        entity.setBirthPlace(dto.getBirthPlace());
        entity.setGender(dto.getGender());
        entity.setAddress(dto.getAddress());
        entity.setPhone(dto.getPhone());
        entity.setEmail(dto.getEmail());
        entity.setHealthCondition(dto.getHealthCondition());
        entity.setNotes(dto.getNotes());
    }

    private Patient findEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new WhodasException("Patient not found: " + id, "PATIENT_NOT_FOUND"));
    }

    /**
     * PatientDto.birthDate e' una Stringa (il form HTML la invia come yyyy-MM-dd)
     * mentre l'entita' usa LocalDate: la conversione va fatta esplicitamente,
     * altrimenti ModelMapper STRICT scarta il valore e la data si perdeva.
     */
    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new WhodasException(
                    "birthDate must be an ISO date (yyyy-MM-dd): " + value, "VALIDATION_ERROR");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private PatientDto toDto(Patient entity) {
        PatientDto dto = new PatientDto();
        dto.setId(entity.getId());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setFiscalCode(entity.getFiscalCode());
        dto.setBirthDate(entity.getBirthDate() != null ? entity.getBirthDate().toString() : null);
        dto.setBirthPlace(entity.getBirthPlace());
        dto.setGender(entity.getGender());
        dto.setAddress(entity.getAddress());
        dto.setPhone(entity.getPhone());
        dto.setEmail(entity.getEmail());
        dto.setHealthCondition(entity.getHealthCondition());
        dto.setNotes(entity.getNotes());
        return dto;
    }
}
