package it.whodas.document.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import it.whodas.common.dto.AssessmentDto;
import it.whodas.common.dto.AssessmentResultDto;
import it.whodas.common.dto.DocumentImportResultDto;
import it.whodas.common.dto.DocumentImportSummaryDto;
import it.whodas.common.dto.ExtractedItemDto;
import it.whodas.common.dto.ImportedAssessmentDto;
import it.whodas.common.dto.ImportedPatientDto;
import it.whodas.common.dto.ItemResponseDto;
import it.whodas.common.dto.PatientDto;
import it.whodas.common.exception.WhodasException;
import it.whodas.document.config.DocumentServiceProperties;
import it.whodas.document.domain.DocumentImport;
import it.whodas.document.domain.DocumentImportField;
import it.whodas.document.domain.DocumentImportItem;
import it.whodas.document.extractor.DocumentExtractionService;
import it.whodas.document.extractor.DocumentFormat;
import it.whodas.document.extractor.RawContent;
import it.whodas.document.parser.ParsedDocument;
import it.whodas.document.parser.WhodasContentParser;
import it.whodas.document.repository.DocumentImportFieldRepository;
import it.whodas.document.repository.DocumentImportItemRepository;
import it.whodas.document.repository.DocumentImportRepository;
import it.whodas.document.service.client.AssessmentServiceClient;
import it.whodas.document.service.client.PatientServiceClient;

/**
 * Import di un documento WHODAS 2.0 compilato: estrazione del contenuto,
 * interpretazione dei valori e scrittura relazionale attraverso le API dei
 * servizi proprietari (patient service e assessment service).
 *
 * Il document service non scrive mai direttamente sulle tabelle degli altri
 * servizi: il paziente viene risolto o creato tramite il patient service e
 * l'assessment tramite l'assessment service, cosi' le validazioni e il calcolo
 * del punteggio restano nel servizio che ne e' responsabile. Sul database del
 * document service resta la traccia dell'import (file originale, valori
 * riconosciuti con confidenza e sorgente, avvisi).
 */
@Service
public class DocumentImportService {

    private static final Logger log = LoggerFactory.getLogger(DocumentImportService.class);

    /** Versione del questionario con cui vengono creati gli assessment. */
    private static final String VERSION = "36-ITEM";
    private static final String LANGUAGE = "it";

    /** Il testo grezzo viene conservato solo in parte per non gonfiare la tabella. */
    private static final int MAX_STORED_TEXT = 200_000;
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 200;

    /** Modalita' di compilazione (assessments.mode) per tipo di compilazione. */
    private static final List<String[]> COMPILE_TYPE_MODES = List.of(
            new String[]{"1", "SELF_ADMINISTERED"},
            new String[]{"2", "INTERVIEWER_ADMINISTERED"},
            new String[]{"3", "PROXY_REPORTED"});

    private final DocumentImportRepository importRepository;
    private final DocumentImportFieldRepository fieldRepository;
    private final DocumentImportItemRepository itemRepository;
    private final DocumentExtractionService extractionService;
    private final WhodasContentParser parser;
    private final PatientServiceClient patientClient;
    private final AssessmentServiceClient assessmentClient;
    private final DocumentServiceProperties properties;

    public DocumentImportService(DocumentImportRepository importRepository,
                                 DocumentImportFieldRepository fieldRepository,
                                 DocumentImportItemRepository itemRepository,
                                 DocumentExtractionService extractionService,
                                 WhodasContentParser parser,
                                 PatientServiceClient patientClient,
                                 AssessmentServiceClient assessmentClient,
                                 DocumentServiceProperties properties) {
        this.importRepository = importRepository;
        this.fieldRepository = fieldRepository;
        this.itemRepository = itemRepository;
        this.extractionService = extractionService;
        this.parser = parser;
        this.patientClient = patientClient;
        this.assessmentClient = assessmentClient;
        this.properties = properties;
    }

    /**
     * Parametri dell'import inviati dal front-end.
     */
    public record ImportCommand(Long patientId, Long compileTypeId, String interviewer,
                                boolean createPatientIfMissing, boolean saveAssessment, boolean submitAndScore) {
    }

    /** File originale conservato per un download successivo. */
    public record StoredFile(String fileName, String contentType, byte[] content) {
    }

    @Transactional
    public DocumentImportResultDto importDocument(MultipartFile file, ImportCommand command) {
        if (file == null || file.isEmpty()) {
            throw new WhodasException("Nessun file ricevuto", "EMPTY_FILE");
        }
        DocumentFormat format = DocumentFormat.detect(file.getOriginalFilename(), file.getContentType())
                .orElseThrow(() -> new WhodasException("Formato non supportato. Sono ammessi solo file "
                        + DocumentFormat.acceptedExtensions(), "UNSUPPORTED_DOCUMENT_FORMAT"));

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (java.io.IOException ex) {
            throw new WhodasException("File non leggibile: " + ex.getMessage(), "DOCUMENT_NOT_READABLE");
        }

        RawContent raw = extractionService.extract(bytes, format);
        ParsedDocument parsed = parser.parse(raw);

        ImportedPatientDto patient = resolvePatient(parsed, command, parsed.getWarnings());
        ImportedAssessmentDto assessment = command.saveAssessment() && patient != null
                ? importAssessment(parsed, command, patient, parsed.getWarnings())
                : null;

        String status = assessment != null ? "IMPORTED" : "ANALYZED_ONLY";
        DocumentImport stored = persist(file, format, bytes, raw, parsed, command, patient, assessment, status);

        log.info("Import {} di '{}': metodo {}, {} risposte su 36, paziente {}, assessment {}",
                stored.getId(), file.getOriginalFilename(), raw.getMethod(), parsed.recognizedAnswers(),
                patient == null ? "-" : patient.getId(), assessment == null ? "-" : assessment.getId());

        return toResult(stored, parsed, raw, patient, assessment, status, command.compileTypeId());
    }

    @Transactional(readOnly = true)
    public List<DocumentImportSummaryDto> summary(int limit, int offset) {
        int safeLimit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        int safeOffset = Math.max(offset, 0);
        return importRepository.findAllByOrderByIdDesc(PageRequest.of(safeOffset / safeLimit, safeLimit))
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentImportResultDto detail(Long id) {
        DocumentImport stored = requireImport(id);
        return toStoredResult(stored);
    }

    @Transactional(readOnly = true)
    public Optional<StoredFile> storedFile(Long id) {
        return importRepository.findById(id)
                .filter(stored -> stored.getContent() != null && stored.getContent().length > 0)
                .map(stored -> new StoredFile(stored.getFileName(), stored.getContentType(), stored.getContent()));
    }

    @Transactional
    public void delete(Long id) {
        DocumentImport stored = requireImport(id);
        importRepository.delete(stored);
    }

    // ------------------------------------------------------------------
    // risoluzione del paziente
    // ------------------------------------------------------------------

    private ImportedPatientDto resolvePatient(ParsedDocument parsed, ImportCommand command, List<String> warnings) {
        if (command.patientId() != null) {
            PatientDto patient = patientClient.findById(command.patientId())
                    .orElseThrow(() -> new WhodasException(
                            "Paziente " + command.patientId() + " non trovato", "PATIENT_NOT_FOUND",
                            HttpStatus.NOT_FOUND));
            return reference(patient, false);
        }

        String fiscalCode = parsed.value("patient.fiscalCode");
        if (fiscalCode != null && !fiscalCode.isBlank()) {
            Optional<PatientDto> existing = patientClient.findByFiscalCode(fiscalCode);
            if (existing.isPresent()) {
                return reference(existing.get(), false);
            }
        }

        String lastName = parsed.value("patient.lastName");
        String firstName = parsed.value("patient.firstName");
        String birthDate = parsed.value("patient.birthDate");

        if (lastName != null || firstName != null) {
            Optional<PatientDto> byName = findByName(lastName, firstName, birthDate);
            if (byName.isPresent()) {
                return reference(byName.get(), false);
            }
        }

        if (!command.createPatientIfMissing()) {
            warnings.add("Paziente non trovato e creazione non richiesta: nessun assessment viene creato.");
            return null;
        }
        if (lastName == null || lastName.isBlank() || firstName == null || firstName.isBlank()) {
            warnings.add("Nome e cognome non riconosciuti nel documento: impossible creare il paziente. "
                    + "Indicare il paziente selezionandolo dall'elenco.");
            return null;
        }

        PatientDto toCreate = new PatientDto();
        toCreate.setFirstName(firstName);
        toCreate.setLastName(lastName);
        toCreate.setFiscalCode(fiscalCode);
        toCreate.setBirthDate(birthDate);
        toCreate.setGender(parsed.value("patient.gender"));
        toCreate.setBirthPlace(parsed.value("patient.birthPlace"));
        toCreate.setAddress(parsed.value("patient.address"));
        toCreate.setPhone(parsed.value("patient.phone"));
        toCreate.setEmail(parsed.value("patient.email"));
        toCreate.setHealthCondition(parsed.value("patient.healthCondition"));
        try {
            return reference(patientClient.create(toCreate), true);
        } catch (WhodasException ex) {
            if ("PATIENT_FISCAL_CODE_EXISTS".equals(ex.getCode())) {
                warnings.add("Un paziente con lo stesso codice fiscale e' gia' presente: "
                        + "il documento viene valutato senza creare un nuovo paziente.");
                return patientClient.findByFiscalCode(fiscalCode).map(patient -> reference(patient, false))
                        .orElse(null);
            }
            throw ex;
        }
    }

    /**
     * Ricerca fra i pazienti con lo stesso cognome: se i dati del documento sono
     * sufficienti si abbina il paziente esistente invece di creare un duplicato.
     */
    private Optional<PatientDto> findByName(String lastName, String firstName, String birthDate) {
        if (lastName == null || lastName.isBlank()) {
            return Optional.empty();
        }
        List<PatientDto> candidates = patientClient.searchByLastName(lastName);
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        if (firstName == null || firstName.isBlank()) {
            return Optional.empty();
        }
        return candidates.stream()
                .filter(candidate -> firstName.equalsIgnoreCase(candidate.getFirstName()))
                .filter(candidate -> birthDate == null || birthDate.equals(candidate.getBirthDate()))
                .findFirst();
    }

    private ImportedPatientDto reference(PatientDto patient, boolean created) {
        return new ImportedPatientDto(patient.getId(), patient.getFirstName(), patient.getLastName(),
                patient.getFiscalCode(), created);
    }

    // ------------------------------------------------------------------
    // creazione dell'assessment
    // ------------------------------------------------------------------

    private ImportedAssessmentDto importAssessment(ParsedDocument parsed, ImportCommand command,
                                                    ImportedPatientDto patient, List<String> warnings) {
        List<ItemResponseDto> responses = new ArrayList<>();
        for (ExtractedItemDto item : parsed.getItems()) {
            if (item.getScore() == null) {
                continue;
            }
            ItemResponseDto response = new ItemResponseDto();
            response.setItemCode(item.getItemCode());
            response.setDomain(item.getDomain());
            response.setScore(item.getScore());
            response.setDays(item.getDays());
            response.setNote(item.getNote() != null ? item.getNote()
                    : (item.getRawValue() == null ? null : "Valore letto dal documento: " + item.getRawValue()));
            responses.add(response);
        }
        if (responses.isEmpty()) {
            warnings.add("Nessuna risposta riconosciuta: l'assessment non viene creato.");
            return null;
        }

        AssessmentDto assessment = new AssessmentDto();
        assessment.setPatientId(String.valueOf(patient.getId()));
        assessment.setVersion(VERSION);
        assessment.setMode(modeOf(command.compileTypeId()));
        assessment.setLanguage(LANGUAGE);
        assessment.setCompileTypeId(command.compileTypeId());
        String interviewer = command.interviewer() != null && !command.interviewer().isBlank()
                ? command.interviewer()
                : parsed.value("assessment.interviewer");
        assessment.setInterviewer(interviewer);
        assessment.setNotes("Importato dal documento " + VERSION + " ("
                + (parsed.value("assessment.date") == null ? "" : parsed.value("assessment.date"))
                + ") dal document service.");
        assessment.setResponses(responses);

        AssessmentDto created = assessmentClient.create(assessment);
        ImportedAssessmentDto result = new ImportedAssessmentDto(created.getId(), created.getStatus(),
                null, null, responses.size(), true);

        if (command.submitAndScore() && responses.size() >= properties.getImportDefaults().getMinAnswersToScore()) {
            try {
                AssessmentResultDto scored = assessmentClient.submitAndScore(created.getId());
                if (scored != null) {
                    result.setStatus(scored.getStatus());
                    result.setOverallPercentDisability(scored.getOverallPercentDisability());
                    result.setInterpretation(scored.getInterpretation());
                }
            } catch (RuntimeException ex) {
                warnings.add("Il punteggio non e' stato calcolato: " + ex.getMessage());
            }
        } else if (command.submitAndScore()) {
            warnings.add("Punteggio non calcolato: risposte riconosciute insufficienti (minimo "
                    + properties.getImportDefaults().getMinAnswersToScore() + ").");
        }
        return result;
    }

    private String modeOf(Long compileTypeId) {
        if (compileTypeId == null) {
            return "DOCUMENT_IMPORT";
        }
        for (String[] mapping : COMPILE_TYPE_MODES) {
            if (mapping[0].equals(String.valueOf(compileTypeId))) {
                return mapping[1];
            }
        }
        return "DOCUMENT_IMPORT";
    }

    // ------------------------------------------------------------------
    // persistenza della traccia
    // ------------------------------------------------------------------

    private DocumentImport persist(MultipartFile file, DocumentFormat format, byte[] bytes, RawContent raw,
                                   ParsedDocument parsed, ImportCommand command, ImportedPatientDto patient,
                                   ImportedAssessmentDto assessment, String status) {
        LocalDateTime now = LocalDateTime.now();
        DocumentImport stored = new DocumentImport();
        stored.setFileName(file.getOriginalFilename());
        stored.setFileType(format.label());
        stored.setContentType(file.getContentType());
        stored.setFileSizeBytes((long) bytes.length);
        stored.setFileChecksum(sha256(bytes));
        stored.setExtractionMethod(raw.getMethod().name());
        stored.setStatus(status);
        stored.setCompileTypeId(command.compileTypeId());
        stored.setPatientId(patient == null ? null : String.valueOf(patient.getId()));
        stored.setPatientName(patient == null ? null : patient.getFullName());
        stored.setAssessmentId(assessment == null ? null : assessment.getId());
        stored.setAnswersImported(parsed.recognizedAnswers());
        stored.setFieldsExtracted(parsed.getFields().size());
        stored.setExtractedTextLength(raw.getText() == null ? 0 : raw.getText().length());
        stored.setRawText(truncate(raw.getText()));
        stored.setContent(bytes);
        stored.setWarnings(String.join(" | ", parsed.getWarnings()));
        stored.setCreatedAt(now);
        stored.setUpdatedAt(now);
        DocumentImport saved = importRepository.save(stored);

        for (var field : parsed.getFields()) {
            DocumentImportField entity = new DocumentImportField();
            entity.setImportId(saved.getId());
            entity.setFieldKey(field.getKey());
            entity.setFieldLabel(field.getLabel());
            entity.setRawValue(truncate(field.getRawValue(), 2000));
            entity.setNormalizedValue(truncate(field.getNormalizedValue(), 2000));
            entity.setConfidence(field.getConfidence());
            entity.setSource(field.getSource());
            entity.setCreatedAt(now);
            fieldRepository.save(entity);
        }

        for (ExtractedItemDto item : parsed.getItems()) {
            DocumentImportItem entity = new DocumentImportItem();
            entity.setImportId(saved.getId());
            entity.setItemCode(item.getItemCode());
            entity.setDomain(item.getDomain());
            entity.setDomainLabelIt(item.getDomainLabelIt());
            entity.setQuestionTextIt(truncate(item.getQuestionTextIt(), 500));
            entity.setRawValue(truncate(item.getRawValue(), 200));
            entity.setScore(item.getScore());
            entity.setConfidence(item.getConfidence());
            entity.setSource(item.getSource());
            entity.setNote(truncate(item.getNote(), 2000));
            entity.setDays(item.getDays());
            entity.setCreatedAt(now);
            itemRepository.save(entity);
        }
        return saved;
    }

    private DocumentImportResultDto toResult(DocumentImport stored, ParsedDocument parsed, RawContent raw,
                                             ImportedPatientDto patient, ImportedAssessmentDto assessment,
                                             String status, Long compileTypeId) {
        DocumentImportResultDto result = new DocumentImportResultDto();
        result.setImportId(stored.getId());
        result.setFileName(stored.getFileName());
        result.setFileType(stored.getFileType());
        result.setFileSizeBytes(stored.getFileSizeBytes());
        result.setExtractionMethod(stored.getExtractionMethod());
        result.setStatus(status);
        result.setCompileTypeId(compileTypeId);
        result.setPatient(patient);
        result.setAssessment(assessment);
        result.setFields(parsed.getFields());
        result.setItems(parsed.getItems());
        result.setWarnings(parsed.getWarnings());
        result.setExtractedTextLength(stored.getExtractedTextLength());
        result.setCreatedAt(stored.getCreatedAt());
        result.setDownloadUrl("/api/v1/documents/imports/" + stored.getId() + "/file");
        return result;
    }

    private DocumentImportResultDto toStoredResult(DocumentImport stored) {
        DocumentImportResultDto result = new DocumentImportResultDto();
        result.setImportId(stored.getId());
        result.setFileName(stored.getFileName());
        result.setFileType(stored.getFileType());
        result.setFileSizeBytes(stored.getFileSizeBytes());
        result.setExtractionMethod(stored.getExtractionMethod());
        result.setStatus(stored.getStatus());
        result.setCompileTypeId(stored.getCompileTypeId());
        result.setExtractedTextLength(stored.getExtractedTextLength());
        result.setCreatedAt(stored.getCreatedAt());
        result.setDownloadUrl("/api/v1/documents/imports/" + stored.getId() + "/file");

        if (stored.getPatientId() != null) {
            ImportedPatientDto patient = new ImportedPatientDto();
            patient.setId(Long.valueOf(stored.getPatientId()));
            patient.setFirstName(null);
            patient.setLastName(stored.getPatientName());
            result.setPatient(patient);
        }
        if (stored.getAssessmentId() != null) {
            ImportedAssessmentDto assessment = new ImportedAssessmentDto();
            assessment.setId(stored.getAssessmentId());
            result.setAssessment(assessment);
        }

        result.setFields(fieldRepository.findByImportIdOrderByIdAsc(stored.getId()).stream()
                .map(field -> new it.whodas.common.dto.ExtractedFieldDto(field.getFieldKey(), field.getFieldLabel(),
                        field.getRawValue(), field.getNormalizedValue(),
                        field.getConfidence() == null ? 0 : field.getConfidence(), field.getSource()))
                .toList());
        result.setItems(itemRepository.findByImportIdOrderByIdAsc(stored.getId()).stream()
                .map(DocumentImportService::toItem)
                .toList());
        result.setWarnings(splitWarnings(stored.getWarnings()));
        return result;
    }

    private static ExtractedItemDto toItem(DocumentImportItem entity) {
        ExtractedItemDto item = new ExtractedItemDto();
        item.setItemCode(entity.getItemCode());
        item.setDomain(entity.getDomain());
        item.setDomainLabelIt(entity.getDomainLabelIt());
        item.setQuestionTextIt(entity.getQuestionTextIt());
        item.setRawValue(entity.getRawValue());
        item.setScore(entity.getScore());
        item.setConfidence(entity.getConfidence() == null ? 0 : entity.getConfidence());
        item.setSource(entity.getSource());
        item.setNote(entity.getNote());
        item.setDays(entity.getDays());
        return item;
    }

    private DocumentImportSummaryDto toSummary(DocumentImport stored) {
        DocumentImportSummaryDto summary = new DocumentImportSummaryDto();
        summary.setId(stored.getId());
        summary.setFileName(stored.getFileName());
        summary.setFileType(stored.getFileType());
        summary.setExtractionMethod(stored.getExtractionMethod());
        summary.setStatus(stored.getStatus());
        summary.setPatientId(stored.getPatientId());
        summary.setPatientName(stored.getPatientName());
        summary.setAssessmentId(stored.getAssessmentId());
        summary.setAnswersImported(stored.getAnswersImported());
        summary.setFileSizeBytes(stored.getFileSizeBytes());
        summary.setCreatedAt(stored.getCreatedAt());
        return summary;
    }

    private DocumentImport requireImport(Long id) {
        return importRepository.findById(id).orElseThrow(() -> new WhodasException(
                "Import non trovato: " + id, "DOCUMENT_IMPORT_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private static List<String> splitWarnings(String warnings) {
        if (warnings == null || warnings.isBlank()) {
            return List.of();
        }
        return List.of(warnings.split("\\|\\s*"));
    }

    private static String truncate(String value) {
        return truncate(value, MAX_STORED_TEXT);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
    }
}