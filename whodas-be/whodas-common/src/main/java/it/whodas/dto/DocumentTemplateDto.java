package it.whodas.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Metadati di un modello di documento WHODAS 2.0 disponibile per il download. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Document template data transfer object")
public class DocumentTemplateDto {

    @Schema(description = "Template code", example = "WHODAS-36-IT")
    private String code;

    @Schema(description = "Template label", example = "WHODAS 2.0 36-item Italian")
    private String label;

    @Schema(description = "Template description")
    private String description;

    @Schema(description = "Number of items", example = "36")
    private int itemCount;

    @Schema(description = "Language", example = "it")
    private String language;
}