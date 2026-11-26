package it.whodas.document.parser;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Riconosce la risposta a un item WHODAS 2.0 dentro il frammento di testo che
 * lo riguarda. Sono previsti tre livelli di evidenza, dal piu' affidabile al
 * piu' fragile:
 *
 * 1. punteggio esplicito ("Punteggio: 3", "Risposta 2", "Score = 4");
 * 2. marcatura apposta su una delle cinque etichette della scala ("X Molta",
 *    "Molta (X)", "Molta X", "Molta (3)") oppure numero isolato nella casella;
 * 3. presenza della sola etichetta di risposta in un documento che non
 *    riporta l'intera intestazione della scala.
 *
 * Quando il frammento contiene l'intera scala ("Nessuna Poca Moderata Molta
 * Moltissima") l'etichetta non basta: serve una marcatura, altrimenti il
 * documento e' semplicemente il modello vuoto e non c'e' risposta da leggere.
 */
public final class AnswerDetector {

    /** Distanza massima (in caratteri, testo normalizzato) fra marcatura ed etichetta. */
    private static final int MARKER_LABEL_WINDOW = 24;

    private static final Pattern SCORE_HINT = Pattern.compile(
            "(punteggio|score|risposta|valore|risposta data|difficulty|difficulty level)\\s*(?:assegnato|assegnata|of|:|=)?\\s*([0-4])\\b");

    private static final Pattern DAYS_HINT = Pattern.compile("(\\d{1,3})\\s*(giorni|giorno|days|day)\\b");

    private static final String MARKERS = "x*✓✔●•■❑";

    private AnswerDetector() {
    }

    public record Answer(Integer score, String rawValue, Integer days, double confidence) {

        public static Answer empty() {
            return new Answer(null, null, null, 0);
        }
    }

    public static Answer detect(String fragment) {
        if (TextNormalizer.isBlank(fragment)) {
            return Answer.empty();
        }
        String normalized = TextNormalizer.normalize(fragment);
        Integer days = detectDays(normalized);

        // 1. punteggio esplicito
        Matcher scoreMatcher = SCORE_HINT.matcher(normalized);
        if (scoreMatcher.find()) {
            int score = Integer.parseInt(scoreMatcher.group(2));
            return new Answer(score, scoreMatcher.group(0), days, 0.9);
        }

        List<WhodasScale.LabelOccurrence> labels = WhodasScale.findLabels(normalized);
        int distinctLabels = (int) labels.stream().map(WhodasScale.LabelOccurrence::score).distinct().count();

        // 2. marcatura su una delle etichette della scala
        Integer marked = detectMarkedLabel(normalized, labels);
        if (marked != null) {
            String rawValue = WhodasScale.label(marked);
            return new Answer(marked, rawValue, days, 0.75);
        }

        // 3. numero isolato: tipico del foglio di calcolo o di una tabella
        //    "D1S1 | 3 | note"
        Integer isolated = detectIsolatedScore(normalized);
        if (isolated != null) {
            return new Answer(isolated, String.valueOf(isolated), days, 0.75);
        }

        // 4. etichetta singola in un documento che non riporta l'intera scala:
        //    il valore e' leggibile ma non e' certo quale opzione sia stata barrata
        if (distinctLabels == 1) {
            WhodasScale.LabelOccurrence only = labels.get(0);
            return new Answer(only.score(), only.label(), days, 0.55);
        }

        return new Answer(null, null, days, 0);
    }

    private static Integer detectMarkedLabel(String normalized, List<WhodasScale.LabelOccurrence> labels) {
        if (labels.isEmpty()) {
            return null;
        }
        WhodasScale.LabelOccurrence best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (MARKERS.indexOf(c) < 0) {
                continue;
            }
            // Le lettere 'x' dentro il testo della domanda non sono marcature:
            // si considerano solo quelle isolate o adiacenti alle etichette.
            if (c == 'x' && i > 0 && normalized.charAt(i - 1) != ' ') {
                continue;
            }
            for (WhodasScale.LabelOccurrence label : labels) {
                int distance = distanceTo(normalized, i, label);
                if (distance <= MARKER_LABEL_WINDOW && distance < bestDistance) {
                    bestDistance = distance;
                    best = label;
                }
            }
        }
        return best == null ? null : best.score();
    }

    private static int distanceTo(String text, int markerIndex, WhodasScale.LabelOccurrence label) {
        if (markerIndex >= label.start()) {
            return label.start() - markerIndex;
        }
        return markerIndex - label.end();
    }

    private static Integer detectIsolatedScore(String normalized) {
        // Toglie le etichette della scala: quello che resta e' il contenuto
        // della casella di risposta.
        StringBuilder stripped = new StringBuilder(normalized);
        for (WhodasScale.LabelOccurrence label : WhodasScale.findLabels(normalized)) {
            replaceRange(stripped, label.start(), label.end());
        }
        Matcher matcher = Pattern.compile("(?:^|\\s)([0-4])(?:\\s|$)").matcher(stripped.toString());
        String found = null;
        while (matcher.find()) {
            if (found != null) {
                // Due numeri ambigui: non si sceglie.
                return null;
            }
            found = matcher.group(1);
        }
        return found == null ? null : Integer.valueOf(found);
    }

    private static void replaceRange(StringBuilder builder, int start, int end) {
        for (int i = start; i < end && i < builder.length(); i++) {
            builder.setCharAt(i, ' ');
        }
    }

    private static Integer detectDays(String normalized) {
        Matcher matcher = DAYS_HINT.matcher(normalized);
        if (!matcher.find()) {
            return null;
        }
        int days = Integer.parseInt(matcher.group(1));
        return days >= 0 && days <= 365 ? days : null;
    }
}