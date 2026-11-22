package it.whodas.assessment.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import it.whodas.assessment.domain.Assessment;
import it.whodas.assessment.domain.ItemCatalogEntry;
import it.whodas.assessment.domain.ItemResponse;
import it.whodas.assessment.domain.PatientReference;
import it.whodas.assessment.repository.AssessmentRepository;
import it.whodas.assessment.repository.ItemCatalogRepository;
import it.whodas.assessment.repository.PatientReferenceRepository;
import it.whodas.common.domain.DomainType;
import it.whodas.common.dto.AssessmentDto;
import it.whodas.common.dto.AssessmentResultDto;
import it.whodas.common.dto.DomainScoreDto;
import it.whodas.common.dto.ItemResponseDto;
import it.whodas.common.exception.WhodasException;

@Service
@Transactional
public class AssessmentService {

    /** Scala WHODAS 2.0: da 0 (nessuna difficolta') a 4 (impossibile). */
    private static final int MAX_ITEM_SCORE = 4;

    private final AssessmentRepository repository;
    private final ItemCatalogRepository itemCatalog;
    private final PatientReferenceRepository patients;

    public AssessmentService(AssessmentRepository repository,
                             ItemCatalogRepository itemCatalog,
                             PatientReferenceRepository patients) {
        this.repository = repository;
        this.itemCatalog = itemCatalog;
        this.patients = patients;
    }

    public AssessmentDto create(AssessmentDto dto, String registeredUserUid) {
        if (dto.getPatientId() == null || dto.getPatientId().isBlank()) {
            throw new WhodasException("Patient ID is required", "VALIDATION_ERROR");
        }
        Assessment entity = new Assessment();
        entity.setPatientId(dto.getPatientId());
        entity.setVersion(dto.getVersion() != null ? dto.getVersion() : "36-ITEM");
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : "DRAFT");
        entity.setMode(dto.getMode() != null ? dto.getMode() : "SELF_ADMINISTERED");
        entity.setLanguage(dto.getLanguage() != null ? dto.getLanguage() : "it-IT");
        entity.setInterviewer(dto.getInterviewer());
        entity.setNotes(dto.getNotes());
        entity.setCompileTypeId(dto.getCompileTypeId());
        entity.setRegisteredUserUid(registeredUserUid);
        entity.setResponses(toEntities(dto.getResponses(), entity));
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return toDto(repository.save(entity));
    }

    public AssessmentDto update(Long id, AssessmentDto dto, String registeredUserUid) {
        Assessment existing = findEntity(id, registeredUserUid);
        if (dto.getPatientId() != null && !dto.getPatientId().isBlank()) {
            existing.setPatientId(dto.getPatientId());
        }
        if (dto.getVersion() != null) existing.setVersion(dto.getVersion());
        if (dto.getStatus() != null) existing.setStatus(dto.getStatus());
        if (dto.getMode() != null) existing.setMode(dto.getMode());
        if (dto.getLanguage() != null) existing.setLanguage(dto.getLanguage());
        if (dto.getInterviewer() != null) existing.setInterviewer(dto.getInterviewer());
        if (dto.getNotes() != null) existing.setNotes(dto.getNotes());
        if (dto.getCompileTypeId() != null) existing.setCompileTypeId(dto.getCompileTypeId());

        if (dto.getResponses() != null) {
            existing.getResponses().clear();
            existing.getResponses().addAll(toEntities(dto.getResponses(), existing));
        }
        existing.setUpdatedAt(LocalDateTime.now());
        return toDto(repository.save(existing));
    }

    public AssessmentResultDto submitAndScore(Long id, String registeredUserUid) {
        Assessment assessment = findEntity(id, registeredUserUid);
        return scoreAndBuild(assessment);
    }

    public AssessmentResultDto scoreAndBuild(Assessment assessment) {
        List<ItemResponse> responses = assessment.getResponses();
        if (responses == null || responses.isEmpty()) {
            throw new WhodasException("No responses found for assessment: " + assessment.getId(), "NO_RESPONSES");
        }
        resolveDomains(responses);

        int totalItems = responses.size();
        int answeredItems = (int) responses.stream().filter(r -> r.getScore() != null).count();
        int missingItems = totalItems - answeredItems;
        int rawScore = responses.stream()
                .filter(r -> r.getScore() != null)
                .mapToInt(ItemResponse::getScore)
                .sum();

        Map<String, List<ItemResponse>> groupedByDomain = responses.stream()
                .collect(Collectors.groupingBy(
                        ItemResponse::getDomain,
                        LinkedHashMap::new,
                        Collectors.toList()));
        List<Map.Entry<String, List<ItemResponse>>> orderedDomains = new ArrayList<>(groupedByDomain.entrySet());
        orderedDomains.sort(Comparator.comparingInt(e -> domainOrder(e.getKey())));

        List<DomainScoreDto> domainScores = new ArrayList<>();
        for (Map.Entry<String, List<ItemResponse>> entry : orderedDomains) {
            String domain = entry.getKey();
            List<ItemResponse> domainResponses = entry.getValue();

            int itemCount = domainResponses.size();
            int domainRawScore = domainResponses.stream()
                    .filter(r -> r.getScore() != null)
                    .mapToInt(ItemResponse::getScore)
                    .sum();

            DomainScoreDto domainScore = new DomainScoreDto();
            domainScore.setDomain(domain);
            domainScore.setDomainLabel(domainLabel(domain, "en"));
            domainScore.setDomainLabelIt(domainLabel(domain, "it"));
            domainScore.setItemCount(itemCount);
            domainScore.setRawScore(domainRawScore);
            domainScore.setAverageScore(itemCount > 0 ? (double) domainRawScore / itemCount : 0);
            domainScore.setSimpleScore((double) domainRawScore);
            domainScore.setComplexScore((double) domainRawScore);
            domainScore.setPercentDisability(
                    itemCount > 0 ? (domainRawScore / (double) (itemCount * MAX_ITEM_SCORE)) * 100 : 0);
            domainScores.add(domainScore);
        }

        double overallAverage = totalItems > 0 ? (double) rawScore / totalItems : 0;
        double overallPercent = totalItems > 0 ? (rawScore / (double) (totalItems * MAX_ITEM_SCORE)) * 100 : 0;

        LocalDateTime now = LocalDateTime.now();
        assessment.setStatus("COMPLETED");
        assessment.setCompletedAt(now);
        assessment.setScoredAt(now);
        assessment.setUpdatedAt(now);
        assessment.setTotalItems(totalItems);
        assessment.setAnsweredItems(answeredItems);
        assessment.setMissingItems(missingItems);
        assessment.setTotalScore((double) rawScore);
        assessment.setOverallSimpleScore((double) rawScore);
        assessment.setOverallComplexScore((double) rawScore);
        assessment.setOverallAverageScore(overallAverage);
        assessment.setOverallPercentDisability(overallPercent);
        assessment.setInterpretation(interpretDisability(overallPercent));

        Assessment saved = repository.save(assessment);

        AssessmentResultDto result = new AssessmentResultDto();
        result.setAssessmentId(saved.getId());
        result.setPatientId(saved.getPatientId());
        result.setPatientName(resolvePatientName(saved.getPatientId()));
        result.setVersion(saved.getVersion());
        result.setStatus(saved.getStatus());
        result.setTotalItems(saved.getTotalItems());
        result.setAnsweredItems(saved.getAnsweredItems());
        result.setMissingItems(saved.getMissingItems());
        result.setOverallSimpleScore(saved.getOverallSimpleScore());
        result.setOverallComplexScore(saved.getOverallComplexScore());
        result.setOverallAverageScore(saved.getOverallAverageScore());
        result.setOverallPercentDisability(saved.getOverallPercentDisability());
        result.setDomainScores(domainScores);
        result.setInterpretation(saved.getInterpretation());
        return result;
    }

    public AssessmentDto findById(Long id, String registeredUserUid) {
        return toDto(findEntity(id, registeredUserUid));
    }

    public AssessmentDto findByIdUnsafe(Long id) {
        return toDto(findEntityUnsafe(id));
    }

    public List<AssessmentDto> findByPatientId(String patientId, String registeredUserUid) {
        List<Assessment> list = StringUtils.hasText(registeredUserUid)
                ? repository.findByPatientIdAndRegisteredUserUid(patientId, registeredUserUid)
                : repository.findByPatientId(patientId);
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<AssessmentDto> findAll(String registeredUserUid) {
        List<Assessment> list = StringUtils.hasText(registeredUserUid)
                ? repository.findAllByRegisteredUserUid(registeredUserUid)
                : repository.findAll();
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    public void delete(Long id, String registeredUserUid) {
        boolean exists = StringUtils.hasText(registeredUserUid)
                ? repository.existsByIdAndRegisteredUserUid(id, registeredUserUid)
                : repository.existsById(id);
        if (!exists) {
            throw new WhodasException("Assessment not found: " + id, "ASSESSMENT_NOT_FOUND");
        }
        repository.findById(id).ifPresent(repository::delete);
    }

    private List<ItemResponse> toEntities(List<ItemResponseDto> dtos, Assessment assessment) {
        if (dtos == null) {
            return new ArrayList<>();
        }
        List<ItemResponse> entities = new ArrayList<>(dtos.size());
        for (ItemResponseDto dto : dtos) {
            if (dto.getItemCode() == null || dto.getItemCode().isBlank()) {
                throw new WhodasException("Item code is required for every response", "VALIDATION_ERROR");
            }
            if (dto.getScore() != null
                    && (dto.getScore() < 0 || dto.getScore() > MAX_ITEM_SCORE)) {
                throw new WhodasException(
                        "Score for item " + dto.getItemCode() + " must be between 0 and " + MAX_ITEM_SCORE,
                        "VALIDATION_ERROR");
            }
            ItemResponse response = new ItemResponse();
            response.setItemCode(dto.getItemCode());
            response.setDomain(dto.getDomain());
            response.setScore(dto.getScore());
            response.setDays(dto.getDays());
            response.setNote(dto.getNote());
            response.setCreatedAt(LocalDateTime.now());
            response.setAssessment(assessment);
            entities.add(response);
        }
        return entities;
    }

    private void resolveDomains(List<ItemResponse> responses) {
        for (ItemResponse response : responses) {
            if (response.getDomain() != null && !response.getDomain().isBlank()) {
                continue;
            }
            ItemCatalogEntry entry = itemCatalog.findByItemCode(response.getItemCode())
                    .orElseThrow(() -> new WhodasException(
                            "Unknown questionnaire item: " + response.getItemCode(), "ITEM_NOT_FOUND"));
            response.setDomain(entry.getDomain());
        }
    }

    private String resolvePatientName(String patientId) {
        if (patientId == null || patientId.isBlank()) {
            return null;
        }
        try {
            return patients.findById(Long.valueOf(patientId)).map(PatientReference::getFullName).orElse(null);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String domainLabel(String domain, String language) {
        return switch (language) {
            case "it" -> domainLabelIt(domain);
            default -> domainLabelEn(domain);
        };
    }

    private String domainLabelEn(String domain) {
        return domainType(domain).map(DomainType::getDescription).orElse(null);
    }

    private String domainLabelIt(String domain) {
        return domainType(domain).map(DomainType::getItalianLabel).orElse(null);
    }

    private Optional<DomainType> domainType(String domain) {
        if (domain == null) {
            return Optional.empty();
        }
        for (DomainType type : DomainType.values()) {
            if (type.name().equals(domain)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    private int domainOrder(String domain) {
        return domainType(domain).map(DomainType::ordinal).orElse(DomainType.values().length);
    }

    private String interpretDisability(double percent) {
        if (percent < 5) return "NONE";
        if (percent < 25) return "MILD";
        if (percent < 50) return "MODERATE";
        if (percent < 75) return "SEVERE";
        return "EXTREME";
    }

    private Assessment findEntity(Long id, String registeredUserUid) {
        if (StringUtils.hasText(registeredUserUid)) {
            return repository.findByIdAndRegisteredUserUid(id, registeredUserUid)
                    .orElseThrow(() -> new WhodasException("Assessment not found: " + id, "ASSESSMENT_NOT_FOUND"));
        }
        return findEntityUnsafe(id);
    }

    private Assessment findEntityUnsafe(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new WhodasException("Assessment not found: " + id, "ASSESSMENT_NOT_FOUND"));
    }

    private AssessmentDto toDto(Assessment entity) {
        AssessmentDto dto = new AssessmentDto();
        dto.setId(entity.getId());
        dto.setPatientId(entity.getPatientId());
        dto.setVersion(entity.getVersion());
        dto.setStatus(entity.getStatus());
        dto.setMode(entity.getMode());
        dto.setLanguage(entity.getLanguage());
        dto.setInterviewer(entity.getInterviewer());
        dto.setNotes(entity.getNotes());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setCompletedAt(entity.getCompletedAt());
        dto.setTotalScore(entity.getTotalScore());
        dto.setOverallPercentDisability(entity.getOverallPercentDisability());
        dto.setOverallSimpleScore(entity.getOverallSimpleScore());
        dto.setOverallComplexScore(entity.getOverallComplexScore());
        dto.setOverallAverageScore(entity.getOverallAverageScore());
        dto.setTotalItems(entity.getTotalItems());
        dto.setAnsweredItems(entity.getAnsweredItems());
        dto.setMissingItems(entity.getMissingItems());
        dto.setInterpretation(entity.getInterpretation());
        dto.setScoredAt(entity.getScoredAt());
        dto.setCompileTypeId(entity.getCompileTypeId());
        dto.setRegisteredUserUid(entity.getRegisteredUserUid());
        if (entity.getResponses() != null) {
            dto.setResponses(entity.getResponses().stream().map(r -> {
                ItemResponseDto responseDto = new ItemResponseDto();
                responseDto.setItemCode(r.getItemCode());
                responseDto.setDomain(r.getDomain());
                responseDto.setScore(r.getScore());
                responseDto.setDays(r.getDays());
                responseDto.setNote(r.getNote());
                return responseDto;
            }).collect(Collectors.toList()));
        }
        return dto;
    }
}
