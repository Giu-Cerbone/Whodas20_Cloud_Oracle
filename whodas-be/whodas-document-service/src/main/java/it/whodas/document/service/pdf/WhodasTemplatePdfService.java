package it.whodas.document.service.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.itextpdf.io.font.FontProgram;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.forms.PdfAcroForm;

import it.whodas.common.domain.DomainType;
import it.whodas.common.exception.WhodasException;
import it.whodas.document.config.DocumentServiceProperties;
import it.whodas.document.parser.WhodasItem;
import it.whodas.document.parser.WhodasScale;

/**
 * Genera il modello WHODAS 2.0 in PDF come modulo compilabile (AcroForm).
 *
 * Il risultato e' utilizzabile in due modi, come richiesto dal progetto:
 * - compilato digitalmente: i campi sono veri campi di modulo, si possono
 *   selezionare e completare a schermo e salvare;
 * - stampato e scritto a mano: il documento e' impaginato come il questionario
 *   ufficiale (scala 1-5 sotto ogni domanda) e le caselle restano visibili.
 *
 * I campi hanno nomi stabili (patient.firstName, item.D1S1.score,
 * assessment.interviewer...): sono gli stessi nomi che il parser
 * dell'import riconosce, quindi un modello scaricato e poi ricaricato viene
 * letto senza perdita.
 *
 * L'impaginazione e' fatta in modo esplicito (non con il motore di layout) per
 * poter posizionare ogni campo modulo nelle coordinate esatte della pagina.
 */
@Component
public class WhodasTemplatePdfService {

    private static final Logger log = LoggerFactory.getLogger(WhodasTemplatePdfService.class);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String FONT_REGULAR = "fonts/LiberationSans-Regular.ttf";
    private static final String FONT_BOLD = "fonts/LiberationSans-Bold.ttf";

    private static final float TITLE_SIZE = 15f;
    private static final float SUBTITLE_SIZE = 9.5f;
    private static final float DOMAIN_SIZE = 11.5f;
    private static final float BODY_SIZE = 9f;
    private static final float SMALL_SIZE = 7.5f;

    private final DocumentServiceProperties properties;

    public WhodasTemplatePdfService(DocumentServiceProperties properties) {
        this.properties = properties;
    }

    public Resource generate(TemplateRequest request) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument document = new PdfDocument(writer);
            try {
                PdfFont regular = loadFont(FONT_REGULAR);
                PdfFont bold = loadFont(FONT_BOLD);
                PdfAcroForm acroForm = PdfAcroForm.getAcroForm(document, true);

                PdfLayout layout = new PdfLayout(document, regular, bold, acroForm);
                drawHeader(layout, request);
                drawPatientBlock(layout, request);
                drawInstructions(layout);
                drawItems(layout, request);

                document.close();
                log.debug("Modello WHODAS 2.0 generato: {} byte", out.size());
                return new ByteArrayResource(out.toByteArray());
            } catch (IOException ex) {
                throw new WhodasException("Generazione del modello PDF non riuscita: " + ex.getMessage(),
                        "TEMPLATE_GENERATION_FAILED");
            }
        } catch (IOException ex) {
            throw new WhodasException("Generazione del modello PDF non riuscita: " + ex.getMessage(),
                    "TEMPLATE_GENERATION_FAILED");
        }
    }

    // ------------------------------------------------------------------
    // intestazione e blocco paziente
    // ------------------------------------------------------------------

    private void drawHeader(PdfLayout layout, TemplateRequest request) {
        layout.text("WHODAS 2.0", TITLE_SIZE, true, ColorConstants.BLACK, 0f);
        layout.text("World Health Organization Disability Assessment Schedule 2.0",
                SUBTITLE_SIZE, false, ColorConstants.DARK_GRAY, 2f);
        String version = request.version() == null ? "36-ITEM" : request.version();
        layout.text("Versione a " + version.toLowerCase(Locale.ROOT) + " - lingua: "
                + (request.language() == null ? "it" : request.language()),
                SUBTITLE_SIZE, false, ColorConstants.DARK_GRAY, 2f);
        if (request.compileTypeLabel() != null && !request.compileTypeLabel().isBlank()) {
            layout.text("Tipo di compilazione: " + request.compileTypeLabel(),
                    SUBTITLE_SIZE, false, ColorConstants.DARK_GRAY, 2f);
        }
        layout.horizontalRule(4f);
    }

    private void drawPatientBlock(PdfLayout layout, TemplateRequest request) {
        layout.text("Dati del paziente", DOMAIN_SIZE, true, ColorConstants.BLACK, 4f);

        layout.fieldRow(2, new PdfField("Cognome", "patient.lastName", request.lastName()),
                new PdfField("Nome", "patient.firstName", request.firstName()));
        layout.fieldRow(2, new PdfField("Codice fiscale", "patient.fiscalCode", request.fiscalCode()),
                new PdfField("Data di nascita", "patient.birthDate", formatDate(request.birthDate())));
        layout.fieldRow(2, new PdfField("Sesso", "patient.gender", genderLabel(request.gender())),
                new PdfField("Età", "patient.age", ageOf(request.birthDate())));
        layout.fieldRow(2, new PdfField("Comune di nascita", "patient.birthPlace", null),
                new PdfField("Indirizzo", "patient.address", null));
        layout.fieldRow(2, new PdfField("Telefono", "patient.phone", null),
                new PdfField("Email", "patient.email", null));
        layout.fieldRow(2, new PdfField("Condizione di salute", "patient.healthCondition", null),
                new PdfField("Note", "assessment.notes", request.notes()));

        layout.fieldRow(2, new PdfField("Intervistatore", "assessment.interviewer", request.interviewer()),
                new PdfField("Data di compilazione", "assessment.date",
                        request.assessmentDate() == null ? null : request.assessmentDate().format(DATE_FORMATTER)));
        layout.text("Tipo di compilazione", SMALL_SIZE, false, ColorConstants.DARK_GRAY, 0f);
        layout.combo("assessment.compileType", compileTypeOptions(), request.compileTypeLabel());
        layout.space(4f);
    }

    private void drawInstructions(PdfLayout layout) {
        layout.text("Negli ultimi 30 giorni, quanta difficoltà hai avuto nel:",
                BODY_SIZE, true, ColorConstants.BLACK, 2f);
        layout.text("Per ogni domanda scegli una sola risposta: "
                        + String.join("  |  ", WhodasScale.labels()) + ".",
                SMALL_SIZE, false, ColorConstants.DARK_GRAY, 0f);
        layout.horizontalRule(6f);
    }

    // ------------------------------------------------------------------
    // item del questionario
    // ------------------------------------------------------------------

    private void drawItems(PdfLayout layout, TemplateRequest request) {
        int perPage = Math.max(4, properties.getTemplate().getItemsPerPage());
        int drawnOnPage = 0;
        DomainType currentDomain = null;

        for (WhodasItem item : WhodasItem.all()) {
            if (currentDomain != item.domain()) {
                if (currentDomain != null && drawnOnPage > 0) {
                    layout.newPage();
                    drawnOnPage = 0;
                }
                currentDomain = item.domain();
                layout.text(domainTitle(currentDomain), DOMAIN_SIZE, true, ColorConstants.BLACK, 2f);
            }

            Integer score = request.answers().get(item.code());
            String line = (properties.getTemplate().isIncludeOfficialNumber()
                    ? item.officialCode() + "  " : "") + item.officialText();
            layout.text(line, BODY_SIZE, false, ColorConstants.BLACK, 0f);
            layout.scoreRow(item, score);
            drawnOnPage++;

            if (drawnOnPage >= perPage) {
                layout.newPage();
                drawnOnPage = 0;
                currentDomain = null;
            }
        }
        if (layout.canFit(24f)) {
            layout.text("Firma del paziente / dell'intervistatore: ______________________________"
                    + "     Data: ____ / ____ / ________", SMALL_SIZE, false, ColorConstants.DARK_GRAY, 6f);
        }
    }

    private String domainTitle(DomainType domain) {
        return domain.getItalianLabel() + " (" + domain.name() + ")";
    }

    private String[] compileTypeOptions() {
        return new String[]{
                "-- seleziona --",
                "Autosomministrato",
                "Somministrato da un intervistatore",
                "Proxy"
        };
    }

    private String genderLabel(String gender) {
        if (gender == null || gender.isBlank()) {
            return null;
        }
        return switch (gender.toUpperCase(Locale.ROOT)) {
            case "M" -> "Maschio";
            case "F" -> "Femmina";
            default -> gender;
        };
    }

    private String ageOf(String birthDate) {
        if (birthDate == null || birthDate.isBlank()) {
            return null;
        }
        try {
            LocalDate birth = LocalDate.parse(birthDate.trim());
            int age = LocalDate.now().getYear() - birth.getYear()
                    - (LocalDate.now().getDayOfYear() < birth.getDayOfYear() ? 1 : 0);
            return age >= 0 && age <= 120 ? String.valueOf(age) : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String formatDate(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(isoDate.trim()).format(DATE_FORMATTER);
        } catch (RuntimeException ex) {
            return isoDate;
        }
    }

    private PdfFont loadFont(String resource) throws IOException {
        try (InputStream in = WhodasTemplatePdfService.class.getResourceAsStream("/" + resource)) {
            if (in == null) {
                throw new IOException("Font non trovato sulle classpath: " + resource);
            }
            FontProgram program = FontProgramFactory.createFont(in.readAllBytes());
            return PdfFontFactory.createFont(program, PdfEncodings.IDENTITY_H);
        }
    }
}