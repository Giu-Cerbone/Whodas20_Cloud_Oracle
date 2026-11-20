package it.whodas.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Domain score data transfer object")
public class DomainScoreDto {

    @Schema(description = "Domain code", example = "MOBILITY")
    private String domain;

    @Schema(description = "Domain label", example = "Mobility")
    private String domainLabel;

    @Schema(description = "Domain label in Italian", example = "Mobilità")
    private String domainLabelIt;

    @Schema(description = "Number of items in domain", example = "6")
    private Integer itemCount;

    @Schema(description = "Raw score", example = "12")
    private Integer rawScore;

    @Schema(description = "Average score", example = "2.0")
    private Double averageScore;

    @Schema(description = "Simple score", example = "25.0")
    private Double simpleScore;

    @Schema(description = "Complex score", example = "30.0")
    private Double complexScore;

    @Schema(description = "Percent disability", example = "45.5")
    private Double percentDisability;
}