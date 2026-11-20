package it.whodas.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Assessment result data transfer object")
public class AssessmentResultDto {

    @Schema(description = "Assessment ID", example = "1")
    private Long assessmentId;

    @Schema(description = "Patient ID", example = "RSSMRA80A01H501Z")
    private String patientId;

    @Schema(description = "Patient name", example = "Mario Rossi")
    private String patientName;

    @Schema(description = "Assessment version", example = "36-ITEM")
    private String version;

    @Schema(description = "Assessment status", example = "COMPLETED")
    private String status;

    @Schema(description = "Total items", example = "36")
    private Integer totalItems;

    @Schema(description = "Answered items", example = "36")
    private Integer answeredItems;

    @Schema(description = "Missing items", example = "0")
    private Integer missingItems;

    @Schema(description = "Overall simple score", example = "25.0")
    private Double overallSimpleScore;

    @Schema(description = "Overall complex score", example = "30.0")
    private Double overallComplexScore;

    @Schema(description = "Overall average score", example = "2.0")
    private Double overallAverageScore;

    @Schema(description = "Overall percent disability", example = "45.5")
    private Double overallPercentDisability;

    @Schema(description = "Domain scores")
    private List<DomainScoreDto> domainScores;

    @Schema(description = "Interpretation", example = "Mild disability")
    private String interpretation;
}