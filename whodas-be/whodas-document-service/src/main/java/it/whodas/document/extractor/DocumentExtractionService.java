package it.whodas.document.extractor;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import it.whodas.common.exception.WhodasException;

/**
 * Punto d'ingresso unico dell'estrazione: dato un file e il suo formato
 * restituisce il contenuto grezzo normalizzato, scegliendo il lettore adatto.
 */
@Service
public class DocumentExtractionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentExtractionService.class);

    private final List<DocumentContentExtractor> extractors;

    public DocumentExtractionService(List<DocumentContentExtractor> extractors) {
        this.extractors = extractors;
    }

    public RawContent extract(byte[] content, DocumentFormat format) {
        DocumentContentExtractor extractor = extractors.stream()
                .filter(candidate -> candidate.supports(format))
                .findFirst()
                .orElseThrow(() -> new WhodasException("Formato non supportato: " + format.label(),
                        "UNSUPPORTED_DOCUMENT_FORMAT"));
        log.debug("Estrazione {} con {}", format.label(), extractor.getClass().getSimpleName());
        return extractor.extract(content);
    }
}