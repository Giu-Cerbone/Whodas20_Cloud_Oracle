package it.whodas.document.service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import it.whodas.common.dto.AssessmentDto;
import it.whodas.common.dto.DocumentTemplateDto;
import it.whodas.common.dto.ItemResponseDto;
import it.whodas.common.dto.PatientDto;
import it.whodas.common.dto.TypeCompileQuestDto;
import it.whodas.document.service.client.AssessmentServiceClient;
import it.whodas.document.service.client.PatientServiceClient;
import it.whodas.document.service.client.QuestionnaireServiceClient;
import it.whodas.document.service.pdf.TemplateRequest;
import it.whodas.document.service.pdf.WhodasTemplatePdfService;

/**
 * Punto di ingresso della generazione dei modelli: arricchisce la richiesta con
 * i dati del paziente e del tipo di compilazione, poi delega la stampa del
 * PDF al servizio di generazione.
 */
@Service
public class DocumentTemplateService {

    private static final Logger log = LoggerFactory.getLogger(DocumentTemplateService.class);

    /** Unico modello supportato: la versione a 36 item del questionario WHODAS 2.0. */
    public static final String TEMPLATE_36 = "WHODAS2-36";

    private final WhodasTemplatePdfService pdfService;
    private final PatientServiceClient patientClient;
    private final AssessmentServiceClient assessmentClient;
    private final QuestionnaireServiceClient questionnaireClient;

    public DocumentTemplateService(WhodasTemplatePdfService pdfService,
                                   PatientServiceClient patientClient,
                                   AssessmentServiceClient assessmentClient,
                                   QuestionnaireServiceClient questionnaireClient) {
        this.pdfService = pdfService;
        this.patientClient = patientClient;
        this.assessmentClient = assessmentClient;
        this.questionnaireClient = questionnaireClient;
    }

    public List<DocumentTemplateDto> templates() {
        return List.of(new DocumentTemplateDto(TEMPLATE_36,
                "Versione a 36 item, autosomministrata",
                "Modello ufficiale WHODAS 2.0 a 36 item, 6 domini: 36 caselle di risposta "
                        + "(scala 0-4), campi per i dati del paziente, l'intervistatore e le note.",
                36, "it"));
    }

    public Resource generate(Long patientId, Long compileTypeId, String interviewer, LocalDate assessmentDate,
                             boolean fillAnswers, Long assessmentId) {
        PatientDto patient = patientId == null ? null
                : patientClient.findById(patientId).orElse(null);
        if (patientId != null && patient == null) {
            log.info("Paziente {} non trovato: il modello viene generato vuoto", patientId);
        }
        String compileTypeLabel = questionnaireClient.compileTypeLabel(compileTypeId);
        Map<String, Integer> answers = fillAnswers
                ? answersOf(patient, assessmentId)
                : Map.of();

        TemplateRequest request = new TemplateRequest(
                TEMPLATE_36, "36-ITEM", "it", compileTypeId, compileTypeLabel,
                patient == null ? null : patient.getId(),
                patient == null ? null : patient.getFirstName(),
                patient == null ? null : patient.getLastName(),
                patient == null ? null : patient.getFiscalCode(),
                patient == null ? null : patient.getBirthDate(),
                patient == null ? null : patient.getGender(),
                interviewer,
                assessmentDate == null ? LocalDate.now() : assessmentDate,
                null,
                answers);
        return pdfService.generate(request);
    }

    /** Risposte gia' registrate, da riportare sul modello. */
    private Map<String, Integer> answersOf(PatientDto patient, Long assessmentId) {
        Map<String, Integer> answers = new LinkedHashMap<>();
        if (patient == null || patient.getId() == null) {
            return answers;
        }
        var assessments = assessmentClient.findByPatientId(String.valueOf(patient.getId()));
        // Senza assessmentId si usa l'ultimo in elenco, con id si cerca quello indicato.
        AssessmentDto assessment = assessments.stream()
                .filter(candidate -> assessmentId == null || assessmentId.equals(candidate.getId()))
                .reduce((first, second) -> second)
                .orElse(null);
        if (assessment == null) {
            return answers;
        }
        List<ItemResponseDto> responses = assessment.getResponses();
        if (responses == null) {
            return answers;
        }
        for (ItemResponseDto response : responses) {
            if (response.getItemCode() != null && response.getScore() != null) {
                answers.put(response.getItemCode(), response.getScore());
            }
        }
        return answers;
    }

    /** Tipo di compilazione del modello, per il valore predefinito della tendina. */
    public Optional<TypeCompileQuestDto> compileType(Long id) {
        return questionnaireClient.findAllCompileTypes().stream()
                .filter(type -> id.equals(type.getId()))
                .findFirst();
    }
}