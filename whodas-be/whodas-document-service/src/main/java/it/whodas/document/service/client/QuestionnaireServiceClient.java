package it.whodas.document.service.client;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import it.whodas.common.dto.QuestionnaireItemDto;
import it.whodas.common.dto.TypeCompileQuestDto;

/**
 * Clienti del questionario: catalogo dei 36 item e tipi di compilazione del
 * questionario (whodas-assessment-service). Se il questionario non e'
 * raggiungibile si ricade sul catalogo di riserva embedded (WhodasItem), quindi
 * la generazione del modello resta disponibile.
 */
@Component
public class QuestionnaireServiceClient {

    private static final Logger log = LoggerFactory.getLogger(QuestionnaireServiceClient.class);

    private final RestClient client;

    public QuestionnaireServiceClient(@Qualifier("questionnaireRestClient") RestClient client) {
        this.client = client;
    }

    public List<QuestionnaireItemDto> findAllItems() {
        List<QuestionnaireItemDto> items = client.get()
                .uri("/api/v1/questionnaire/items")
                .retrieve()
                .body(new ParameterizedTypeReference<List<QuestionnaireItemDto>>() {});
        return items == null ? List.of() : items;
    }

    public List<TypeCompileQuestDto> findAllCompileTypes() {
        List<TypeCompileQuestDto> types = client.get()
                .uri("/api/v1/type-compile-quest")
                .retrieve()
                .body(new ParameterizedTypeReference<List<TypeCompileQuestDto>>() {});
        return types == null ? List.of() : types;
    }

    /**
     * Etichetta del tipo di compilazione indicato, per riportarla nel modello.
     * Un errore non blocca la generazione: l'etichetta resta vuota.
     */
    public String compileTypeLabel(Long id) {
        if (id == null) {
            return null;
        }
        try {
            TypeCompileQuestDto type = client.get()
                    .uri("/api/v1/type-compile-quest/{id}", id)
                    .retrieve()
                    .body(TypeCompileQuestDto.class);
            return type == null ? null : type.getDescType();
        } catch (RuntimeException ex) {
            log.debug("Tipo di compilazione {} non recuperato: {}", id, ex.toString());
            return null;
        }
    }
}
