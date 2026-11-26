package it.whodas.document.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Valore riconosciuto in un documento importato: permette di sapere
 * esattamente cosa il document service ha letto e con quale confidenza,
 * anche a distanza di mesi dall'import.
 */
@Entity
@Table(name = "document_import_fields")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Document import field entity")
public class DocumentImportField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Import field ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(name = "import_id", nullable = false)
    @Schema(description = "Import ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long importId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "import_id", nullable = false, insertable = false, updatable = false)
    @JsonIgnore
    @Schema(hidden = true)
    private DocumentImport documentImport;

    @Column(name = "field_key", nullable = false, length = 100)
    @Schema(description = "Field key", example = "fiscalCode", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fieldKey;

    @Column(name = "field_label", length = 200)
    @Schema(description = "Field label", example = "Codice Fiscale")
    private String fieldLabel;

    @Column(name = "raw_value", columnDefinition = "TEXT")
    @Schema(description = "Raw value", example = "RSSMRA80A01H501Z")
    private String rawValue;

    @Column(name = "normalized_value", columnDefinition = "TEXT")
    @Schema(description = "Normalized value", example = "RSSMRA80A01H501Z")
    private String normalizedValue;

    @Column(name = "confidence")
    @Schema(description = "Confidence", example = "0.95")
    private Double confidence;

    @Column(name = "source", length = 20)
    @Schema(description = "Source", example = "OCR")
    private String source;

    @Column(name = "created_at", nullable = false)
    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;
}