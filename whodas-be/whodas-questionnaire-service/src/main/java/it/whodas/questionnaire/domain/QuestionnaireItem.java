package it.whodas.questionnaire.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "questionnaire_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Questionnaire item entity")
public class QuestionnaireItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Item ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(name = "item_code", nullable = false, unique = true, length = 20)
    @NotBlank(message = "Item code is required")
    @Schema(description = "Item code", example = "WHODAS-01", requiredMode = Schema.RequiredMode.REQUIRED)
    private String itemCode;

    @Column(name = "domain", nullable = false, length = 50)
    @NotBlank(message = "Domain is required")
    @Schema(description = "Domain code", example = "MOBILITY", requiredMode = Schema.RequiredMode.REQUIRED)
    private String domain;

    @Column(name = "domain_label", length = 100)
    @Schema(description = "Domain label", example = "Mobility")
    private String domainLabel;

    @Column(name = "domain_label_it", length = 100)
    @Schema(description = "Domain label in Italian", example = "Mobilità")
    private String domainLabelIt;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Question text is required")
    @Schema(description = "Question text", example = "Do you have difficulty standing for long periods?", requiredMode = Schema.RequiredMode.REQUIRED)
    private String questionText;

    @Column(name = "question_text_it", columnDefinition = "TEXT")
    @Schema(description = "Question text in Italian", example = "Ha difficoltà a stare in piedi per lunghi periodi?")
    private String questionTextIt;

    @Column(name = "icf_code", length = 50)
    @Schema(description = "ICF code", example = "d450")
    private String icfCode;

    @Column(name = "item_group", length = 50)
    @Schema(description = "Item group", example = "Walking")
    private String itemGroup;

    @Column(name = "order_num")
    @Schema(description = "Order number", example = "1")
    private Integer orderNum;

    @Column(name = "response_type", length = 20)
    @Schema(description = "Response type", example = "LIKERT_5", defaultValue = "LIKERT_5")
    private String responseType = "LIKERT_5";
}