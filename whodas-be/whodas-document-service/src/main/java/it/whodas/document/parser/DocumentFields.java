package it.whodas.document.parser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Etichette dei campi di testata del questionario WHODAS 2.0 (italiano e
 * inglese) con il tipo di normalizzazione da applicare al valore riconosciuto.
 */
public final class DocumentFields {

    /** Tipi di dato riconosciuti in un documento WHODAS 2.0. */
    public enum Kind {
        LAST_NAME("Cognome"),
        FIRST_NAME("Nome"),
        FISCAL_CODE("Codice fiscale"),
        BIRTH_DATE("Data di nascita"),
        AGE("Eta'"),
        GENDER("Sesso"),
        BIRTH_PLACE("Luogo di nascita"),
        ADDRESS("Indirizzo"),
        PHONE("Telefono"),
        EMAIL("Email"),
        HEALTH_CONDITION("Condizione di salute"),
        ASSESSMENT_DATE("Data di compilazione"),
        INTERVIEWER("Intervistatore"),
        COMPILE_TYPE("Tipo di compilazione"),
        NOTES("Note");

        private final String label;

        Kind(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private static final Map<Kind, List<String>> LABELS = new LinkedHashMap<>();
    private static final Map<Kind, Kind> ALIASES = new LinkedHashMap<>();

    static {
        LABELS.put(Kind.LAST_NAME, List.of("cognome", "cognome e nome", "cognome del paziente", "last name",
                "surname", "family name"));
        LABELS.put(Kind.FIRST_NAME, List.of("nome", "nome del paziente", "first name", "given name"));
        LABELS.put(Kind.FISCAL_CODE, List.of("codice fiscale", "cod fisc", "codice fiscale del paziente",
                "fiscal code", "tax code", "cf"));
        LABELS.put(Kind.BIRTH_DATE, List.of("data di nascita", "nato il", "nata il", "data nascita", "birth date",
                "date of birth", "dob"));
        LABELS.put(Kind.AGE, List.of("eta", "eta del paziente", "age"));
        LABELS.put(Kind.GENDER, List.of("sesso", "genere", "gender", "sex"));
        LABELS.put(Kind.BIRTH_PLACE, List.of("luogo di nascita", "comune di nascita", "nato a", "birth place"));
        LABELS.put(Kind.ADDRESS, List.of("indirizzo", "indirizzo di residenza", "residenza", "address"));
        LABELS.put(Kind.PHONE, List.of("telefono", "tel", "recapito", "phone", "telephone"));
        LABELS.put(Kind.EMAIL, List.of("email", "e mail", "posta elettronica", "e-mail"));
        LABELS.put(Kind.HEALTH_CONDITION, List.of("condizione di salute", "condizioni di salute", "stato di salute",
                "health condition", "problemi di salute"));
        LABELS.put(Kind.ASSESSMENT_DATE, List.of("data", "data di compilazione", "data compilazione", "data del questionario",
                "date", "compilato il"));
        LABELS.put(Kind.INTERVIEWER, List.of("intervistatore", "intervistata da", "somaticatore", "compilato da",
                "interviewer", "compiled by"));
        LABELS.put(Kind.COMPILE_TYPE, List.of("tipo di compilazione", "modalita di compilazione", "compilato da",
                "somministrato da", "type of compilation", "compilation type"));
        LABELS.put(Kind.NOTES, List.of("note", "osservazioni", "annotazioni", "notes"));

        // "Compilato da" compare in entrambe le tabelle: nel documento e' l'intervistatore.
        ALIASES.put(Kind.COMPILE_TYPE, Kind.INTERVIEWER);
    }

    private DocumentFields() {
    }

    /** Etichette accettate per un tipo di dato, per il pattern del parser. */
    public static List<String> labelsOf(Kind kind) {
        return LABELS.getOrDefault(kind, List.of());
    }

    /**
     * Cerca il tipo di campo a cui appartiene un'etichetta.
     * La ricerca e' fatta su testo normalizzato e tollera etichette con
     * punteggiatura finale ("Cognome:").
     */
    public static Kind resolveLabel(String label) {
        String normalized = TextNormalizer.normalize(label);
        if (normalized.isBlank()) {
            return null;
        }
        Kind best = null;
        int bestLength = 0;
        for (Map.Entry<Kind, List<String>> entry : LABELS.entrySet()) {
            for (String candidate : entry.getValue()) {
                String normalizedCandidate = TextNormalizer.normalize(candidate);
                if (normalizedCandidate.isEmpty()) {
                    continue;
                }
                boolean matches = normalized.equals(normalizedCandidate)
                        || normalized.startsWith(normalizedCandidate)
                        || normalized.contains(normalizedCandidate);
                if (matches && normalizedCandidate.length() > bestLength) {
                    bestLength = normalizedCandidate.length();
                    best = entry.getKey();
                }
            }
        }
        if (best == null) {
            return null;
        }
        Kind alias = ALIASES.get(best);
        return alias != null ? alias : best;
    }

/** Normalizza un valore grezzo nel formato atteso dal dato di destinazione. */
    public static String normalizeValue(Kind kind, String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String value = rawValue.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
        return switch (kind) {
            case FISCAL_CODE -> value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
            case GENDER -> normalizeGender(value);
            case BIRTH_DATE, ASSESSMENT_DATE -> normalizeDate(value);
            case AGE -> normalizeAge(value);
            case LAST_NAME, FIRST_NAME, INTERVIEWER -> value;
            default -> value.isBlank() ? null : value;
        };
    }

    private static String normalizeGender(String value) {
        String normalized = TextNormalizer.normalize(value);
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.contains("masch") || normalized.startsWith("m")) {
            return "M";
        }
        if (normalized.contains("femmin") || normalized.startsWith("f")) {
            return "F";
        }
        if (normalized.contains("altro") || normalized.contains("non bin")) {
            return "Altro";
        }
        return null;
    }

    private static String normalizeAge(String value) {
        Matcher matcher = Pattern.compile("(\\d{1,3})").matcher(value);
        if (!matcher.find()) {
            return null;
        }
        int age = Integer.parseInt(matcher.group(1));
        return age > 0 && age <= 120 ? String.valueOf(age) : null;
    }

    /** Converte una data in formato ISO (yyyy-MM-dd), accettando i formati italiani. */
    public static String normalizeDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String cleaned = value.trim().replace('.', '/').replace('-', '/');
        Matcher dayMonthYear = Pattern.compile("^(\\d{1,2})/(\\d{1,2})/(\\d{2,4})").matcher(cleaned);
        if (dayMonthYear.find()) {
            return toIso(dayMonthYear.group(3), dayMonthYear.group(2), dayMonthYear.group(1));
        }
        Matcher yearMonthDay = Pattern.compile("^(\\d{4})/(\\d{1,2})/(\\d{1,2})").matcher(cleaned);
        if (yearMonthDay.find()) {
            return toIso(yearMonthDay.group(1), yearMonthDay.group(2), yearMonthDay.group(3));
        }
        Matcher compact = Pattern.compile("^(\\d{4})(\\d{2})(\\d{2})").matcher(cleaned);
        if (compact.find()) {
            return toIso(compact.group(1), compact.group(2), compact.group(3));
        }
        return null;
    }

    private static String toIso(String year, String month, String day) {
        try {
            int y = Integer.parseInt(year);
            int m = Integer.parseInt(month);
            int d = Integer.parseInt(day);
            if (y < 100) {
                y += y > 30 ? 1900 : 2000;
            }
            return String.format("%04d-%02d-%02d", y, m, d);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}