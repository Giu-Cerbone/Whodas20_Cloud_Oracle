package it.whodas.document.extractor;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDNonTerminalField;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import it.whodas.common.exception.WhodasException;
import it.whodas.document.config.DocumentServiceProperties;

/**
 * Lettore dei PDF. L'ordine di estrazione e' quello che massimizza
 * l'affidabilita':
 *
 * 1. campi AcroForm: se il PDF e' stato compilato digitalmente i valori sono
 *    esatti, nessuna interpretazione e' necessaria;
 * 2. testo del PDF: per i PDF generati da stampanti o da ufficio;
 * 3. OCR: per le scansioni, dove non esiste un livello di testo.
 */
@Component
public class PdfContentExtractor implements DocumentContentExtractor {

    private static final Logger log = LoggerFactory.getLogger(PdfContentExtractor.class);

    /** Sotto questa soglia di caratteri il testo e' considerato non utilizzabile. */
    private static final int MIN_TEXT_LENGTH = 24;

    private final OcrService ocrService;
    private final DocumentServiceProperties properties;

    public PdfContentExtractor(OcrService ocrService, DocumentServiceProperties properties) {
        this.ocrService = ocrService;
        this.properties = properties;
    }

    @Override
    public boolean supports(DocumentFormat format) {
        return format == DocumentFormat.PDF;
    }

    @Override
    public RawContent extract(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            RawContent raw = new RawContent().withPageCount(document.getNumberOfPages());
            readAcroForm(document, raw);

            String text = extractText(document);
            raw.withText(text);

            if (text == null || text.strip().length() < MIN_TEXT_LENGTH) {
                String ocrText = ocrService.recognise(renderPages(document, raw));
                if (ocrText != null && !ocrText.isBlank()) {
                    raw.withText(ocrText).withMethod(ExtractionMethod.OCR);
                    if (raw.getFormValues().isEmpty()) {
                        return raw;
                    }
                } else if (raw.hasFormValues()) {
                    raw.withMethod(ExtractionMethod.ACROFORM);
                    return raw;
                } else {
                    raw.withMethod(ExtractionMethod.NONE);
                    raw.withWarning("PDF senza livello di testo e senza campi modulo: "
                            + "OCR non disponibile o non ha riconosciuto nulla.");
                    return raw;
                }
            }

            if (raw.hasFormValues()) {
                raw.withMethod(ExtractionMethod.ACROFORM);
            } else {
                raw.withMethod(ExtractionMethod.PDF_TEXT);
            }
            return raw;
        } catch (WhodasException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw new WhodasException("PDF non leggibile: " + ex.getMessage(), "PDF_NOT_READABLE");
        }
    }

    private String extractText(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        // Ordinamento per posizione: mantiene la relazione fra la domanda e la
        // casella di risposta anche quando il testo e' impaginato in piu' colonne.
        stripper.setSortByPosition(true);
        return stripper.getText(document);
    }

    private void readAcroForm(PDDocument document, RawContent raw) {
        PDDocumentCatalog catalog = document.getDocumentCatalog();
        PDAcroForm acroForm = catalog.getAcroForm();
        if (acroForm == null) {
            return;
        }
        List<PDField> fields = new ArrayList<>();
        for (PDField field : acroForm.getFieldTree()) {
            collectFields(field, fields);
        }
        for (PDField field : fields) {
            String name = field.getFullyQualifiedName();
            if (name == null) {
                continue;
            }
            String value = valueOf(field);
            if (value != null && !value.isBlank()) {
                raw.withFormValue(name, value);
            }
        }
    }

    /**
     * I campi possono essere annidati: i campi non terminali (gruppi) contengono
     * gli elementi terminali, che sono quelli con il valore.
     */
    private void collectFields(PDField field, List<PDField> accumulator) {
        accumulator.add(field);
        if (field instanceof PDNonTerminalField group) {
            for (PDField child : group.getChildren()) {
                collectFields(child, accumulator);
            }
        }
    }

    private String valueOf(PDField field) {
        if (field instanceof PDCheckBox checkBox) {
            String value = checkBox.getValue();
            // Le caselle di spunta del questionario officiale non portano un
            // valore: "Yes" significa barrata, "Off" non barrata.
            return "Yes".equalsIgnoreCase(value) || "On".equalsIgnoreCase(value) ? "1" : null;
        }
        try {
            String value = field.getValueAsString();
            if (value != null && !value.isBlank()) {
                return cleanValue(value);
            }
        } catch (RuntimeException ex) {
            log.debug("Valore del campo {} non leggibile: {}", field.getFullyQualifiedName(), ex.toString());
        }
        return null;
    }

    /**
     * Per i campi a scelta multipla PDFBox restituisce il valore racchiuso tra
     * parentesi quadre: vengono tolte per ottenere il valore reale.
     */
    private String cleanValue(String value) {
        String cleaned = value.trim();
        if (cleaned.length() > 2 && cleaned.startsWith("[") && cleaned.endsWith("]")) {
            return cleaned.substring(1, cleaned.length() - 1).trim();
        }
        return cleaned;
    }

    private List<BufferedImage> renderPages(PDDocument document, RawContent raw) throws IOException {
        int maxPages = properties.getOcr().getMaxPages();
        PDFRenderer renderer = new PDFRenderer(document);
        int pages = Math.min(document.getNumberOfPages(), maxPages);
        List<BufferedImage> images = new ArrayList<>();
        for (int index = 0; index < pages; index++) {
            images.add(renderer.renderImageWithDPI(index, properties.getOcr().getDpi(), ImageType.RGB));
        }
        if (document.getNumberOfPages() > maxPages) {
            raw.withWarning("OCR eseguito solo sulle prime " + maxPages + " pagine del documento.");
        }
        return images;
    }
}