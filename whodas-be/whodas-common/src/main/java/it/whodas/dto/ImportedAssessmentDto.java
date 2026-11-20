package it.whodas.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Esito della creazione/aggiornamento dell'assessment derivato dall'import. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Imported assessment data transfer object")
public class ImportedAssessmentDto {

    @Schema(description = "Assessment ID", example = "1")
    private Long id;

    @Schema(description = "Assessment status", example = "COMPLETED")
    private String status;

    @Schema(description = "Overall percent disability", example = "45.5")
    private Double overallPercentDisability;

    @Schema(description = "Interpretation", example = "Mild disability")
    private String interpretation;

    @Schema(description = "Number of answers imported", example = "36")
    private int answersImported;

    @Schema(description = "Whether the assessment was created", example = "true")
    private boolean created;
}