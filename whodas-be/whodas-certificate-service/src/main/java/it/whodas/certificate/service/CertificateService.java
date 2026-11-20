package it.whodas.certificate.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.whodas.certificate.domain.Certificate;
import it.whodas.certificate.domain.CertificateDomainScore;
import it.whodas.certificate.repository.CertificateRepository;
import it.whodas.common.dto.CertificateDto;
import it.whodas.common.dto.DomainScoreDto;
import it.whodas.common.exception.WhodasException;

@Service
@Transactional
public class CertificateService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final CertificateRepository repository;

    public CertificateService(CertificateRepository repository) {
        this.repository = repository;
    }

    public CertificateDto create(CertificateDto dto) {
        requireMandatoryFields(dto);

        Certificate entity = new Certificate();
        entity.setCertificateNumber(generateCertificateNumber());
        entity.setAssessmentId(dto.getAssessmentId());
        entity.setPatientId(dto.getPatientId());
        entity.setPatientName(dto.getPatientName());
        entity.setIssueDate(LocalDateTime.now());
        entity.setValidUntil(parseDate(dto.getValidUntil(), "validUntil"));
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : "ISSUED");
        entity.setOverallPercentDisability(dto.getOverallPercentDisability());
        entity.setOverallComplexScore(dto.getOverallComplexScore());
        entity.setCertifyingBody(dto.getCertifyingBody());
        entity.setDoctorName(dto.getDoctorName());
        entity.setDoctorLicense(dto.getDoctorLicense());
        entity.setNotes(dto.getNotes());
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setDomainScores(toEntities(dto.getDomainScores(), entity));

        return toDto(repository.save(entity));
    }

    public CertificateDto findById(Long id) {
        return toDto(findEntity(id));
    }

    public CertificateDto findByCertificateNumber(String certificateNumber) {
        Certificate cert = repository.findByCertificateNumber(certificateNumber);
        if (cert == null) {
            throw new WhodasException("Certificate not found: " + certificateNumber, "CERTIFICATE_NOT_FOUND");
        }
        return toDto(cert);
    }

    public List<CertificateDto> findByPatientId(String patientId) {
        return repository.findByPatientId(patientId).stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<CertificateDto> findByAssessmentId(Long assessmentId) {
        return repository.findByAssessmentId(assessmentId).stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<CertificateDto> findAll() {
        return repository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new WhodasException("Certificate not found: " + id, "CERTIFICATE_NOT_FOUND");
        }
        repository.findById(id).ifPresent(repository::delete);
    }

    private void requireMandatoryFields(CertificateDto dto) {
        if (dto.getAssessmentId() == null) {
            throw new WhodasException("Assessment ID is required", "VALIDATION_ERROR");
        }
        if (dto.getPatientId() == null || dto.getPatientId().isBlank()) {
            throw new WhodasException("Patient ID is required", "VALIDATION_ERROR");
        }
        if (dto.getPatientName() == null || dto.getPatientName().isBlank()) {
            throw new WhodasException("Patient name is required", "VALIDATION_ERROR");
        }
    }

    private List<CertificateDomainScore> toEntities(List<DomainScoreDto> dtos, Certificate certificate) {
        List<CertificateDomainScore> entities = new ArrayList<>();
        if (dtos == null) {
            return entities;
        }
        for (DomainScoreDto d : dtos) {
            CertificateDomainScore ds = new CertificateDomainScore();
            ds.setDomain(d.getDomain());
            ds.setDomainLabel(d.getDomainLabel());
            ds.setDomainLabelIt(d.getDomainLabelIt());
            ds.setItemCount(d.getItemCount());
            ds.setRawScore(d.getRawScore());
            ds.setAverageScore(d.getAverageScore());
            ds.setSimpleScore(d.getSimpleScore());
            ds.setComplexScore(d.getComplexScore());
            ds.setPercentDisability(d.getPercentDisability());
            ds.setCertificate(certificate);
            entities.add(ds);
        }
        return entities;
    }

    private Certificate findEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new WhodasException("Certificate not found: " + id, "CERTIFICATE_NOT_FOUND"));
    }

    private LocalDateTime parseDate(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, ISO);
        } catch (DateTimeParseException ex) {
            throw new WhodasException(
                    field + " must be an ISO-8601 local date-time (e.g. 2026-09-26T10:15:00): " + value,
                    "VALIDATION_ERROR");
        }
    }

    private String generateCertificateNumber() {
        int year = LocalDateTime.now().getYear();
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = String.format("WHODAS-%d-%04d", year, ThreadLocalRandom.current().nextInt(10000));
            if (repository.findByCertificateNumber(candidate) == null) {
                return candidate;
            }
        }
        return String.format("WHODAS-%d-%d", year, System.currentTimeMillis());
    }

    private CertificateDto toDto(Certificate entity) {
        CertificateDto dto = new CertificateDto();
        dto.setId(entity.getId());
        dto.setCertificateNumber(entity.getCertificateNumber());
        dto.setAssessmentId(entity.getAssessmentId());
        dto.setPatientId(entity.getPatientId());
        dto.setPatientName(entity.getPatientName());
        dto.setIssueDate(entity.getIssueDate() != null ? entity.getIssueDate().format(ISO) : null);
        dto.setValidUntil(entity.getValidUntil() != null ? entity.getValidUntil().format(ISO) : null);
        dto.setStatus(entity.getStatus());
        dto.setOverallPercentDisability(entity.getOverallPercentDisability());
        dto.setOverallComplexScore(entity.getOverallComplexScore());
        dto.setCertifyingBody(entity.getCertifyingBody());
        dto.setDoctorName(entity.getDoctorName());
        dto.setDoctorLicense(entity.getDoctorLicense());
        dto.setNotes(entity.getNotes());
        if (entity.getDomainScores() != null) {
            dto.setDomainScores(entity.getDomainScores().stream().map(ds -> {
                DomainScoreDto d = new DomainScoreDto();
                d.setDomain(ds.getDomain());
                d.setDomainLabel(ds.getDomainLabel());
                d.setDomainLabelIt(ds.getDomainLabelIt());
                d.setItemCount(ds.getItemCount());
                d.setRawScore(ds.getRawScore());
                d.setAverageScore(ds.getAverageScore());
                d.setSimpleScore(ds.getSimpleScore());
                d.setComplexScore(ds.getComplexScore());
                d.setPercentDisability(ds.getPercentDisability());
                return d;
            }).collect(Collectors.toList()));
        }
        return dto;
    }
}
