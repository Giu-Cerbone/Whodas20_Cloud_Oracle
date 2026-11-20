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
@Schema(description = "Patient data transfer object")
public class PatientDto {

    @Schema(description = "Patient ID", example = "1")
    private Long id;

    @NotBlank(message = "First name is required")
    @Schema(description = "Patient first name", example = "Mario", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Schema(description = "Patient last name", example = "Rossi", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;

    @Schema(description = "Patient fiscal code", example = "RSSMRA80A01H501Z")
    private String fiscalCode;

    @Schema(description = "Patient birth date (ISO format)", example = "1980-01-01")
    private String birthDate;

    @Schema(description = "Patient birth place", example = "Roma")
    private String birthPlace;

    @Schema(description = "Patient gender", example = "M")
    private String gender;

    @Schema(description = "Patient address", example = "Via Roma 1")
    private String address;

    @Schema(description = "Patient phone number", example = "+39 06 1234567")
    private String phone;

    @Schema(description = "Patient email", example = "mario.rossi@example.com")
    private String email;

    @Schema(description = "Patient health condition")
    private String healthCondition;

    @Schema(description = "Additional notes")
    private String notes;
}