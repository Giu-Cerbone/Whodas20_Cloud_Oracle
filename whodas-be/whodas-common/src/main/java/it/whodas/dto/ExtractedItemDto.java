package it.whodas.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Singolo item WHODAS 2.0 riconosciuto nel documento importato. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Extracted item data transfer object")
public class ExtractedItemDto {

    @Schema(description = "Item code", example = "WHODAS-01")
    private String itemCode;

    @Schema(description = "Domain code", example = "MOBILITY")
    private String domain;

    @Schema(description = "Domain label in Italian", example = "Mobilità")
    private String domainLabelIt;

    @Schema(description = "Question text in Italian", example = "Ha difficoltà a stare in piedi per lunghi periodi?")
    private String questionTextIt;

    @Schema(description = "Raw value extracted", example = "2")
    private String rawValue;

    @Schema(description = "Score (0-4)", example = "2")
    private Integer score;

    @Schema(description = "Confidence level", example = "0.95")
    private double confidence;

    @Schema(description = "Source of extraction", example = "OCR")
    private String source;

    @Schema(description = "Additional note")
    private String note;

    @Schema(description = "Number of days", example = "5")
    private Integer days;
}