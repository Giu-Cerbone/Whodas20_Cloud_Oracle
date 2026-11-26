package it.whodas.certificate.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "certificate_domain_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Certificate domain score entity")
public class CertificateDomainScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Domain score ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(name = "domain", length = 50)
    @Schema(description = "Domain code", example = "MOBILITY")
    private String domain;

    @Column(name = "domain_label", length = 100)
    @Schema(description = "Domain label", example = "Mobility")
    private String domainLabel;

    @Column(name = "domain_label_it", length = 100)
    @Schema(description = "Domain label in Italian", example = "Mobilità")
    private String domainLabelIt;

    @Column(name = "item_count")
    @Schema(description = "Number of items", example = "6")
    private Integer itemCount;

    @Column(name = "raw_score")
    @Schema(description = "Raw score", example = "12")
    private Integer rawScore;

    @Column(name = "average_score")
    @Schema(description = "Average score", example = "2.0")
    private Double averageScore;

    @Column(name = "simple_score")
    @Schema(description = "Simple score", example = "25.0")
    private Double simpleScore;

    @Column(name = "complex_score")
    @Schema(description = "Complex score", example = "30.0")
    private Double complexScore;

    @Column(name = "percent_disability")
    @Schema(description = "Percent disability", example = "45.5")
    private Double percentDisability;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "certificate_id", nullable = false)
    @JsonIgnore
    @Schema(hidden = true)
    private Certificate certificate;
}