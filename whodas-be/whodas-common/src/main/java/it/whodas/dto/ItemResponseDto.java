package it.whodas.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Item response data transfer object")
public class ItemResponseDto {

    @Schema(description = "Item code", example = "WHODAS-01")
    private String itemCode;

    /** Opzionale: se assente il dominio viene risolto dal server da questionnaire_items. */
    @Schema(description = "Domain code", example = "MOBILITY")
    private String domain;

    @Min(value = 0, message = "Score must be between 0 and 4")
    @Max(value = 4, message = "Score must be between 0 and 4")
    @Schema(description = "Score (0-4)", example = "2", minimum = "0", maximum = "4")
    private Integer score;

    @Schema(description = "Number of days", example = "5")
    private Integer days;

    @Schema(description = "Note", example = "Some additional info")
    private String note;
}