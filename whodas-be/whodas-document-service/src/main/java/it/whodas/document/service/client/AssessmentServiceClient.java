package it.whodas.document.service.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import it.whodas.common.dto.AssessmentDto;
import it.whodas.common.dto.AssessmentResultDto;
import it.whodas.common.exception.WhodasException;

/**
 * Clienti dell'assessment service: creazione dell'assessment a partire dalle
 * risposte riconosciute e calcolo del punteggio WHODAS 2.0.
 */
@Component
public class AssessmentServiceClient {

    private final RestClient client;

    public AssessmentServiceClient(@Qualifier("assessmentRestClient") RestClient client) {
        this.client = client;
    }

    public AssessmentDto findById(Long id) {
        try {
            AssessmentDto assessment = client.get()
                    .uri("/api/v1/assessments/{id}", id)
                    .retrieve()
                    .body(AssessmentDto.class);
            if (assessment == null) {
                throw new WhodasException("Assessment not found: " + id, "ASSESSMENT_NOT_FOUND");
            }
            return assessment;
        } catch (WhodasException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw unavailable(ex);
        }
    }

    public AssessmentDto create(AssessmentDto assessment) {
        try {
            AssessmentDto created = client.post()
                    .uri("/api/v1/assessments")
                    .body(assessment)
                    .retrieve()
                    .body(AssessmentDto.class);
            if (created == null || created.getId() == null) {
                throw new WhodasException("Assessment service did not return the created assessment",
                        "ASSESSMENT_NOT_CREATED");
            }
            return created;
        } catch (WhodasException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw unavailable(ex);
        }
    }

    public AssessmentResultDto submitAndScore(Long id) {
        try {
            return client.put()
                    .uri("/api/v1/assessments/{id}/submit", id)
                    .body(java.util.Map.of())
                    .retrieve()
                    .body(AssessmentResultDto.class);
        } catch (RuntimeException ex) {
            throw unavailable(ex);
        }
    }

    public List<AssessmentDto> findByPatientId(String patientId) {
        try {
            List<AssessmentDto> assessments = client.get()
                    .uri("/api/v1/assessments/patient/{patientId}", patientId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<AssessmentDto>>() {});
            return assessments == null ? List.of() : assessments;
        } catch (RuntimeException ex) {
            throw unavailable(ex);
        }
    }

    private WhodasException unavailable(RuntimeException ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return new WhodasException("assessment service not reachable: " + message, "UPSTREAM_SERVICE_UNAVAILABLE",
                HttpStatus.SERVICE_UNAVAILABLE);
    }
}
