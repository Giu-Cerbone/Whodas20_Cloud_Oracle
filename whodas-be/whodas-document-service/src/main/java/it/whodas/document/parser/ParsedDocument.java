package it.whodas.document.parser;

import java.util.ArrayList;
import java.util.List;

import it.whodas.common.dto.ExtractedFieldDto;
import it.whodas.common.dto.ExtractedItemDto;

/**
 * Esito dell'interpretazione di un documento: i valori riconosciuti e gli
 * avvisi su quello che non e' stato possibile leggere.
 */
public class ParsedDocument {

    private final List<ExtractedFieldDto> fields = new ArrayList<>();
    private final List<ExtractedItemDto> items = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public List<ExtractedFieldDto> getFields() {
        return fields;
    }

    public List<ExtractedItemDto> getItems() {
        return items;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public int recognizedAnswers() {
        return (int) items.stream().filter(item -> item.getScore() != null).count();
    }

    public ExtractedFieldDto field(String key) {
        return fields.stream()
                .filter(candidate -> candidate.getKey().equals(key))
                .findFirst()
                .orElse(null);
    }

    public String value(String key) {
        ExtractedFieldDto field = field(key);
        return field == null ? null : field.getNormalizedValue();
    }
}