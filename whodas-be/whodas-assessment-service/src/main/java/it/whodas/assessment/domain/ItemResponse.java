package it.whodas.assessment.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "item_responses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Item response entity")
public class ItemResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Item response ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(name = "assessment_id", nullable = false, insertable = false, updatable = false)
    @Schema(description = "Assessment ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long assessmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    @JsonIgnore
    @Schema(hidden = true)
    private Assessment assessment;

    @Column(name = "item_code", nullable = false, length = 20)
    @Schema(description = "Item code", example = "WHODAS-01", requiredMode = Schema.RequiredMode.REQUIRED)
    private String itemCode;

    @Column(name = "domain", length = 50)
    @Schema(description = "Domain code", example = "MOBILITY")
    private String domain;

    @Column(name = "score")
    @Schema(description = "Score (0-4)", example = "2", minimum = "0", maximum = "4")
    private Integer score;

    @Column(name = "days")
    @Schema(description = "Number of days", example = "5")
    private Integer days;

    @Column(name = "note", columnDefinition = "TEXT")
    @Schema(description = "Note")
    private String note;

    @Column(name = "created_at")
    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;
}