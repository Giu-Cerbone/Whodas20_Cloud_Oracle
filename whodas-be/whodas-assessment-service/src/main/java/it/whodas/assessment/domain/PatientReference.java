package it.whodas.assessment.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.Immutable;

import java.time.LocalDate;

/**
 * Vista in sola lettura di patients, usata solo per comporre
 * AssessmentResultDto.patientName. Stesso schema condiviso dei microservizi.
 */
@Entity
@Immutable
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Patient reference entity")
public class PatientReference {

    @Id
    @Column(name = "id")
    @Schema(description = "Patient ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(name = "first_name")
    @Schema(description = "First name", example = "Mario")
    private String firstName;

    @Column(name = "last_name")
    @Schema(description = "Last name", example = "Rossi")
    private String lastName;

    @Column(name = "fiscal_code")
    @Schema(description = "Fiscal code", example = "RSSMRA80A01H501Z")
    private String fiscalCode;

    @Column(name = "birth_date")
    @Schema(description = "Birth date", example = "1980-01-01")
    private LocalDate birthDate;

    @Column(name = "gender")
    @Schema(description = "Gender", example = "M")
    private String gender;

    public String getFullName() {
        if (firstName == null && lastName == null) {
            return null;
        }
        return (firstName == null ? "" : firstName + " ") + (lastName == null ? "" : lastName).trim();
    }
}