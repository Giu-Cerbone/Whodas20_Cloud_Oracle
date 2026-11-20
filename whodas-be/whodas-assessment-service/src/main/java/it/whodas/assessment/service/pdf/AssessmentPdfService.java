package it.whodas.assessment.service.pdf;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.layout.renderer.DrawContext;
import com.itextpdf.layout.renderer.IRenderer;
import com.itextpdf.layout.renderer.LineRenderer;
import it.whodas.assessment.domain.Assessment;
import it.whodas.assessment.domain.ItemResponse;
import it.whodas.assessment.domain.PatientReference;
import it.whodas.assessment.repository.AssessmentRepository;
import it.whodas.assessment.repository.ItemCatalogRepository;
import it.whodas.assessment.repository.PatientReferenceRepository;
import it.whodas.common.domain.DomainType;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssessmentPdfService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[] SCALE_LABELS = {"", "Nessuna", "Poca", "Moderata", "Molta", "Moltissima o non ho potuto farlo"};

    private final AssessmentRepository assessmentRepository;
    private final ItemCatalogRepository itemCatalogRepository;
    private final PatientReferenceRepository patientReferenceRepository;

    public Resource generatePdf(Long assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found: " + assessmentId));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(36, 36, 36, 36);

            // Use embedded TTF fonts from classpath for proper Unicode support
            PdfFont font = loadFont("fonts/Roboto-Regular.ttf");
            PdfFont fontBold = loadFont("fonts/Roboto-Bold.ttf");

            addHeader(document, font, fontBold, assessment);
            addPatientInfo(document, font, fontBold, assessment);
            addAssessmentInfo(document, font, fontBold, assessment);
            addResponsesTable(document, font, fontBold, assessment);
            addScoresSummary(document, font, fontBold, assessment);
            addFooter(document, font, assessment);

            document.close();
            return new ByteArrayResource(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    private PdfFont loadFont(String fontPath) {
        // Try to load TTF from classpath
        try {
            InputStream is = getClass().getResourceAsStream("/" + fontPath);
            if (is != null) {
                byte[] fontBytes = is.readAllBytes();
                return PdfFontFactory.createFont(fontBytes, "WinAnsi");
            }
        } catch (Exception ignored) {}

        // Try to load from filesystem
        try {
            String[] fontPaths = {
                "C:\\Windows\\Fonts\\arial.ttf",
                "C:\\Windows\\Fonts\\arialbd.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
            };
            String target = fontPath.contains("Bold") ? fontPaths[1] : fontPaths[0];
            if (fontPath.contains("DejaVu")) {
                target = fontPath.contains("Bold") ? fontPaths[3] : fontPaths[2];
            }
            if (new File(target).exists()) {
                return PdfFontFactory.createFont(target, "WinAnsi");
            }
        } catch (Exception ignored) {}

        // Fallback: use built-in Helvetica
        try {
            return PdfFontFactory.createFont("Helvetica", "WinAnsi");
        } catch (Exception e) {
            throw new RuntimeException("Unable to load any font", e);
        }
    }

    private void addHeader(Document document, PdfFont font, PdfFont fontBold, Assessment assessment) {
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1, 3, 1}))
                .useAllAvailableWidth();

        Cell leftCell = new Cell().add(new Paragraph("WHODAS 2.0")
                .setFont(fontBold).setFontSize(18).setFontColor(ColorConstants.BLUE))
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);

        Cell centerCell = new Cell().add(new Paragraph("Questionario WHODAS 2.0 - 36 item\nWorld Health Organization Disability Assessment Schedule 2.0")
                .setFont(font).setFontSize(11).setTextAlignment(TextAlignment.CENTER))
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);

        Cell rightCell = new Cell().add(new Paragraph("Scheda di Valutazione")
                .setFont(fontBold).setFontSize(12).setTextAlignment(TextAlignment.RIGHT))
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);

        headerTable.addCell(leftCell).addCell(centerCell).addCell(rightCell);
        document.add(headerTable);

        // Add a blue line using a table with border
        Table lineTable = new Table(UnitValue.createPercentArray(new float[]{1}))
                .useAllAvailableWidth();
        lineTable.addCell(new Cell().setBorder(new SolidBorder(ColorConstants.BLUE, 2)).setPadding(0));
        document.add(lineTable);
        document.add(new Paragraph("\n"));
    }

    private void addPatientInfo(Document document, PdfFont font, PdfFont fontBold, Assessment assessment) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 3, 1, 3}))
                .useAllAvailableWidth();

        addInfoRow(table, font, fontBold, "Cognome:", getPatientLastName(assessment));
        addInfoRow(table, font, fontBold, "Nome:", getPatientFirstName(assessment));
        addInfoRow(table, font, fontBold, "Codice Fiscale:", getPatientFiscalCode(assessment));
        addInfoRow(table, font, fontBold, "Data di nascita:", getPatientBirthDate(assessment));
        addInfoRow(table, font, fontBold, "Sesso:", getPatientGender(assessment));

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addInfoRow(Table table, PdfFont font, PdfFont fontBold, String label, String value) {
        Border border = new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f);
        Cell labelCell = new Cell().add(new Paragraph(label).setFont(fontBold).setFontSize(10))
                .setBorder(border)
                .setPadding(5);
        Cell valueCell = new Cell().add(new Paragraph(value != null ? value : "").setFont(font).setFontSize(10))
                .setBorder(border)
                .setPadding(5);
        table.addCell(labelCell).addCell(valueCell);
    }

    private void addAssessmentInfo(Document document, PdfFont font, PdfFont fontBold, Assessment assessment) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 3, 1, 3}))
                .useAllAvailableWidth();

        addInfoRow(table, font, fontBold, "Data compilazione:", assessment.getCreatedAt() != null 
                ? assessment.getCreatedAt().format(DATE_FORMATTER) : "");
        addInfoRow(table, font, fontBold, "Ultimo salvataggio:", assessment.getUpdatedAt() != null 
                ? assessment.getUpdatedAt().format(DATETIME_FORMATTER) : "");
        addInfoRow(table, font, fontBold, "Stato:", assessment.getStatus());
        addInfoRow(table, font, fontBold, "Modalità:", assessment.getMode());
        addInfoRow(table, font, fontBold, "Intervistatore:", assessment.getInterviewer() != null ? assessment.getInterviewer() : "");
        addInfoRow(table, font, fontBold, "Versione:", assessment.getVersion());

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addResponsesTable(Document document, PdfFont font, PdfFont fontBold, Assessment assessment) {
        Paragraph title = new Paragraph("Risposte al Questionario")
                .setFont(fontBold).setFontSize(13).setFontColor(ColorConstants.BLUE)
                .setMarginBottom(10);
        document.add(title);

        java.util.List<ItemResponse> responses = assessment.getResponses();
        if (responses == null || responses.isEmpty()) {
            document.add(new Paragraph("Nessuna risposta registrata.").setFont(font).setFontSize(10).setFontColor(ColorConstants.GRAY));
            return;
        }

        Map<String, java.util.List<ItemResponse>> groupedByDomain = responses.stream()
                .collect(Collectors.groupingBy(
                        ItemResponse::getDomain,
                        LinkedHashMap::new,
                        Collectors.toList()));

        java.util.List<String> orderedDomains = new java.util.ArrayList<>(groupedByDomain.keySet());
        orderedDomains.sort(Comparator.comparingInt(this::domainOrder));

        for (String domain : orderedDomains) {
            java.util.List<ItemResponse> domainResponses = groupedByDomain.get(domain);
            DomainType domainType = DomainType.valueOf(domain);

            Paragraph domainTitle = new Paragraph(domainType.getItalianLabel())
                    .setFont(fontBold).setFontSize(11).setFontColor(ColorConstants.DARK_GRAY)
                    .setMarginTop(12).setMarginBottom(6);
            document.add(domainTitle);

            Table respTable = new Table(UnitValue.createPercentArray(new float[]{0.5f, 5f, 1f, 2f, 1.5f}))
                    .useAllAvailableWidth();

            addResponseHeader(respTable, fontBold);

            for (ItemResponse resp : domainResponses) {
                it.whodas.assessment.domain.ItemCatalogEntry entry = itemCatalogRepository.findByItemCode(resp.getItemCode()).orElse(null);
                String questionText = resp.getItemCode();
                if (entry != null) {
                    try {
                        String qText = entry.getQuestionTextIt();
                        if (qText != null && !qText.isBlank()) {
                            questionText = qText;
                        }
                    } catch (Exception e) {
                        // Column might not exist in view, use itemCode as fallback
                    }
                }

                Border border = new SolidBorder(ColorConstants.LIGHT_GRAY, 0.3f);
                
                Cell codeCell = new Cell().add(new Paragraph(resp.getItemCode()).setFont(font).setFontSize(9))
                        .setBorder(border).setPadding(4);
                Cell questionCell = new Cell().add(new Paragraph(questionText).setFont(font).setFontSize(9))
                        .setBorder(border).setPadding(4);
                Cell scoreCell = new Cell().add(new Paragraph(resp.getScore() != null ? resp.getScore().toString() : "-")
                        .setFont(font).setFontSize(9).setTextAlignment(TextAlignment.CENTER))
                        .setBorder(border).setPadding(4);
                Cell labelCell = new Cell().add(new Paragraph(resp.getScore() != null && resp.getScore() >= 1 && resp.getScore() <= 5
                        ? SCALE_LABELS[resp.getScore()] : "-")
                        .setFont(font).setFontSize(8).setTextAlignment(TextAlignment.CENTER))
                        .setBorder(border).setPadding(4);
                Cell daysCell = new Cell().add(new Paragraph(resp.getDays() != null ? resp.getDays().toString() : "-")
                        .setFont(font).setFontSize(9).setTextAlignment(TextAlignment.CENTER))
                        .setBorder(border).setPadding(4);

                respTable.addCell(codeCell).addCell(questionCell).addCell(scoreCell).addCell(labelCell).addCell(daysCell);
            }

            document.add(respTable);
        }
    }

    private void addResponseHeader(Table table, PdfFont fontBold) {
        String[] headers = {"Item", "Domanda", "Punteggio", "Livello", "Giorni"};
        for (String h : headers) {
            table.addHeaderCell(new Cell().add(new Paragraph(h).setFont(fontBold).setFontSize(9).setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(ColorConstants.BLUE).setPadding(4));
        }
    }

    private void addScoresSummary(Document document, PdfFont font, PdfFont fontBold, Assessment assessment) {
        document.add(new Paragraph("\n"));
        Paragraph title = new Paragraph("Punteggi e Interpretazione")
                .setFont(fontBold).setFontSize(13).setFontColor(ColorConstants.BLUE)
                .setMarginBottom(10);
        document.add(title);

        Table scoreTable = new Table(UnitValue.createPercentArray(new float[]{3, 1, 1, 1, 1, 1, 1}))
                .useAllAvailableWidth();

        String[] scoreHeaders = {"Dominio", "Item", "Raw", "Media", "Semplice", "Complesso", "% Disabilità"};
        for (String h : scoreHeaders) {
            scoreTable.addHeaderCell(new Cell().add(new Paragraph(h).setFont(fontBold).setFontSize(9).setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(ColorConstants.DARK_GRAY).setPadding(4));
        }

        if (assessment.getResponses() != null) {
            Map<String, java.util.List<ItemResponse>> groupedByDomain = assessment.getResponses().stream()
                    .collect(Collectors.groupingBy(
                            ItemResponse::getDomain,
                            LinkedHashMap::new,
                            Collectors.toList()));

            java.util.List<String> orderedDomains = new java.util.ArrayList<>(groupedByDomain.keySet());
            orderedDomains.sort(Comparator.comparingInt(this::domainOrder));

            for (String domain : orderedDomains) {
                java.util.List<ItemResponse> domainResponses = groupedByDomain.get(domain);
                DomainType domainType = DomainType.valueOf(domain);

                int itemCount = domainResponses.size();
                int rawScore = domainResponses.stream().filter(r -> r.getScore() != null).mapToInt(ItemResponse::getScore).sum();
                double avgScore = itemCount > 0 ? (double) rawScore / itemCount : 0;
                double simpleScore = rawScore;
                double maxPossible = itemCount * 4;
                double complexScore = maxPossible > 0 ? (rawScore / maxPossible) * 100 : 0;
                double percentDisability = complexScore;

                Border border = new SolidBorder(ColorConstants.LIGHT_GRAY, 0.3f);
                
                scoreTable.addCell(new Cell().add(new Paragraph(domainType.getItalianLabel()).setFont(fontBold).setFontSize(9))
                        .setBorder(border).setPadding(4));
                scoreTable.addCell(new Cell().add(new Paragraph(String.valueOf(itemCount)).setFont(font).setFontSize(9).setTextAlignment(TextAlignment.CENTER))
                        .setBorder(border).setPadding(4));
                scoreTable.addCell(new Cell().add(new Paragraph(String.valueOf(rawScore)).setFont(font).setFontSize(9).setTextAlignment(TextAlignment.CENTER))
                        .setBorder(border).setPadding(4));
                scoreTable.addCell(new Cell().add(new Paragraph(String.format("%.2f", avgScore)).setFont(font).setFontSize(9).setTextAlignment(TextAlignment.CENTER))
                        .setBorder(border).setPadding(4));
                scoreTable.addCell(new Cell().add(new Paragraph(String.format("%.2f", simpleScore)).setFont(font).setFontSize(9).setTextAlignment(TextAlignment.CENTER))
                        .setBorder(border).setPadding(4));
                scoreTable.addCell(new Cell().add(new Paragraph(String.format("%.2f", complexScore)).setFont(font).setFontSize(9).setTextAlignment(TextAlignment.CENTER))
                        .setBorder(border).setPadding(4));
                scoreTable.addCell(new Cell().add(new Paragraph(String.format("%.2f%%", percentDisability)).setFont(font).setFontSize(9).setTextAlignment(TextAlignment.CENTER))
                        .setBorder(border).setPadding(4));
            }
        }

        document.add(scoreTable);

        document.add(new Paragraph("\n"));
        Table overallTable = new Table(UnitValue.createPercentArray(new float[]{2, 1, 1, 1, 1}))
                .useAllAvailableWidth();

        String[] overallHeaders = {"Totale", "Raw Score", "Media", "Semplice", "% Disabilità"};
        for (String h : overallHeaders) {
            overallTable.addHeaderCell(new Cell().add(new Paragraph(h).setFont(fontBold).setFontSize(9).setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(ColorConstants.BLUE).setPadding(4));
        }

        int totalItems = assessment.getTotalItems() != null ? assessment.getTotalItems() : 0;
        int totalRaw = assessment.getTotalScore() != null ? assessment.getTotalScore().intValue() : 0;
        double overallAvg = assessment.getOverallAverageScore() != null ? assessment.getOverallAverageScore() : 0;
        double overallSimple = assessment.getOverallSimpleScore() != null ? assessment.getOverallSimpleScore() : 0;
        double overallPercent = assessment.getOverallPercentDisability() != null ? assessment.getOverallPercentDisability() : 0;

        Border border = new SolidBorder(ColorConstants.LIGHT_GRAY, 0.3f);
        
        overallTable.addCell(new Cell().add(new Paragraph("TOTALE").setFont(fontBold).setFontSize(9))
                .setBorder(border).setPadding(4));
        overallTable.addCell(new Cell().add(new Paragraph(String.valueOf(totalRaw)).setFont(fontBold).setFontSize(9).setTextAlignment(TextAlignment.CENTER))
                .setBorder(border).setPadding(4));
        overallTable.addCell(new Cell().add(new Paragraph(String.format("%.2f", overallAvg)).setFont(font).setFontSize(9).setTextAlignment(TextAlignment.CENTER))
                .setBorder(border).setPadding(4));
        overallTable.addCell(new Cell().add(new Paragraph(String.format("%.2f", overallSimple)).setFont(font).setFontSize(9).setTextAlignment(TextAlignment.CENTER))
                .setBorder(border).setPadding(4));
        overallTable.addCell(new Cell().add(new Paragraph(String.format("%.2f%%", overallPercent)).setFont(fontBold).setFontSize(9).setTextAlignment(TextAlignment.CENTER))
                .setBorder(border).setPadding(4));

        document.add(overallTable);

        if (assessment.getInterpretation() != null) {
            document.add(new Paragraph("\n"));
            String interpretationIt = switch (assessment.getInterpretation()) {
                case "NONE" -> "Nessuna disabilità";
                case "MILD" -> "Disabilità lieve";
                case "MODERATE" -> "Disabilità moderata";
                case "SEVERE" -> "Disabilità grave";
                case "EXTREME" -> "Disabilità estrema";
                default -> assessment.getInterpretation();
            };
            Paragraph interp = new Paragraph("Interpretazione complessiva: " + interpretationIt)
                    .setFont(fontBold).setFontSize(11).setFontColor(ColorConstants.RED);
            document.add(interp);
        }
    }

    private void addFooter(Document document, PdfFont font, Assessment assessment) {
        document.add(new Paragraph("\n\n"));
        
        // Add a gray line using a table with border
        Table lineTable = new Table(UnitValue.createPercentArray(new float[]{1}))
                .useAllAvailableWidth();
        lineTable.addCell(new Cell().setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)).setPadding(0));
        document.add(lineTable);

        String generatedAt = java.time.LocalDateTime.now().format(DATETIME_FORMATTER);
        String filename = buildFilename(assessment);

        Paragraph footer = new Paragraph("Documento generato il " + generatedAt + " | File: " + filename)
                .setFont(font).setFontSize(8).setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(footer);
    }

    private String buildFilename(Assessment assessment) {
        String fiscalCode = getPatientFiscalCode(assessment);
        String dateStr = assessment.getUpdatedAt() != null 
                ? assessment.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                : java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String status = assessment.getStatus() != null ? assessment.getStatus().toLowerCase() : "unknown";
        return String.format("%s_questionario_%s_%s.pdf", 
                fiscalCode != null ? fiscalCode : "NOCF", 
                dateStr, 
                status);
    }

    private int domainOrder(String domain) {
        try {
            return DomainType.valueOf(domain).ordinal();
        } catch (Exception e) {
            return DomainType.values().length;
        }
    }

    private String getPatientLastName(Assessment assessment) {
        return patientReferenceRepository.findById(Long.valueOf(assessment.getPatientId()))
                .map(PatientReference::getLastName).orElse("N/A");
    }

    private String getPatientFirstName(Assessment assessment) {
        return patientReferenceRepository.findById(Long.valueOf(assessment.getPatientId()))
                .map(PatientReference::getFirstName).orElse("N/A");
    }

    private String getPatientFiscalCode(Assessment assessment) {
        try {
            return patientReferenceRepository.findById(Long.valueOf(assessment.getPatientId()))
                    .map(PatientReference::getFiscalCode).orElse("NOCF");
        } catch (Exception e) {
            return "NOCF";
        }
    }

    private String getPatientBirthDate(Assessment assessment) {
        try {
            return patientReferenceRepository.findById(Long.valueOf(assessment.getPatientId()))
                    .map(p -> p.getBirthDate() != null ? p.getBirthDate().format(DATE_FORMATTER) : "N/A")
                    .orElse("N/A");
        } catch (Exception e) {
            return "N/A";
        }
    }

    private String getPatientGender(Assessment assessment) {
        try {
            return patientReferenceRepository.findById(Long.valueOf(assessment.getPatientId()))
                    .map(PatientReference::getGender).orElse("N/A");
        } catch (Exception e) {
            return "N/A";
        }
    }
}