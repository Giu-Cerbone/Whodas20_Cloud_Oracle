package it.whodas.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** Esito completo di un import di documento WHODAS 2.0. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Document import result data transfer object")
public class DocumentImportResultDto {

    @Schema(description = "Import ID", example = "1")
    private Long importId;

    @Schema(description = "File name", example = "whodas_assessment.pdf")
    private String fileName;

    @Schema(description = "File type", example = "PDF")
    private String fileType;

    @Schema(description = "File size in bytes", example = "1024000")
    private long fileSizeBytes;

    @Schema(description = "Extraction method", example = "OCR")
    private String extractionMethod;

    @Schema(description = "Import status", example = "COMPLETED")
    private String status;

    @Schema(description = "Compile type ID", example = "1")
    private Long compileTypeId;

    @Schema(description = "Imported patient")
    private ImportedPatientDto patient;

    @Schema(description = "Imported assessment")
    private ImportedAssessmentDto assessment;

    @Schema(description = "Extracted fields")
    private List<ExtractedFieldDto> fields;

    @Schema(description = "Extracted items")
    private List<ExtractedItemDto> items;

    @Schema(description = "Warnings")
    private List<String> warnings;

    @Schema(description = "Extracted text length", example = "5000")
    private int extractedTextLength;

    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Download URL")
    private String downloadUrl;
}