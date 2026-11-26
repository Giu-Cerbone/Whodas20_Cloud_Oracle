package it.whodas.document.extractor;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Formati documentali accettati dal document service. Il requisito di progetto
 * ammette solo PDF, Word ed Excel: tutto il resto viene respinto con un
 * errore di validazione, non con un errore generico del server.
 */
public enum DocumentFormat {

    PDF("pdf", List.of("application/pdf", "application/x-pdf", "application/octet-stream"), "PDF"),
    DOCX("docx", List.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document"), "DOCX"),
    DOC("doc", List.of("application/msword", "application/vnd.ms-word"), "DOC"),
    XLSX("xlsx", List.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), "XLSX"),
    XLS("xls", List.of("application/vnd.ms-excel", "application/msexcel"), "XLS");

    private final String extension;
    private final List<String> contentTypes;
    private final String label;

    DocumentFormat(String extension, List<String> contentTypes, String label) {
        this.extension = extension;
        this.contentTypes = contentTypes;
        this.label = label;
    }

    public String extension() {
        return extension;
    }

    public List<String> contentTypes() {
        return contentTypes;
    }

    public String label() {
        return label;
    }

    public static Optional<DocumentFormat> byExtension(String extension) {
        if (extension == null) {
            return Optional.empty();
        }
        String trimmed = extension.trim().toLowerCase(Locale.ROOT);
        final String normalized = trimmed.startsWith(".") ? trimmed.substring(1) : trimmed;
        return Arrays.stream(values()).filter(format -> format.extension.equals(normalized)).findFirst();
    }

    /**
     * Determina il formato dal nome del file, con fallback sul content type
     * dichiarato dal browser quando l'estensione manca o e' contraddittoria.
     */
    public static Optional<DocumentFormat> detect(String fileName, String contentType) {
        if (fileName != null) {
            int dot = fileName.lastIndexOf('.');
            if (dot >= 0 && dot < fileName.length() - 1) {
                Optional<DocumentFormat> byExtension = byExtension(fileName.substring(dot + 1));
                if (byExtension.isPresent()) {
                    return byExtension;
                }
            }
        }
        if (contentType != null) {
            String normalized = contentType.trim().toLowerCase(Locale.ROOT);
            int semicolon = normalized.indexOf(';');
            if (semicolon > 0) {
                normalized = normalized.substring(0, semicolon).trim();
            }
            final String declaredType = normalized;
            for (DocumentFormat format : values()) {
                if (format.contentTypes.contains(declaredType)) {
                    return Optional.of(format);
                }
            }
            return byExtension(declaredType);
        }
        return Optional.empty();
    }

    public static String acceptedExtensions() {
        return String.join(",", Arrays.stream(values()).map(DocumentFormat::extension).toList());
    }
}