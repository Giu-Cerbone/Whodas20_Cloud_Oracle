package it.whodas.assessment.domain;

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
@Table(name = "assessments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Assessment entity")
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Assessment ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    @NotBlank(message = "Patient ID is required")
    @Schema(description = "Patient ID", example = "RSSMRA80A01H501Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private String patientId;

    @Column(name = "version", length = 20)
    @Schema(description = "Assessment version", example = "36-ITEM", defaultValue = "36-ITEM")
    private String version = "36-ITEM";

    @Column(name = "status", length = 20)
    @Schema(description = "Assessment status", example = "DRAFT", defaultValue = "DRAFT")
    private String status = "DRAFT";

    @Column(name = "mode", length = 20)
    @Schema(description = "Assessment mode", example = "SELF_ADMINISTERED", defaultValue = "SELF_ADMINISTERED")
    private String mode = "SELF_ADMINISTERED";

    @Column(name = "language", length = 10)
    @Schema(description = "Assessment language", example = "it-IT", defaultValue = "it-IT")
    private String language = "it-IT";

    @Column(name = "interviewer", length = 100)
    @Schema(description = "Interviewer name", example = "Dr. Bianchi")
    private String interviewer;

    @Column(name = "notes", columnDefinition = "TEXT")
    @Schema(description = "Notes")
    private String notes;

    @Column(name = "created_at")
    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @Schema(description = "Last update timestamp", example = "2024-01-15T11:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    @Schema(description = "Completion timestamp", example = "2024-01-15T11:30:00")
    private LocalDateTime completedAt;

    @Column(name = "total_score")
    @Schema(description = "Total score", example = "72.0")
    private Double totalScore;

    @Column(name = "overall_percent_disability")
    @Schema(description = "Overall percent disability", example = "45.5")
    private Double overallPercentDisability;

    @Column(name = "overall_simple_score")
    @Schema(description = "Overall simple score", example = "25.0")
    private Double overallSimpleScore;

    @Column(name = "overall_complex_score")
    @Schema(description = "Overall complex score", example = "30.0")
    private Double overallComplexScore;

    @Column(name = "overall_average_score")
    @Schema(description = "Overall average score", example = "2.0")
    private Double overallAverageScore;

    @Column(name = "total_items")
    @Schema(description = "Total items", example = "36")
    private Integer totalItems;

    @Column(name = "answered_items")
    @Schema(description = "Answered items", example = "36")
    private Integer answeredItems;

    @Column(name = "missing_items")
    @Schema(description = "Missing items", example = "0")
    private Integer missingItems;

    @Column(name = "interpretation", length = 50)
    @Schema(description = "Interpretation", example = "Mild disability")
    private String interpretation;

    @Column(name = "scored_at")
    @Schema(description = "Scoring timestamp", example = "2024-01-15T11:30:00")
    private LocalDateTime scoredAt;

    @OneToMany(targetEntity = ItemResponse.class, fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "assessment_id")
    @Schema(description = "Item responses")
    private List<ItemResponse> responses;

    /** Tipo di compilazione del questionario (FK a tab_type_compile_quest). */
    @Column(name = "compile_type_id")
    @Schema(description = "Compile type ID", example = "1")
    private Long compileTypeId;
}