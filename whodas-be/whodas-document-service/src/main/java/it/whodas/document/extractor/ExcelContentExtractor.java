package it.whodas.document.extractor;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.PictureData;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import it.whodas.common.exception.WhodasException;

/**
 * Lettore dei fogli Excel (.xlsx e .xls).
 *
 * Ogni riga viene esportata come testo separato da " | ", mantenendo lo
 * schema "etichetta | valore" o "codice item | punteggio" che il parser sa
 * interpretare. Le formule vengono calcolate prima della lettura, cosi' un
 * file con punteggi calcolati non arriva al parser come testo vuoto.
 */
@Component
public class ExcelContentExtractor implements DocumentContentExtractor {

    private static final Logger log = LoggerFactory.getLogger(ExcelContentExtractor.class);

    private final OcrService ocrService;

    public ExcelContentExtractor(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    @Override
    public boolean supports(DocumentFormat format) {
        return format == DocumentFormat.XLSX || format == DocumentFormat.XLS;
    }

    @Override
    public RawContent extract(byte[] content) {
        boolean ooxml = content.length > 4 && content[0] == 'P' && content[1] == 'K'
                && content[2] == 3 && content[3] == 4;
        RawContent raw = new RawContent().withMethod(ooxml ? ExtractionMethod.XLSX : ExtractionMethod.XLS);
        StringBuilder text = new StringBuilder();
        DataFormatter formatter = new DataFormatter(java.util.Locale.ITALY);

        try (InputStream in = new ByteArrayInputStream(content);
             Workbook workbook = ooxml ? new XSSFWorkbook(in) : new HSSFWorkbook(in)) {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            raw.withPageCount(workbook.getNumberOfSheets());
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                text.append("# ").append(sheet.getSheetName()).append(System.lineSeparator());
                for (Row row : sheet) {
                    appendRow(raw, text, row, formatter, evaluator);
                }
            }
            raw.withText(text.toString());
            if (!hasUsableText(raw)) {
                appendOcr(raw, workbook);
            }
            return raw;
        } catch (Exception ex) {
            log.debug("Lettura Excel fallita: {}", ex.toString());
            throw new WhodasException("Foglio Excel non leggibile: " + ex.getMessage(), "DOCUMENT_NOT_READABLE");
        }
    }

    private void appendRow(RawContent raw, StringBuilder text, Row row, DataFormatter formatter,
                           FormulaEvaluator evaluator) {
        StringBuilder line = new StringBuilder();
        String firstLabel = null;
        String firstValue = null;
        int notEmpty = 0;
        for (int index = 0; index < row.getLastCellNum(); index++) {
            Cell cell = row.getCell(index);
            String value = cell == null ? "" : formatter.formatCellValue(cell, evaluator).strip();
            if (value.isEmpty()) {
                continue;
            }
            if (line.length() > 0) {
                line.append(" | ");
            }
            line.append(value);
            notEmpty++;
            if (notEmpty == 1) {
                firstLabel = value;
            } else if (notEmpty == 2) {
                firstValue = value;
            }
        }
        if (notEmpty == 0) {
            return;
        }
        text.append(line).append(System.lineSeparator());
        if (notEmpty == 2) {
            raw.withPair(firstLabel, firstValue);
        }
    }

    /** Il foglio puo' contenere l'immagine della pagina compilata a mano. */
    private void appendOcr(RawContent raw, Workbook workbook) {
        List<byte[]> images = new ArrayList<>();
        for (PictureData picture : workbook.getAllPictures()) {
            images.add(picture.getData());
        }
        if (images.isEmpty()) {
            raw.withWarning("Il foglio Excel non contiene celle con testo leggibile ne' immagini da analizzare.");
            return;
        }
        List<BufferedImage> decoded = new ArrayList<>();
        for (byte[] image : images) {
            try {
                BufferedImage buffered = ImageIO.read(new ByteArrayInputStream(image));
                if (buffered != null) {
                    decoded.add(buffered);
                }
            } catch (IOException ex) {
                log.debug("Immagine Excel non leggibile: {}", ex.toString());
            }
        }
        String ocrText = ocrService.recognise(decoded);
        if (ocrText == null || ocrText.isBlank()) {
            raw.withWarning("Il foglio Excel contiene solo immagini e l'OCR non e' disponibile: "
                    + "nessun valore riconosciuto.");
            return;
        }
        raw.withText(ocrText)
                .withMethod(ExtractionMethod.OCR)
                .withWarning("Testo riconosciuto con OCR sulle immagini presenti nel foglio Excel.");
    }

    private boolean hasUsableText(RawContent raw) {
        return raw.getText() != null && raw.getText().strip().length() >= 24;
    }
}