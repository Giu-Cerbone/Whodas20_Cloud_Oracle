package it.whodas.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Certificate data transfer object")
public class CertificateDto {

    @Schema(description = "Certificate ID", example = "1")
    private Long id;

    @Schema(description = "Certificate number", example = "CERT-2024-001")
    private String certificateNumber;

    @Schema(description = "Assessment ID", example = "1")
    private Long assessmentId;

    @Schema(description = "Patient ID", example = "RSSMRA80A01H501Z")
    private String patientId;

    @Schema(description = "Patient name", example = "Mario Rossi")
    private String patientName;

    @Schema(description = "Issue date (ISO format)", example = "2024-01-15")
    private String issueDate;

    @Schema(description = "Validity end date (ISO format)", example = "2025-01-15")
    private String validUntil;

    @Schema(description = "Certificate status", example = "ISSUED")
    private String status;

    @Schema(description = "Overall percent disability", example = "45.5")
    private Double overallPercentDisability;

    @Schema(description = "Overall complex score", example = "30.0")
    private Double overallComplexScore;

    @Schema(description = "Certifying body", example = "ASL Roma 1")
    private String certifyingBody;

    @Schema(description = "Doctor name", example = "Dr. Verdi")
    private String doctorName;

    @Schema(description = "Doctor license number", example = "12345")
    private String doctorLicense;

    @Schema(description = "Notes")
    private String notes;

    @Schema(description = "Domain scores")
    private List<DomainScoreDto> domainScores;

    @Schema(description = "UID dell'utenza registrata proprietaria del certificato (ldap_uid)", example = "testuser")
    private String registeredUserUid;
}