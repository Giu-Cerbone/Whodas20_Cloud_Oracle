package it.whodas.document.domain;

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
 * Risposta riconosciuta a un item WHODAS 2.0 in un documento importato.
 * La tabella conserva lo stato del riconoscimento (punteggio, confidenza e
 * sorgente) cosi' l'anteprima di un import passato resta fedele a quello che
 * era stato letto al momento del caricamento.
 */
@Entity
@Table(name = "document_import_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Document import item entity")
public class DocumentImportItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Import item ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(name = "import_id", nullable = false)
    @Schema(description = "Import ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long importId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "import_id", nullable = false, insertable = false, updatable = false)
    @Schema(hidden = true)
    private DocumentImport documentImport;

    @Column(name = "item_code", nullable = false, length = 20)
    @Schema(description = "Item code", example = "WHODAS-01", requiredMode = Schema.RequiredMode.REQUIRED)
    private String itemCode;

    @Column(name = "domain", length = 40)
    @Schema(description = "Domain code", example = "MOBILITY")
    private String domain;

    @Column(name = "domain_label_it", length = 100)
    @Schema(description = "Domain label in Italian", example = "Mobilità")
    private String domainLabelIt;

    @Column(name = "question_text_it", length = 500)
    @Schema(description = "Question text in Italian", example = "Ha difficoltà a stare in piedi per lunghi periodi?")
    private String questionTextIt;

    @Column(name = "raw_value", length = 200)
    @Schema(description = "Raw value", example = "2")
    private String rawValue;

    @Column(name = "score")
    @Schema(description = "Score (0-4)", example = "2", minimum = "0", maximum = "4")
    private Integer score;

    @Column(name = "confidence")
    @Schema(description = "Confidence", example = "0.95")
    private Double confidence;

    @Column(name = "source", length = 20)
    @Schema(description = "Source", example = "OCR")
    private String source;

    @Column(name = "note", columnDefinition = "TEXT")
    @Schema(description = "Note")
    private String note;

    @Column(name = "days")
    @Schema(description = "Days", example = "5")
    private Integer days;

    @Column(name = "created_at", nullable = false)
    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;
}