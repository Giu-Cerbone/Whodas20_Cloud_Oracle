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

/**
 * Vista in sola lettura di questionnaire_items.
 *
 * Il item_responses.domain deve essere valorizzato perche' il calcolo del
 * punteggio raggruppa le risposte per dominio. Fidarsi del solo client non
 * e' sufficiente (il DTO non lo rendeva obbligatorio), quindi il servizio
 * assessment legge il dominio canonico direttamente dal catalogo: i due
 * microservizi condividono lo stesso schema.
 */
@Entity
@Immutable
@Table(name = "questionnaire_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Item catalog entry entity")
public class ItemCatalogEntry {

    @Id
    @Column(name = "id")
    @Schema(description = "Item ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(name = "item_code")
    @Schema(description = "Item code", example = "WHODAS-01")
    private String itemCode;

    @Column(name = "domain")
    @Schema(description = "Domain code", example = "MOBILITY")
    private String domain;

    @Column(name = "domain_label")
    @Schema(description = "Domain label", example = "Mobility")
    private String domainLabel;

    @Column(name = "domain_label_it")
    @Schema(description = "Domain label in Italian", example = "Mobilità")
    private String domainLabelIt;

    @Column(name = "question_text_it", columnDefinition = "TEXT")
    @Schema(description = "Question text in Italian", example = "Ha difficoltà a stare in piedi per lunghi periodi?")
    private String questionTextIt;

    @Column(name = "order_num")
    @Schema(description = "Order number", example = "1")
    private Integer orderNum;
}