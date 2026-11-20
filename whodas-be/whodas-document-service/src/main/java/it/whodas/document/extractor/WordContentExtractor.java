package it.whodas.document.extractor;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableRow;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import it.whodas.common.exception.WhodasException;

/**
 * Lettore dei documenti Word (.docx e .doc).
 *
 * Il contenuto utile di un questionario compilato in Word e' quasi sempre in
 * una tabella (cella etichetta, cella valore) oppure in righe "D1.1 - Molta".
 * Il testo viene quindi esportato riga per riga e, quando possibile, le
 * coppie etichetta/valore delle tabelle vengono passate al parser come coppie
 * esplicite, che sono l'evidenza piu' affidabile dopo i campi di un modulo.
 * Se il documento non contiene testo (foto della pagina scansionata inserita
 * nel Word) si ricade sull'OCR delle immagini incorporate.
 */
@Component
public class WordContentExtractor implements DocumentContentExtractor {

    private static final Logger log = LoggerFactory.getLogger(WordContentExtractor.class);

    private final OcrService ocrService;

    public WordContentExtractor(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    @Override
    public boolean supports(DocumentFormat format) {
        return format == DocumentFormat.DOCX || format == DocumentFormat.DOC;
    }

    @Override
    public RawContent extract(byte[] content) {
        return formatOf(content) == DocumentFormat.DOC ? extractDoc(content) : extractDocx(content);
    }

    private DocumentFormat formatOf(byte[] content) {
        // La firma OOXML e' "PK\003\004": identifica i file .docx/.xlsx, che sono
        // archivi zip, rispetto ai vecchi formati OLE2 (.doc/.xls).
        if (content.length > 4 && content[0] == 'P' && content[1] == 'K'
                && content[2] == 3 && content[3] == 4) {
            return DocumentFormat.DOCX;
        }
        return DocumentFormat.DOC;
    }

    private RawContent extractDocx(byte[] content) {
        RawContent raw = new RawContent().withMethod(ExtractionMethod.DOCX);
        StringBuilder text = new StringBuilder();
        try (InputStream in = new ByteArrayInputStream(content); XWPFDocument document = new XWPFDocument(in)) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                appendLine(text, paragraph.getText());
            }
            for (XWPFTable table : document.getTables()) {
                appendTable(raw, text, table);
            }
            raw.withText(text.toString());
            List<XWPFPictureData> pictures = document.getAllPictures();
            if (!pictures.isEmpty() && !hasUsableText(raw)) {
                appendOcr(raw, pictures.stream().map(XWPFPictureData::getData).toList());
            }
            return raw;
        } catch (IOException | RuntimeException ex) {
            throw new WhodasException("Documento Word non leggibile: " + ex.getMessage(), "DOCUMENT_NOT_READABLE");
        }
    }

    private RawContent extractDoc(byte[] content) {
        RawContent raw = new RawContent().withMethod(ExtractionMethod.DOC);
        StringBuilder text = new StringBuilder();
        try (InputStream in = new ByteArrayInputStream(content); HWPFDocument document = new HWPFDocument(in)) {
            Range range = document.getRange();
            for (int index = 0; index < range.numParagraphs(); index++) {
                appendLine(text, range.getParagraph(index).text());
            }
            // Le tabelle del formato .doc non sono esposte come elenco: si
            // deducono dai paragrafi che vi stanno dentro, saltando i paragrafi
            // gia' esportati come righe di tabella.
            int tableEndOffset = -1;
            for (int index = 0; index < range.numParagraphs(); index++) {
                var paragraph = range.getParagraph(index);
                if (!paragraph.isInTable() || paragraph.getEndOffset() <= tableEndOffset) {
                    continue;
                }
                Table table = range.getTable(paragraph);
                if (table == null) {
                    continue;
                }
                for (int rowIndex = 0; rowIndex < table.numRows(); rowIndex++) {
                    TableRow row = table.getRow(rowIndex);
                    List<String> cells = new ArrayList<>();
                    for (int cellIndex = 0; cellIndex < row.numCells(); cellIndex++) {
                        cells.add(cleanCell(row.getCell(cellIndex).text()));
                    }
                    appendCells(raw, text, cells);
                    tableEndOffset = Math.max(tableEndOffset, row.getEndOffset());
                }
            }
            raw.withText(text.toString());
            return raw;
        } catch (IOException | RuntimeException ex) {
            throw new WhodasException("Documento Word (.doc) non leggibile: " + ex.getMessage(), "DOCUMENT_NOT_READABLE");
        }
    }

    private void appendTable(RawContent raw, StringBuilder text, XWPFTable table) {
        for (XWPFTableRow row : table.getRows()) {
            List<String> cells = new ArrayList<>();
            for (XWPFTableCell cell : row.getTableCells()) {
                cells.add(cleanCell(cell.getText()));
            }
            appendCells(raw, text, cells);
        }
    }

    /**
     * Una riga di tabella e' un insieme di celle. Con due celle non vuote la
     * seconda e' il valore della prima (etichetta/valore); con piu' celle la
     * riga viene passata cosi' com'e' al parser, che sa riconoscere il codice
     * dell'item e la casella di risposta.
     */
    private void appendCells(RawContent raw, StringBuilder text, List<String> cells) {
        List<String> notEmpty = cells.stream().filter(cell -> !cell.isBlank()).toList();
        if (notEmpty.isEmpty()) {
            return;
        }
        text.append(String.join(" | ", notEmpty)).append(System.lineSeparator());
        if (notEmpty.size() == 2) {
            raw.withPair(notEmpty.get(0), notEmpty.get(1));
        }
    }

    private void appendOcr(RawContent raw, List<byte[]> images) {
        List<BufferedImage> rendered = new ArrayList<>();
        for (byte[] image : images) {
            try {
                BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(image));
                if (decoded != null) {
                    rendered.add(decoded);
                }
            } catch (IOException ex) {
                log.debug("Immagine Word non leggibile: {}", ex.toString());
            }
        }
        String ocrText = ocrService.recognise(rendered);
        if (ocrText == null || ocrText.isBlank()) {
            raw.withWarning("Il documento non contiene testo utilizzabile e l'OCR non e' disponibile: "
                    + "nessun valore riconosciuto.");
            return;
        }
        raw.withText(ocrText)
                .withMethod(ExtractionMethod.OCR)
                .withWarning("Testo riconosciuto con OCR sulle immagini presenti nel documento Word.");
    }

    private boolean hasUsableText(RawContent raw) {
        return raw.getText() != null && raw.getText().strip().length() >= 24;
    }

    private void appendLine(StringBuilder text, String value) {
        String line = value == null ? "" : value.strip();
        if (!line.isEmpty()) {
            text.append(line).append(System.lineSeparator());
        }
    }

    private String cleanCell(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").strip();
    }
}