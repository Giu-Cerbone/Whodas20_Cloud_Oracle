package it.whodas.document.extractor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Contenuto grezzo di un documento, normalizzato in una forma che il parser
 * puo' leggere indipendentemente dal formato di origine.
 */
public class RawContent {

    private ExtractionMethod method = ExtractionMethod.NONE;
    private String text = "";
    private final Map<String, String> formValues = new LinkedHashMap<>();
    private final List<String[]> pairs = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private int pageCount;

    public RawContent withMethod(ExtractionMethod method) {
        this.method = method;
        return this;
    }

    public RawContent withText(String text) {
        this.text = text == null ? "" : text;
        return this;
    }

    /**
     * Valori dei campi di un modulo. La chiave e' il nome completo del campo in
     * minuscolo: i nomi generati dal document service sono
     * patient.firstName, item.D1S1.score, assessment.interviewer.
     */
    public RawContent withFormValue(String fieldName, String value) {
        if (fieldName != null && value != null && !value.isBlank()) {
            formValues.put(fieldName.trim().toLowerCase(java.util.Locale.ROOT), value.trim());
        }
        return this;
    }

    public RawContent withPair(String label, String value) {
        if (label != null && value != null && !label.isBlank() && !value.isBlank()) {
            pairs.add(new String[]{label.trim(), value.trim()});
        }
        return this;
    }

    public RawContent withWarning(String warning) {
        if (warning != null && !warning.isBlank()) {
            warnings.add(warning);
        }
        return this;
    }

    public RawContent withPageCount(int pageCount) {
        this.pageCount = pageCount;
        return this;
    }

    public ExtractionMethod getMethod() {
        return method;
    }

    public void setMethod(ExtractionMethod method) {
        this.method = method;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Map<String, String> getFormValues() {
        return formValues;
    }

    public List<String[]> getPairs() {
        return pairs;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public boolean hasFormValues() {
        return !formValues.isEmpty();
    }

    public boolean hasText() {
        return !text.isBlank();
    }
}