package it.whodas.document.service.client;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import it.whodas.common.dto.PatientDto;
import it.whodas.common.exception.WhodasException;

/**
 * Clienti del patient service. Gli errori 404 sono tradotti in Optional vuoto
 * (il paziente non esiste ancora), gli altri in WhodasException.
 */
@Component
public class PatientServiceClient {

    private final RestClient client;

    public PatientServiceClient(@Qualifier("patientRestClient") RestClient client) {
        this.client = client;
    }

    public Optional<PatientDto> findByFiscalCode(String fiscalCode) {
        try {
            return Optional.ofNullable(client.get()
                    .uri("/api/v1/patients/fiscal-code/{fiscalCode}", fiscalCode)
                    .retrieve()
                    .body(PatientDto.class));
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (RuntimeException ex) {
            throw unavailable("patient service", ex);
        }
    }

    public Optional<PatientDto> findById(Long id) {
        try {
            return Optional.ofNullable(client.get()
                    .uri("/api/v1/patients/{id}", id)
                    .retrieve()
                    .body(PatientDto.class));
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (RuntimeException ex) {
            throw unavailable("patient service", ex);
        }
    }

    public List<PatientDto> searchByLastName(String lastName) {
        try {
            List<PatientDto> patients = client.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/v1/patients/search/last-name")
                            .queryParam("lastName", lastName)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<PatientDto>>() {});
            return patients == null ? List.of() : patients;
        } catch (RuntimeException ex) {
            throw unavailable("patient service", ex);
        }
    }

    public PatientDto create(PatientDto patient) {
        try {
            PatientDto created = client.post()
                    .uri("/api/v1/patients")
                    .body(patient)
                    .retrieve()
                    .body(PatientDto.class);
            if (created == null) {
                throw new WhodasException("Patient service did not return the created patient", "PATIENT_NOT_CREATED");
            }
            return created;
        } catch (HttpClientErrorException.Conflict ex) {
            // Un paziente con lo stesso codice fiscale puo' essere stato creato
            // nel frattempo: il chiamante riprova con la ricerca per codice fiscale.
            throw new WhodasException("A patient with the same fiscal code already exists", "PATIENT_FISCAL_CODE_EXISTS");
        } catch (WhodasException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw unavailable("patient service", ex);
        }
    }

    private WhodasException unavailable(String service, RuntimeException ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return new WhodasException(service + " not reachable: " + message, "UPSTREAM_SERVICE_UNAVAILABLE",
                HttpStatus.SERVICE_UNAVAILABLE);
    }
}
