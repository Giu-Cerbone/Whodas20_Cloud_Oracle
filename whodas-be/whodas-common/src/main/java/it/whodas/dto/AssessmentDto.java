package it.whodas.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Assessment data transfer object")
public class AssessmentDto {

    @Schema(description = "Assessment ID", example = "1")
    private Long id;

    @NotBlank(message = "Patient ID is required")
    @Schema(description = "Patient ID", example = "RSSMRA80A01H501Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private String patientId;

    @Schema(description = "Assessment version", example = "36-ITEM")
    private String version;

    @Schema(description = "Assessment status", example = "DRAFT")
    private String status;

    @Schema(description = "Assessment mode", example = "SELF_ADMINISTERED")
    private String mode;

    @Schema(description = "Assessment language", example = "it-IT")
    private String language;

    @Schema(description = "Interviewer name", example = "Dr. Bianchi")
    private String interviewer;

    @Schema(description = "Assessment notes")
    private String notes;

    @Schema(description = "List of item responses")
    private List<ItemResponseDto> responses;

    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2024-01-15T11:00:00")
    private LocalDateTime updatedAt;

    @Schema(description = "Completion timestamp", example = "2024-01-15T11:30:00")
    private LocalDateTime completedAt;

    @Schema(description = "Total score", example = "72.0")
    private Double totalScore;

    @Schema(description = "Overall percent disability", example = "45.5")
    private Double overallPercentDisability;

    @Schema(description = "Overall simple score", example = "25.0")
    private Double overallSimpleScore;

    @Schema(description = "Overall complex score", example = "30.0")
    private Double overallComplexScore;

    @Schema(description = "Overall average score", example = "2.0")
    private Double overallAverageScore;

    @Schema(description = "Total items", example = "36")
    private Integer totalItems;

    @Schema(description = "Answered items", example = "36")
    private Integer answeredItems;

    @Schema(description = "Missing items", example = "0")
    private Integer missingItems;

    @Schema(description = "Interpretation", example = "Mild disability")
    private String interpretation;

    @Schema(description = "Scoring timestamp", example = "2024-01-15T11:30:00")
    private LocalDateTime scoredAt;

    /** ID del tipo di compilazione (tab_type_compile_quest). */
    @Schema(description = "Compile type ID", example = "1")
    private Long compileTypeId;

    @Schema(description = "UID dell'utenza registrata proprietaria della valutazione (ldap_uid)", example = "testuser")
    private String registeredUserUid;
}