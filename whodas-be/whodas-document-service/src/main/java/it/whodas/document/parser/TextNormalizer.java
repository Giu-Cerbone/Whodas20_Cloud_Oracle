package it.whodas.document.parser;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;

/**
 * Normalizzazione del testo estratto dai documenti: minuscole, senza
 * accenti, spazi compattati. Serve al parser per confrontare etichette,
 * numerazioni di item e risposte senza essere fragili alle differenze di
 * battitura (OCR, maiuscole, accenti, trattini).
 */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        String withoutAccents = decomposed.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return withoutAccents
                .replaceAll("[^A-Za-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Versione che mantiene i separatori '.' e '-', usata per riconoscere le
     * numerazioni degli item (D1.1, D1-1, D1S1).
     */
    public static String normalizeCode(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static boolean contains(String normalizedText, String normalizedNeedle) {
        if (normalizedNeedle == null || normalizedNeedle.isBlank() || normalizedText == null) {
            return false;
        }
        return normalizedText.contains(normalizedNeedle);
    }

    /**
     * Prime {@code words} parole significative di un testo: usate per agganciare
     * il testo di una domanda che l'OCR ha letto in modo leggermente diverso.
     */
    public static String signature(String text, int words) {
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return "";
        }
        String[] parts = normalized.split(" ");
        int limit = Math.min(words, parts.length);
        return String.join(" ", Arrays.copyOfRange(parts, 0, limit));
    }
}