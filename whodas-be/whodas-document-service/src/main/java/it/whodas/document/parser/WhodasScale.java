package it.whodas.document.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Scala di risposta WHODAS 2.0 (LIKERT_5) con i sinonimi accettati quando la
 * risposta arriva come testo libero da un documento.
 */
public final class WhodasScale {

    public static final int MIN_SCORE = 0;
    public static final int MAX_SCORE = 4;

    private static final Map<Integer, List<String>> SYNONYMS = new LinkedHashMap<>();

    /** Sinonimi normalizzati, dal piu' lungo al piu' corto. */
    private static final List<Synonym> ORDERED_SYNONYMS = new ArrayList<>();

    static {
        SYNONYMS.put(0, List.of(
                "nessuna", "nessuna difficolta", "nessuna difficoltà", "nulla", "no", "none",
                "no difficulty", "not at all"));
        SYNONYMS.put(1, List.of(
                "poca", "poco", "leggera", "lieve", "qualche", "minima",
                "a little", "slight", "some"));
        SYNONYMS.put(2, List.of(
                "moderata", "moderato", "media", "medio",
                "moderate", "medium"));
        SYNONYMS.put(3, List.of(
                "molta", "molto", "severa", "severo", "grave",
                "a lot", "severe", "great"));
        SYNONYMS.put(4, List.of(
                "moltissima", "molto molta", "non ho potuto farlo", "impossibile", "totale", "completa",
                "very much", "unable", "extreme", "complete"));
        for (Map.Entry<Integer, List<String>> entry : SYNONYMS.entrySet()) {
            for (String synonym : entry.getValue()) {
                ORDERED_SYNONYMS.add(new Synonym(entry.getKey(), TextNormalizer.normalize(synonym)));
            }
        }
        ORDERED_SYNONYMS.sort((a, b) -> Integer.compare(b.text().length(), a.text().length()));
    }

    private WhodasScale() {
    }

    public static List<String> labels() {
        return List.of("Nessuna", "Poca", "Moderata", "Molta", "Moltissima o non ho potuto farlo");
    }

    public static String label(int score) {
        List<String> labels = labels();
        if (score < MIN_SCORE || score > MAX_SCORE) {
            return null;
        }
        return labels.get(score);
    }

    public static boolean isValidScore(Integer score) {
        return score != null && score >= MIN_SCORE && score <= MAX_SCORE;
    }

    /**
     * Traduce un valore testuale in punteggio 0..4. Restituisce null se il testo
     * non corrisponde a nessuna etichetta della scala.
     */
    public static Integer scoreOf(String rawValue) {
        String normalized = TextNormalizer.normalize(rawValue);
        if (normalized.isBlank()) {
            return null;
        }
        // Un punteggio numerico esplicito ha la precedenza.
        if (normalized.matches("^[0-4]$")) {
            return Integer.valueOf(normalized);
        }
        for (Synonym synonym : ORDERED_SYNONYMS) {
            if (matchesLabel(normalized, synonym.text())) {
                return synonym.score();
            }
        }
        return null;
    }

    /**
     * Etichette della scala presenti nel testo, nell'ordine in cui compaiono.
     * Usata dal detector di marcature per capire quale etichetta e' stata
     * barrata/circolata.
     */
    public static List<LabelOccurrence> findLabels(String normalizedText) {
        List<LabelOccurrence> occurrences = new ArrayList<>();
        for (Synonym synonym : ORDERED_SYNONYMS) {
            int index = normalizedText.indexOf(synonym.text());
            while (index >= 0) {
                final int start = index;
                boolean covered = occurrences.stream()
                        .anyMatch(o -> start >= o.start() && start < o.end());
                if (!covered) {
                    occurrences.add(new LabelOccurrence(
                            synonym.score(), synonym.text(), index, index + synonym.text().length()));
                }
                index = normalizedText.indexOf(synonym.text(), index + 1);
            }
        }
        occurrences.sort((a, b) -> Integer.compare(a.start(), b.start()));
        return occurrences;
    }

    /**
     * Un'etichetta conta solo se appare come parola intera: evita che
     * "poco" venga riconosciuto dentro "pochissimo".
     */
    private static boolean matchesLabel(String normalizedText, String synonym) {
        String normalizedSynonym = TextNormalizer.normalize(synonym);
        if (normalizedSynonym.isBlank()) {
            return false;
        }
        int index = normalizedText.indexOf(normalizedSynonym);
        while (index >= 0) {
            boolean startOk = index == 0 || normalizedText.charAt(index - 1) == ' ';
            int end = index + normalizedSynonym.length();
            boolean endOk = end == normalizedText.length() || normalizedText.charAt(end) == ' ';
            if (startOk && endOk) {
                return true;
            }
            index = normalizedText.indexOf(normalizedSynonym, index + 1);
        }
        return false;
    }

    public record LabelOccurrence(int score, String label, int start, int end) {
    }

    private record Synonym(int score, String text) {
    }
}