package it.whodas.document.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Traccia di un documento WHODAS 2.0 caricato dal front-end: file originale,
 * modalita' di estrazione, esito e riferimenti al paziente/assessment.
 * Il file originale viene conservato per permettere il download successivo.
 */
@Entity
@Table(name = "document_imports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Document import entity")
public class DocumentImport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Document import ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 255)
    @Schema(description = "File name", example = "whodas_assessment.pdf", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileName;

    @Column(name = "file_type", length = 20)
    @Schema(description = "File type", example = "PDF")
    private String fileType;

    @Column(name = "content_type", length = 100)
    @Schema(description = "Content type", example = "application/pdf")
    private String contentType;

    @Column(name = "file_size_bytes")
    @Schema(description = "File size in bytes", example = "1024000")
    private Long fileSizeBytes;

    @Column(name = "file_checksum", length = 64)
    @Schema(description = "File checksum (SHA-256)", example = "a1b2c3d4...")
    private String fileChecksum;

    @Column(name = "extraction_method", length = 20)
    @Schema(description = "Extraction method", example = "OCR")
    private String extractionMethod;

    @Column(name = "status", nullable = false, length = 30)
    @Schema(description = "Import status", example = "COMPLETED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    @Column(name = "compile_type_id")
    @Schema(description = "Compile type ID", example = "1")
    private Long compileTypeId;

    @Column(name = "patient_id", length = 64)
    @Schema(description = "Patient ID", example = "RSSMRA80A01H501Z")
    private String patientId;

    @Column(name = "patient_name", length = 200)
    @Schema(description = "Patient name", example = "Mario Rossi")
    private String patientName;

    @Column(name = "assessment_id")
    @Schema(description = "Assessment ID", example = "1")
    private Long assessmentId;

    @Column(name = "answers_imported")
    @Schema(description = "Number of answers imported", example = "36")
    private Integer answersImported;

    @Column(name = "fields_extracted")
    @Schema(description = "Number of fields extracted", example = "10")
    private Integer fieldsExtracted;

    @Column(name = "extracted_text_length")
    @Schema(description = "Extracted text length", example = "5000")
    private Integer extractedTextLength;

    @Lob
    @Column(name = "raw_text", columnDefinition = "TEXT")
    @Schema(description = "Raw extracted text")
    private String rawText;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "content", columnDefinition = "bytea")
    @Schema(description = "File content", accessMode = Schema.AccessMode.READ_ONLY)
    private byte[] content;

    @Lob
    @Column(name = "warnings", columnDefinition = "TEXT")
    @Schema(description = "Warnings")
    private String warnings;

    @Column(name = "created_at", nullable = false)
    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @Schema(description = "Last update timestamp", example = "2024-01-15T11:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "documentImport", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Schema(description = "Imported fields")
    private List<DocumentImportField> fields = new ArrayList<>();
}