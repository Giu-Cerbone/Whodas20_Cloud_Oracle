package it.whodas.document.service.pdf;

import java.util.ArrayList;
import java.util.List;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.ChoiceFormFieldBuilder;
import com.itextpdf.forms.fields.PdfChoiceFormField;
import com.itextpdf.forms.fields.PdfTextFormField;
import com.itextpdf.forms.fields.TextFormFieldBuilder;

import it.whodas.document.parser.WhodasItem;
import it.whodas.document.parser.WhodasScale;

/**
 * Motore di impaginazione del modello: scrive il testo e colloca i campi modulo
 * nelle coordinate esatte della pagina.
 *
 * La posizione verticale corrente e' espressa come distanza dal bordo superiore
 * della pagina; la conversione in coordinate PDF (dal basso) avviene nei metodi
 * di disegno. Ogni campo viene aggiunto all'AcroForm nello stesso ordine in cui
 * viene disegnato, cosi' la navigazione da tastiera segue l'ordine di lettura.
 */
class PdfLayout {

    private static final float MARGIN_LEFT = 36f;
    private static final float MARGIN_RIGHT = 36f;
    private static final float MARGIN_TOP = 40f;
    private static final float MARGIN_BOTTOM = 46f;
    private static final float FIELD_HEIGHT = 14f;
    private static final float FIELD_GAP = 6f;
    private static final float FIELD_PADDING = 4f;
    private static final float SCORE_FIELD_WIDTH = 132f;

    private final PdfDocument document;
    private final PdfFont regular;
    private final PdfFont bold;
    private final PdfAcroForm acroForm;

    private PdfPage page;
    private PdfCanvas canvas;
    private float cursor;
    private int pageNumber;

    PdfLayout(PdfDocument document, PdfFont regular, PdfFont bold, PdfAcroForm acroForm) {
        this.document = document;
        this.regular = regular;
        this.bold = bold;
        this.acroForm = acroForm;
        newPage();
    }

    void newPage() {
        page = document.addNewPage();
        canvas = new PdfCanvas(page);
        cursor = MARGIN_TOP;
        pageNumber++;
        drawFooter();
    }

    private void drawFooter() {
        canvas.setStrokeColor(ColorConstants.LIGHT_GRAY).setLineWidth(0.5f)
                .moveTo(MARGIN_LEFT, MARGIN_BOTTOM - 14f).lineTo(contentWidth(), MARGIN_BOTTOM - 14f).stroke();
        canvas.setFontAndSize(regular, 7.5f).setFillColor(ColorConstants.DARK_GRAY);
        canvas.beginText().moveText(contentWidth(), MARGIN_BOTTOM - 24f)
                .showText("WHODAS 2.0 - documento generato dal sistema - pagina " + pageNumber)
                .endText();
    }

    float contentWidth() {
        return page.getPageSize().getWidth() - MARGIN_LEFT - MARGIN_RIGHT;
    }

    float remainingHeight() {
        return page.getPageSize().getHeight() - MARGIN_BOTTOM - cursor;
    }

    boolean canFit(float height) {
        return remainingHeight() >= height;
    }

    /** Scrive un testo, andando a capo nella larghezza utile della pagina. */
    void text(String value, float size, boolean isBold, Color color, float spaceAfter) {
        if (value == null || value.isBlank()) {
            cursor += spaceAfter;
            return;
        }
        PdfFont font = isBold ? bold : regular;
        for (String line : wrap(value, size, font)) {
            if (!canFit(size * 1.4f)) {
                newPage();
            }
            canvas.setFontAndSize(font, size).setFillColor(color);
            canvas.beginText().moveText(MARGIN_LEFT, baseline(size)).showText(line).endText();
            cursor += size * 1.25f;
        }
        cursor += spaceAfter;
    }

    /** Etichetta di un campo, allineata al bordo sinistro. */
    private void label(String text, float x, float y, float size) {
        canvas.setFontAndSize(regular, size).setFillColor(ColorConstants.DARK_GRAY);
        canvas.beginText().moveText(x, y).showText(text).endText();
    }

    void space(float amount) {
        cursor += amount;
    }

    void horizontalRule(float spaceBefore) {
        cursor += spaceBefore;
        canvas.setStrokeColor(ColorConstants.LIGHT_GRAY).setLineWidth(0.7f)
                .moveTo(MARGIN_LEFT, baseline(2f))
                .lineTo(contentWidth(), baseline(2f))
                .stroke();
        cursor += 3f;
    }

    /** Riga di campi di testo, suddivisa in colonne uguali. */
    void fieldRow(int columns, PdfField... fields) {
        float labelSize = 7.5f;
        float width = (contentWidth() - FIELD_GAP * (columns - 1)) / columns;
        for (int index = 0; index < fields.length && index < columns; index++) {
            float x = MARGIN_LEFT + index * (width + FIELD_GAP);
            if (!canFit(labelSize + FIELD_HEIGHT + FIELD_PADDING)) {
                newPage();
            }
            PdfField field = fields[index];
            label(field.label() == null ? "" : field.label(), x, baseline(labelSize), labelSize);
            cursor += labelSize * 1.2f;
            addTextField(field.name(), x, width, field.value() == null ? "" : field.value());
            cursor += FIELD_HEIGHT + FIELD_PADDING;
        }
    }

    /** Campo a tendina: le opzioni sono testo libero, il valore selezionato coincide
 *  con l'opzione mostrata, cosi' il campo resta leggibile in un secondo passaggio. */
    void combo(String name, String[] options, String selected) {
        if (!canFit(FIELD_HEIGHT + FIELD_PADDING)) {
            newPage();
        }
        float height = FIELD_HEIGHT + 3f;
        float bottom = page.getPageSize().getHeight() - cursor - height;
        PdfChoiceFormField combo = new ChoiceFormFieldBuilder(document, name)
                .setWidgetRectangle(new Rectangle(MARGIN_LEFT, bottom, 260f, height))
                .setPage(page)
                .setOptions(options)
                .createComboBox();
        combo.setFont(regular).setFontSize(9f);
        combo.setAlternativeName(name);
        if (selected != null && !selected.isBlank()) {
            combo.setValue(selected);
        }
        acroForm.addField(combo);
        cursor += height + FIELD_PADDING;
    }

    /**
     * Riga di risposta di un item: le cinque opzioni della scala stampate come
     * riferimento, la tendina con il punteggio e il campo note.
     */
    void scoreRow(WhodasItem item, Integer score) {
        StringBuilder scale = new StringBuilder();
        for (int value = WhodasScale.MIN_SCORE; value <= WhodasScale.MAX_SCORE; value++) {
            if (scale.length() > 0) {
                scale.append("    ");
            }
            scale.append(value).append(' ').append(WhodasScale.label(value));
        }
        text(scale.toString(), 7.5f, false, ColorConstants.DARK_GRAY, 1.5f);

        if (!canFit(FIELD_HEIGHT + FIELD_PADDING + 3f)) {
            newPage();
        }
        float height = FIELD_HEIGHT + 3f;
        float bottom = page.getPageSize().getHeight() - cursor - height;
        float noteX = MARGIN_LEFT + SCORE_FIELD_WIDTH + 10f;
        float noteWidth = contentWidth() - SCORE_FIELD_WIDTH - 10f;

        PdfChoiceFormField combo = new ChoiceFormFieldBuilder(document, "item." + item.code() + ".score")
                .setWidgetRectangle(new Rectangle(MARGIN_LEFT, bottom, SCORE_FIELD_WIDTH, height))
                .setPage(page)
                .setOptions(scaleOptions())
                .createComboBox();
        combo.setFont(regular).setFontSize(9f);
        combo.setAlternativeName(item.officialCode() + " - " + item.officialText());
        if (score != null) {
            combo.setValue(String.valueOf(score));
        }
        acroForm.addField(combo);

        canvas.setFontAndSize(regular, 7.5f).setFillColor(ColorConstants.DARK_GRAY);
        canvas.beginText().moveText(noteX - 32f, bottom + 5f).showText("Note:").endText();

        PdfTextFormField note = new TextFormFieldBuilder(document, "item." + item.code() + ".note")
                .setWidgetRectangle(new Rectangle(noteX, bottom, noteWidth, height))
                .setPage(page)
                .createText();
        note.setFont(regular).setFontSize(9f);
        note.setAlternativeName("Nota per l'item " + item.officialCode());
        acroForm.addField(note);

        cursor += height + FIELD_PADDING + 3f;
    }

    private void addTextField(String name, float x, float width, String value) {
        float bottom = page.getPageSize().getHeight() - cursor - FIELD_HEIGHT;
        PdfTextFormField field = new TextFormFieldBuilder(document, name)
                .setWidgetRectangle(new Rectangle(x, bottom, width, FIELD_HEIGHT))
                .setPage(page)
                .createText();
        field.setFont(regular).setFontSize(9f);
        field.setAlternativeName(name);
        if (value != null && !value.isBlank()) {
            field.setValue(value);
        }
        acroForm.addField(field);
    }

    /**
     * Opzioni del campo punteggio: prima la voce vuota, cosi' un item non
     * compilato resta tale (e viene riconosciuto come mancante in import),
     * poi i punteggi 0..4 della scala WHODAS 2.0.
     */
    private String[] scaleOptions() {
        List<String> options = new ArrayList<>();
        options.add("");
        for (int value = WhodasScale.MIN_SCORE; value <= WhodasScale.MAX_SCORE; value++) {
            options.add(String.valueOf(value));
        }
        return options.toArray(new String[0]);
    }

    List<String> wrap(String value, float size) {
        return wrap(value, size, regular);
    }

    private List<String> wrap(String value, float size, PdfFont font) {
        List<String> lines = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return lines;
        }
        for (String paragraph : value.split("\\R")) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split("\\s+")) {
                if (word.isEmpty()) {
                    continue;
                }
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (line.length() > 0 && font.getWidth(candidate, size) > contentWidth()) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            if (!line.isEmpty()) {
                lines.add(line.toString());
            }
        }
        return lines;
    }

    private float baseline(float fontSize) {
        // Il testo e' posizionato con la base poco sopra il cursore corrente.
        return page.getPageSize().getHeight() - cursor + fontSize * 0.25f;
    }
}