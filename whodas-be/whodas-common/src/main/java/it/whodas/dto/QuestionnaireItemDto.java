package it.whodas.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Questionnaire item data transfer object")
public class QuestionnaireItemDto {

    /** item_code nell'entita QuestionnaireItem. */
    @Schema(description = "Item code", example = "WHODAS-01")
    private String code;

    /** question_text nell'entita QuestionnaireItem. */
    @NotBlank(message = "Question text is required")
    @Schema(description = "Question text", example = "Do you have difficulty standing for long periods?", requiredMode = Schema.RequiredMode.REQUIRED)
    private String text;

    @Schema(description = "Domain code", example = "MOBILITY")
    private String domain;

    @Schema(description = "Domain label", example = "Mobility")
    private String domainLabel;

    @Schema(description = "Domain label in Italian", example = "Mobilità")
    private String domainLabelIt;

    @Schema(description = "Question text in Italian", example = "Ha difficoltà a stare in piedi per lunghi periodi?")
    private String questionTextIt;

    @Schema(description = "ICF code", example = "d450")
    private String icfCode;

    @Schema(description = "Item group", example = "Walking")
    private String itemGroup;

    @Schema(description = "Order number", example = "1")
    private Integer orderNum;

    @Schema(description = "Response type", example = "LIKERT_5")
    private String responseType;
}