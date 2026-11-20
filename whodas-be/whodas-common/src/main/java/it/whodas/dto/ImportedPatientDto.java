package it.whodas.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Paziente riconosciuto nel documento, con indicazione se e' stato creato o riusato. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Imported patient data transfer object")
public class ImportedPatientDto {

    @Schema(description = "Patient ID", example = "1")
    private Long id;

    @Schema(description = "First name", example = "Mario")
    private String firstName;

    @Schema(description = "Last name", example = "Rossi")
    private String lastName;

    @Schema(description = "Fiscal code", example = "RSSMRA80A01H501Z")
    private String fiscalCode;

    @Schema(description = "Whether the patient was created", example = "true")
    private boolean created;

    public String getFullName() {
        if (firstName == null && lastName == null) {
            return null;
        }
        return (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName);
    }
}