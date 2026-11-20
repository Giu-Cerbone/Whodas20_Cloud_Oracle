package it.whodas.patient.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    public PatientDto create(PatientDto dto, String registeredUserUid) {
        requireNames(dto);
        if (StringUtils.hasText(registeredUserUid)
                && dto.getFiscalCode() != null && !dto.getFiscalCode().isBlank()
                && repository.existsByFiscalCodeAndRegisteredUserUid(dto.getFiscalCode(), registeredUserUid)) {
            throw new WhodasException(
                    "Patient with fiscal code already exists: " + dto.getFiscalCode(),
                    "PATIENT_FISCAL_CODE_EXISTS");
        }

        Patient entity = new Patient();
        apply(dto, entity);
        entity.setRegisteredUserUid(registeredUserUid);
        return toDto(repository.save(entity));
    }

    public PatientDto update(Long id, PatientDto dto, String registeredUserUid) {
        Patient existing = findEntity(id, registeredUserUid);
        if (dto.getFirstName() == null || dto.getFirstName().isBlank()) {
            throw new WhodasException("First name is required", "VALIDATION_ERROR");
        }
        if (dto.getLastName() == null || dto.getLastName().isBlank()) {
            throw new WhodasException("Last name is required", "VALIDATION_ERROR");
        }
        if (StringUtils.hasText(registeredUserUid)
                && dto.getFiscalCode() != null && !dto.getFiscalCode().isBlank()
                && !dto.getFiscalCode().equals(existing.getFiscalCode())
                && repository.existsByFiscalCodeAndRegisteredUserUid(dto.getFiscalCode(), registeredUserUid)) {
            throw new WhodasException(
                    "Patient with fiscal code already exists: " + dto.getFiscalCode(),
                    "PATIENT_FISCAL_CODE_EXISTS");
        }
        apply(dto, existing);
        return toDto(repository.save(existing));
    }

    public PatientDto findById(Long id, String registeredUserUid) {
        return toDto(findEntity(id, registeredUserUid));
    }

    public PatientDto findByFiscalCode(String fiscalCode, String registeredUserUid) {
        Patient patient = repository.findByFiscalCode(fiscalCode);
        if (patient == null) {
            throw new WhodasException("Patient not found with fiscal code: " + fiscalCode, "PATIENT_NOT_FOUND");
        }
        if (StringUtils.hasText(registeredUserUid)
                && !registeredUserUid.equals(patient.getRegisteredUserUid())) {
            throw new WhodasException("Patient not found with fiscal code: " + fiscalCode, "PATIENT_NOT_FOUND");
        }
        return toDto(patient);
    }

    public List<PatientDto> searchByLastName(String lastName, String registeredUserUid) {
        List<Patient> list = StringUtils.hasText(registeredUserUid)
                ? repository.findByLastNameContainingAndRegisteredUserUid(lastName, registeredUserUid)
                : repository.findByLastNameContaining(lastName);
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<PatientDto> searchByFirstName(String firstName, String registeredUserUid) {
        List<Patient> list = StringUtils.hasText(registeredUserUid)
                ? repository.findByFirstNameContainingAndRegisteredUserUid(firstName, registeredUserUid)
                : repository.findByFirstNameContaining(firstName);
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<PatientDto> findAll(String registeredUserUid) {
        List<Patient> list = StringUtils.hasText(registeredUserUid)
                ? repository.findAllByRegisteredUserUid(registeredUserUid)
                : repository.findAll();
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    public void delete(Long id, String registeredUserUid) {
        boolean exists = StringUtils.hasText(registeredUserUid)
                ? repository.existsByIdAndRegisteredUserUid(id, registeredUserUid)
                : repository.existsById(id);
        if (!exists) {
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

    private Patient findEntity(Long id, String registeredUserUid) {
        if (StringUtils.hasText(registeredUserUid)) {
            return repository.findByIdAndRegisteredUserUid(id, registeredUserUid)
                    .orElseThrow(() -> new WhodasException("Patient not found: " + id, "PATIENT_NOT_FOUND"));
        }
        return repository.findById(id)
                .orElseThrow(() -> new WhodasException("Patient not found: " + id, "PATIENT_NOT_FOUND"));
    }

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
        dto.setRegisteredUserUid(entity.getRegisteredUserUid());
        return dto;
    }
}
