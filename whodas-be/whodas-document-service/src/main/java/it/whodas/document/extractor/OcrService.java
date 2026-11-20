package it.whodas.document.extractor;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import it.whodas.document.config.DocumentServiceProperties;
import net.sourceforge.tess4j.Tesseract;

/**
 * OCR con Tesseract (Tess4J).
 *
 * Il motore nativo e i modelli linguistici non sono distribuiti con il
 * progetto: se non sono presenti sulla macchina l'OCR viene semplicemente
 * saltato e l'import prosegue con l'estrazione del testo, segnalando la
 * condizione tra gli avvisi. Questo evita di far fallire l'intero import per
 * un problema di ambiente.
 */
@Component
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    private final DocumentServiceProperties properties;
    private volatile String unavailableReason;

    public OcrService(DocumentServiceProperties properties) {
        this.properties = properties;
    }

    /**
     * Verifica al primo uso se Tesseract e i modelli linguistici sono
     * utilizzabili. Il risultato viene memorizzato: se il motore e' installato
     * dopo l'avvio del servizio serve un riavvio per prenderlo.
     */
    public boolean isAvailable() {
        return unavailableReason == null;
    }

    public String unavailableReason() {
        return unavailableReason;
    }

    public String recognise(List<BufferedImage> images) {
        if (!properties.getOcr().isEnabled()) {
            unavailableReason = "OCR disabilitato (whodas.document.ocr.enabled=false)";
            return null;
        }
        if (images == null || images.isEmpty()) {
            return null;
        }
        try {
            Tesseract tesseract = buildTesseract();
            StringBuilder text = new StringBuilder();
            for (BufferedImage image : images) {
                String pageText = tesseract.doOCR(image);
                if (pageText != null) {
                    text.append(pageText).append(System.lineSeparator());
                }
            }
            return text.length() == 0 ? null : text.toString();
        } catch (Throwable ex) {
            // Throwable: su Windows la libreria nativa lancia UnsatisfiedLinkError,
            // che e' un Error e non un'Exception.
            unavailableReason = "motore OCR Tesseract non disponibile: " + ex.getMessage();
            log.warn("OCR non disponibile: {}", ex.toString());
            return null;
        }
    }

    private Tesseract buildTesseract() {
        String tessdata = properties.getOcr().getTessdataPath();
        if (tessdata == null || tessdata.isBlank() || !new File(tessdata).isDirectory()) {
            throw new IllegalStateException("cartella tessdata non trovata: " + tessdata
                    + " (impostare whodas.document.ocr.tessdata-path)");
        }
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessdata);
        tesseract.setLanguage(properties.getOcr().getLanguage());
        // PSM 3: segmentazione automatica della pagina, adatta a un questionario
        // con piu' colonne e caselle di risposta.
        tesseract.setPageSegMode(3);
        return tesseract;
    }

    /**
     * Verifica a caldo se il motore e' utilizzabile, senza far partire
     * l'elaborazione di un documento. Usata dal log di avvio.
     */
    public String probe() {
        if (!properties.getOcr().isEnabled()) {
            return "OCR disabilitato";
        }
        try {
            Path tessdata = Path.of(properties.getOcr().getTessdataPath());
            if (!Files.isDirectory(tessdata)) {
                return "OCR non configurato: " + tessdata;
            }
            boolean trainedDataPresent;
            try (var stream = Files.list(tessdata)) {
                trainedDataPresent = stream.anyMatch(p -> p.getFileName().toString().endsWith(".traineddata"));
            }
            return trainedDataPresent ? "OCR pronto (Tesseract, " + properties.getOcr().getLanguage() + ")"
                    : "OCR non configurato: nessun modello linguistico in " + tessdata;
        } catch (RuntimeException | IOException ex) {
            return "OCR non configurato: " + ex.getMessage();
        }
    }
}