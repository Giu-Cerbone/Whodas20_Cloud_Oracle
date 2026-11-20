package it.whodas.patient.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Patient entity")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Patient ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    @NotBlank(message = "First name is required")
    @Schema(description = "First name", example = "Mario", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    @NotBlank(message = "Last name is required")
    @Schema(description = "Last name", example = "Rossi", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;

    @Column(name = "fiscal_code", unique = true, length = 16)
    @Schema(description = "Fiscal code", example = "RSSMRA80A01H501Z")
    private String fiscalCode;

    @Column(name = "birth_date")
    @Schema(description = "Birth date", example = "1980-01-01")
    private LocalDate birthDate;

    @Column(name = "birth_place", length = 100)
    @Schema(description = "Birth place", example = "Roma")
    private String birthPlace;

    @Column(name = "gender", length = 10)
    @Schema(description = "Gender", example = "M")
    private String gender;

    @Column(name = "address", length = 255)
    @Schema(description = "Address", example = "Via Roma 1")
    private String address;

    @Column(name = "phone", length = 20)
    @Schema(description = "Phone number", example = "+39 06 1234567")
    private String phone;

    @Column(name = "email", length = 100)
    @Schema(description = "Email", example = "mario.rossi@example.com")
    private String email;

    @Column(name = "health_condition", columnDefinition = "TEXT")
    @Schema(description = "Health condition")
    private String healthCondition;

    @Column(name = "notes", columnDefinition = "TEXT")
    @Schema(description = "Notes")
    private String notes;

    @Column(name = "registered_user_uid", length = 100)
    @Schema(description = "UID dell'utenza registrata proprietaria del paziente (ldap_uid)", example = "testuser")
    private String registeredUserUid;
}