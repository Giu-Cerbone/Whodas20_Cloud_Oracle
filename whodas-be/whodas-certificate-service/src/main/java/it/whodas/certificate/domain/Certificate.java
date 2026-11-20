package it.whodas.certificate.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Certificate entity")
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Certificate ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(name = "certificate_number", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Certificate number is required")
    @Schema(description = "Certificate number", example = "CERT-2024-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String certificateNumber;

    @Column(name = "assessment_id", nullable = false)
    @Schema(description = "Assessment ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long assessmentId;

    @Column(name = "patient_id", nullable = false)
    @Schema(description = "Patient ID", example = "RSSMRA80A01H501Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private String patientId;

    @Column(name = "patient_name", nullable = false, length = 200)
    @NotBlank(message = "Patient name is required")
    @Schema(description = "Patient name", example = "Mario Rossi", requiredMode = Schema.RequiredMode.REQUIRED)
    private String patientName;

    @Column(name = "issue_date", nullable = false)
    @Schema(description = "Issue date", example = "2024-01-15T10:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime issueDate;

    @Column(name = "valid_until")
    @Schema(description = "Validity end date", example = "2025-01-15T10:30:00")
    private LocalDateTime validUntil;

    @Column(name = "status", length = 20)
    @Schema(description = "Certificate status", example = "ISSUED", defaultValue = "ISSUED")
    private String status = "ISSUED";

    @Column(name = "overall_percent_disability")
    @Schema(description = "Overall percent disability", example = "45.5")
    private Double overallPercentDisability;

    @Column(name = "overall_complex_score")
    @Schema(description = "Overall complex score", example = "30.0")
    private Double overallComplexScore;

    @Column(name = "certifying_body", length = 200)
    @Schema(description = "Certifying body", example = "ASL Roma 1")
    private String certifyingBody;

    @Column(name = "doctor_name", length = 100)
    @Schema(description = "Doctor name", example = "Dr. Verdi")
    private String doctorName;

    @Column(name = "doctor_license", length = 50)
    @Schema(description = "Doctor license number", example = "12345")
    private String doctorLicense;

    @Column(name = "notes", columnDefinition = "TEXT")
    @Schema(description = "Notes")
    private String notes;

    @Column(name = "created_at")
    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @Schema(description = "Last update timestamp", example = "2024-01-15T11:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "certificate", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Schema(description = "Domain scores")
    private List<CertificateDomainScore> domainScores;
}