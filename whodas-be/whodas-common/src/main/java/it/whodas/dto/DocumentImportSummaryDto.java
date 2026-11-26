package it.whodas.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Riga di riepilogo di un import, usata nelle liste paginate. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Document import summary data transfer object")
public class DocumentImportSummaryDto {

    @Schema(description = "Import ID", example = "1")
    private Long id;

    @Schema(description = "File name", example = "whodas_assessment.pdf")
    private String fileName;

    @Schema(description = "File type", example = "PDF")
    private String fileType;

    @Schema(description = "Extraction method", example = "OCR")
    private String extractionMethod;

    @Schema(description = "Import status", example = "COMPLETED")
    private String status;

    @Schema(description = "Patient ID", example = "RSSMRA80A01H501Z")
    private String patientId;

    @Schema(description = "Patient name", example = "Mario Rossi")
    private String patientName;

    @Schema(description = "Assessment ID", example = "1")
    private Long assessmentId;

    @Schema(description = "Number of answers imported", example = "36")
    private int answersImported;

    @Schema(description = "File size in bytes", example = "1024000")
    private long fileSizeBytes;

    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;
}