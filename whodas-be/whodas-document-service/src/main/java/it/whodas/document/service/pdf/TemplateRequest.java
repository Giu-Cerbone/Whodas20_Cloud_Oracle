package it.whodas.document.service.pdf;

import java.time.LocalDate;
import java.util.Map;

/**
 * Dati con cui il modello WHODAS 2.0 viene precompilato: quando non arrivano
 * dal front-end il modello e' comunque utilizzabile, vuoto e compilabile.
 *
 * @param templateCode     codice del modello (WHODAS2-36)
 * @param version          versione del questionario (36-ITEM)
 * @param language         lingua del modello (it)
 * @param compileTypeId    id di tab_type_compile_quest, null se non scelto
 * @param compileTypeLabel descrizione del tipo di compilazione
 * @param patientId        id del paziente da usare come riferimento
 * @param firstName        nome del paziente
 * @param lastName         cognome del paziente
 * @param fiscalCode       codice fiscale del paziente
 * @param birthDate        data di nascita del paziente
 * @param gender           sesso del paziente
 * @param interviewer      intervistatore
 * @param assessmentDate   data di compilazione
 * @param notes            note riportate in testata
 * @param answers          punteggi gia' presenti (itemCode -> 0..4)
 */
public record TemplateRequest(
        String templateCode,
        String version,
        String language,
        Long compileTypeId,
        String compileTypeLabel,
        Long patientId,
        String firstName,
        String lastName,
        String fiscalCode,
        String birthDate,
        String gender,
        String interviewer,
        LocalDate assessmentDate,
        String notes,
        Map<String, Integer> answers) {

    public TemplateRequest {
        answers = answers == null ? Map.of() : Map.copyOf(answers);
    }
}