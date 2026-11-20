package it.whodas.document.parser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import it.whodas.common.dto.ExtractedFieldDto;
import it.whodas.common.dto.ExtractedItemDto;
import it.whodas.document.extractor.RawContent;

/**
 * Interpreta il contenuto grezzo di un documento WHODAS 2.0 e ne ricava i
 * dati anagrafici del paziente e le risposte ai 36 item.
 *
 * Le fonti di evidenza, dalla piu' affidabile alla meno affidabile, sono:
 *
 * 1. campi AcroForm del modello generato dal document service
 *    (patient.*, item.D1S1.score, assessment.*): valori esatti, confidenza 1.0;
 * 2. coppie etichetta/valore di tabelle Word o Excel: confidenza 0.8;
 * 3. testo libero del documento, agganciato all'item dal codice (D1.1, D1S1)
 *    o dal testo della domanda: confidenza da 0.4 a 0.75 secondo la prova
 *    trovata (marcatura, punteggio numerico, sola etichetta di risposta).
 *
 * Ogni valore riporta confidenza e sorgente, cosi' chi usa il risultato
 * distingue un dato certo da una lettura da verificare.
 */
@Component
public class WhodasContentParser {

    /** Fonte del valore riconosciuto, esposta nella risposta all'operatore. */
    public enum Source {
        ACROFORM, TABLE, TEXT
    }

    private static final Pattern ITEM_FIELD = Pattern.compile("^item\\.([a-z0-9]+)\\.(score|note|days)$");

    /**
     * Numerazioni degli item nei documenti: D1.1, D1-1, D1S1, D1 S 1. Il
     * secondo gruppo e' l'indice dell'item all'interno del dominio.
     */
    private static final Pattern ITEM_CODE_TOKEN =
            Pattern.compile("\\bD\\s*[1-6]\\s*([.\\-]?\\s*S?\\s*\\.?\\s*)([1-9]|[12]\\d|3[0-6])\\b",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern ITEM_ORDER_TOKEN = Pattern.compile("\\b(?:item|item\\s*n\\.?|n\\.?)\\s*([1-9]|[12]\\d|3[0-6])\\b",
            Pattern.CASE_INSENSITIVE);

    private Pattern labelPattern;

    public ParsedDocument parse(RawContent raw) {
        ParsedDocument parsed = new ParsedDocument();
        buildItemList(parsed);
        readFormValues(raw, parsed);
        readPairs(raw, parsed);
        readText(raw, parsed);
        parsed.getWarnings().addAll(0, raw.getWarnings());
        buildWarnings(parsed);
        return parsed;
    }

    // ------------------------------------------------------------------
    // elenco dei 36 item
    // ------------------------------------------------------------------

    private void buildItemList(ParsedDocument parsed) {
        for (WhodasItem item : WhodasItem.all()) {
            ExtractedItemDto dto = new ExtractedItemDto();
            dto.setItemCode(item.code());
            dto.setDomain(item.domain().name());
            dto.setDomainLabelIt(item.domain().getItalianLabel());
            dto.setQuestionTextIt(item.shortText());
            parsed.getItems().add(dto);
        }
    }

    private ExtractedItemDto itemOf(ParsedDocument parsed, WhodasItem item) {
        return parsed.getItems().stream()
                .filter(candidate -> item.code().equals(candidate.getItemCode()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Registra una risposta riconosciuta. Se lo stesso item e' stato letto da
     * piu' fonti si conserva la lettura con confidenza maggiore.
     */
    private void applyAnswer(ParsedDocument parsed, WhodasItem item, Integer score, String rawValue,
                             String note, Integer days, Source source, double confidence) {
        ExtractedItemDto target = itemOf(parsed, item);
        if (target == null || confidence <= 0) {
            return;
        }
        if (confidence <= target.getConfidence() && score != null && target.getScore() != null) {
            return;
        }
        if (score != null && confidence >= target.getConfidence()) {
            target.setScore(score);
            target.setRawValue(rawValue);
            target.setConfidence(confidence);
            target.setSource(source.name());
        } else if (score == null && target.getRawValue() == null && rawValue != null && confidence >= 0.8) {
            target.setRawValue(rawValue);
            target.setConfidence(confidence);
            target.setSource(source.name());
        }
        if (note != null && target.getNote() == null) {
            target.setNote(note);
        }
        if (days != null && target.getDays() == null) {
            target.setDays(days);
        }
    }

    // ------------------------------------------------------------------
    // 1. campi AcroForm
    // ------------------------------------------------------------------

    private void readFormValues(RawContent raw, ParsedDocument parsed) {
        for (var entry : raw.getFormValues().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            Matcher itemField = ITEM_FIELD.matcher(key);
            if (itemField.matches()) {
                WhodasItem item = WhodasItem.resolve(itemField.group(1));
                if (item == null) {
                    parsed.getWarnings().add("Campo modulo sconosciuto, ignorato: " + key);
                    continue;
                }
                applyFormItemField(parsed, item, itemField.group(2), value);
                continue;
            }

            DocumentFields.Kind kind = kindOfFormKey(key);
            if (kind == null) {
                continue;
            }
            String normalized = DocumentFields.normalizeValue(kind, value);
            if (normalized == null || normalized.isBlank()) {
                continue;
            }
            parsed.getFields().add(new ExtractedFieldDto(
                    fieldKey(kind), kind.label(), value, normalized, 1.0, Source.ACROFORM.name()));
        }
    }

    private void applyFormItemField(ParsedDocument parsed, WhodasItem item, String suffix, String value) {
        switch (suffix) {
            case "score" -> applyAnswer(parsed, item, WhodasScale.scoreOf(value), value, null, null,
                    Source.ACROFORM, 1.0);
            case "days" -> applyAnswer(parsed, item, null, null, null, asDays(value), Source.ACROFORM, 1.0);
            case "note" -> applyAnswer(parsed, item, null, null, value, null, Source.ACROFORM, 1.0);
            default -> parsed.getWarnings().add("Campo modulo non gestito: item." + item.code() + "." + suffix);
        }
    }

    private Integer asDays(String value) {
        try {
            int days = Integer.parseInt(value.strip());
            return days >= 0 && days <= 365 ? days : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private DocumentFields.Kind kindOfFormKey(String key) {
        return switch (key) {
            case "patient.lastname" -> DocumentFields.Kind.LAST_NAME;
            case "patient.firstname" -> DocumentFields.Kind.FIRST_NAME;
            case "patient.fiscalcode" -> DocumentFields.Kind.FISCAL_CODE;
            case "patient.birthdate" -> DocumentFields.Kind.BIRTH_DATE;
            case "patient.age" -> DocumentFields.Kind.AGE;
            case "patient.gender" -> DocumentFields.Kind.GENDER;
            case "patient.birthplace" -> DocumentFields.Kind.BIRTH_PLACE;
            case "patient.address" -> DocumentFields.Kind.ADDRESS;
            case "patient.phone" -> DocumentFields.Kind.PHONE;
            case "patient.email" -> DocumentFields.Kind.EMAIL;
            case "patient.healthcondition" -> DocumentFields.Kind.HEALTH_CONDITION;
            case "assessment.date" -> DocumentFields.Kind.ASSESSMENT_DATE;
            case "assessment.interviewer" -> DocumentFields.Kind.INTERVIEWER;
            case "assessment.compiletype" -> DocumentFields.Kind.COMPILE_TYPE;
            case "assessment.notes" -> DocumentFields.Kind.NOTES;
            default -> null;
        };
    }

    // ------------------------------------------------------------------
    // 2. coppie etichetta/valore di tabelle Word ed Excel
    // ------------------------------------------------------------------

    private void readPairs(RawContent raw, ParsedDocument parsed) {
        for (String[] pair : raw.getPairs()) {
            WhodasItem item = resolveItem(pair[0]).orElse(null);
            if (item != null) {
                Integer score = WhodasScale.scoreOf(pair[1]);
                double confidence = score != null ? 0.85 : 0.6;
                applyAnswer(parsed, item, score, score != null ? pair[1] : null, null, null,
                        Source.TABLE, confidence);
                continue;
            }
            DocumentFields.Kind kind = DocumentFields.resolveLabel(pair[0]);
            if (kind == null || parsed.field(fieldKey(kind)) != null) {
                continue;
            }
            String normalized = DocumentFields.normalizeValue(kind, pair[1]);
            if (normalized == null || normalized.isBlank()) {
                continue;
            }
            parsed.getFields().add(new ExtractedFieldDto(
                    fieldKey(kind), kind.label(), pair[1], normalized, 0.8, Source.TABLE.name()));
        }
    }

    // ------------------------------------------------------------------
    // 3. testo libero
    // ------------------------------------------------------------------

    private void readText(RawContent raw, ParsedDocument parsed) {
        String text = raw.getText();
        if (TextNormalizer.isBlank(text)) {
            return;
        }
        List<String> lines = List.of(text.split("\\R"));
        for (int index = 0; index < lines.size(); index++) {
            readItemFromLine(lines, index, parsed);
            readFieldsFromLine(lines.get(index), parsed);
        }
    }

    /**
     * Una riga puo' contenere solo la domanda e la scala di risposta, con la
     * risposta scritta nella riga seguente: per questo si valuta la riga
     * insieme a quella successiva.
     */
    private void readItemFromLine(List<String> lines, int index, ParsedDocument parsed) {
        String line = lines.get(index);
        if (TextNormalizer.isBlank(line)) {
            return;
        }
        WhodasItem item = itemByCode(line).orElseGet(() -> itemByQuestionText(line).orElse(null));
        if (item == null) {
            return;
        }
        StringBuilder fragment = new StringBuilder(line);
        if (index + 1 < lines.size()) {
            String next = lines.get(index + 1);
            if (itemByCode(next).isEmpty() && itemByQuestionText(next).isEmpty()) {
                fragment.append(' ').append(next);
            }
        }
        String cleaned = cleanFragment(fragment.toString());
        AnswerDetector.Answer answer = AnswerDetector.detect(cleaned);
        if (answer.score() == null && answer.days() == null) {
            return;
        }
        applyAnswer(parsed, item, answer.score(), answer.rawValue(), null, answer.days(),
                Source.TEXT, answer.confidence());
    }

    private void readFieldsFromLine(String line, ParsedDocument parsed) {
        // Le righe che riportano un item contengono solo la domanda e la scala
        // di risposta: le etichette che vi compaiono ("condizioni di salute",
        // "data") appartengono al testo della domanda, non alla testata.
        if (itemByCode(line).isPresent() || itemByQuestionText(line).isPresent()) {
            return;
        }
        List<LabelHit> hits = labelHits(line);
        for (int index = 0; index < hits.size(); index++) {
            LabelHit hit = hits.get(index);
            DocumentFields.Kind kind = hit.kind();
            if (kind == null || parsed.field(fieldKey(kind)) != null) {
                continue;
            }
            int valueStart = hit.valueStart();
            int valueEnd = index + 1 < hits.size() ? hits.get(index + 1).labelStart() : line.length();
            if (valueEnd <= valueStart) {
                continue;
            }
            String rawValue = line.substring(valueStart, valueEnd).strip();
            if (isBlankFill(rawValue)) {
                continue;
            }
            String normalized = DocumentFields.normalizeValue(kind, rawValue);
            if (normalized == null || normalized.isBlank()) {
                continue;
            }
            parsed.getFields().add(new ExtractedFieldDto(
                    fieldKey(kind), kind.label(), rawValue, normalized, 0.7, Source.TEXT.name()));
        }
    }

    /** Una riga di solo trattini o caselle vuote non e' un valore. */
    private boolean isBlankFill(String value) {
        return TextNormalizer.normalize(value).isBlank();
    }

    /**
     * Ripulisce il frammento prima del riconoscimento della risposta: via le
     * numerazioni degli item (il loro numero non e' un punteggio), i separatori
     * di tabella e di colonna, e la domanda in modo che l'etichetta cercata non
     * venga letta dentro il testo della domanda.
     */
    private String cleanFragment(String fragment) {
        String cleaned = ITEM_CODE_TOKEN.matcher(fragment).replaceAll(" ");
        cleaned = cleaned.replace('|', ' ').replace(';', ' ').replace('\t', ' ');
        for (WhodasItem item : WhodasItem.all()) {
            cleaned = cleaned.replace(item.officialText(), " ");
        }
        return cleaned;
    }

    private record LabelHit(DocumentFields.Kind kind, int labelStart, int valueStart) {
    }

    /**
     * Individua le etichette presenti in una riga anche quando sono piu' di una
     * ("Nome: Mario Rossi Eta: 45 Sesso: M"): ogni etichetta delimita il valore
     * della successiva.
     */
    private List<LabelHit> labelHits(String line) {
        List<LabelHit> hits = new ArrayList<>();
        Matcher matcher = labelPattern().matcher(line);
        while (matcher.find()) {
            DocumentFields.Kind kind = DocumentFields.resolveLabel(matcher.group(1));
            if (kind == null) {
                continue;
            }
            int valueStart = matcher.end();
            while (valueStart < line.length() && isSeparator(line.charAt(valueStart))) {
                valueStart++;
            }
            hits.add(new LabelHit(kind, matcher.start(), valueStart));
        }
        hits.sort(Comparator.comparingInt(LabelHit::labelStart));
        return hits;
    }

    private boolean isSeparator(char c) {
        return c == ':' || c == '=' || c == ' ' || c == '_' || c == '-' || c == '\t' || c == '.';
    }

    private Pattern labelPattern() {
        if (labelPattern == null) {
            StringBuilder regex = new StringBuilder();
            for (DocumentFields.Kind kind : DocumentFields.Kind.values()) {
                for (String label : DocumentFields.labelsOf(kind)) {
                    if (regex.length() > 0) {
                        regex.append('|');
                    }
                    regex.append(Pattern.quote(label));
                }
            }
            labelPattern = Pattern.compile("(?i)(" + regex + ")(?![a-z])");
        }
        return labelPattern;
    }

    // ------------------------------------------------------------------
    // riconoscimento degli item nel testo
    // ------------------------------------------------------------------

    private Optional<WhodasItem> itemByCode(String line) {
        Matcher matcher = ITEM_CODE_TOKEN.matcher(line);
        if (matcher.find()) {
            WhodasItem item = WhodasItem.resolve(matcher.group());
            if (item != null) {
                return Optional.of(item);
            }
            WhodasItem byOrder = byOrder(safeParse(matcher.group(2)));
            if (byOrder != null) {
                return Optional.of(byOrder);
            }
        }
        Matcher order = ITEM_ORDER_TOKEN.matcher(line);
        if (order.find()) {
            WhodasItem byOrder = byOrder(safeParse(order.group(1)));
            if (byOrder != null) {
                return Optional.of(byOrder);
            }
        }
        return Optional.empty();
    }

    private Integer safeParse(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private WhodasItem byOrder(Integer order) {
        return order == null ? null : WhodasItem.resolveByOrder(order);
    }

    private Optional<WhodasItem> itemByQuestionText(String line) {
        String normalizedLine = TextNormalizer.normalize(line);
        if (normalizedLine.length() < 12) {
            return Optional.empty();
        }
        for (WhodasItem item : WhodasItem.all()) {
            if (matchesQuestion(normalizedLine, item.officialText(), 5)
                    || matchesQuestion(normalizedLine, item.shortText(), 4)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    private boolean matchesQuestion(String normalizedLine, String question, int words) {
        String signature = TextNormalizer.signature(question, words);
        return !signature.isBlank() && TextNormalizer.contains(normalizedLine, signature);
    }

    private Optional<WhodasItem> resolveItem(String label) {
        WhodasItem byCode = WhodasItem.resolve(label);
        if (byCode != null) {
            return Optional.of(byCode);
        }
        return itemByQuestionText(label);
    }

    private String fieldKey(DocumentFields.Kind kind) {
        return switch (kind) {
            case LAST_NAME -> "patient.lastName";
            case FIRST_NAME -> "patient.firstName";
            case FISCAL_CODE -> "patient.fiscalCode";
            case BIRTH_DATE -> "patient.birthDate";
            case AGE -> "patient.age";
            case GENDER -> "patient.gender";
            case BIRTH_PLACE -> "patient.birthPlace";
            case ADDRESS -> "patient.address";
            case PHONE -> "patient.phone";
            case EMAIL -> "patient.email";
            case HEALTH_CONDITION -> "patient.healthCondition";
            case ASSESSMENT_DATE -> "assessment.date";
            case INTERVIEWER -> "assessment.interviewer";
            case COMPILE_TYPE -> "assessment.compileType";
            case NOTES -> "assessment.notes";
        };
    }

    private void buildWarnings(ParsedDocument parsed) {
        if (parsed.getItems().isEmpty()) {
            parsed.getWarnings().add("Catalogo item WHODAS 2.0 non disponibile.");
            return;
        }
        int recognized = parsed.recognizedAnswers();
        int missing = parsed.getItems().size() - recognized;
        if (recognized == 0) {
            parsed.getWarnings().add("Nessun punteggio riconosciuto nel documento: verificare che il questionario "
                    + "risulti compilato, oppure caricare il PDF del modello generato dal sistema.");
        } else if (missing > 0) {
            parsed.getWarnings().add(missing + " item su " + parsed.getItems().size()
                    + " senza punteggio riconosciuto.");
        }
        boolean hasIdentity = parsed.value("patient.fiscalCode") != null
                || (parsed.value("patient.lastName") != null && parsed.value("patient.firstName") != null);
        if (!hasIdentity) {
            parsed.getWarnings().add("Dati anagrafici del paziente non riconosciuti: l'assessment non puo' essere "
                    + "creato senza indicare il paziente (id, oppure cognome e nome).");
        }
    }
}