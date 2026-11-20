package it.whodas.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Campo di anagrafici riconosciuto nel documento importato. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Extracted field data transfer object")
public class ExtractedFieldDto {

    @Schema(description = "Field key", example = "fiscalCode")
    private String key;

    @Schema(description = "Field label", example = "Codice Fiscale")
    private String label;

    @Schema(description = "Raw value extracted", example = "RSSMRA80A01H501Z")
    private String rawValue;

    @Schema(description = "Normalized value", example = "RSSMRA80A01H501Z")
    private String normalizedValue;

    @Schema(description = "Confidence level", example = "0.95")
    private double confidence;

    @Schema(description = "Source of extraction", example = "OCR")
    private String source;
}