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

/**
 * Entity per la tabella tab_type_compile_quest.
 *
 * Rappresenta le tre modalità di compilazione del questionario WHODAS 2.0:
 * - Autosomministrato
 * - Somministrato da un intervistatore
 * - Proxy
 */
@Entity
@Table(name = "tab_type_compile_quest")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Compile type entity")
public class TypeCompileQuest {

    @Id
    @Column(name = "id")
    @Schema(description = "Compile type ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(name = "desc_type", nullable = false, length = 100)
    @Schema(description = "Description type", example = "SELF_ADMINISTERED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String descType;

    @Column(name = "specific_desc", columnDefinition = "TEXT")
    @Schema(description = "Specific description", example = "Auto-compilazione da parte del paziente")
    private String specificDesc;

    @Column(name = "additional_notes", columnDefinition = "TEXT")
    @Schema(description = "Additional notes")
    private String additionalNotes;
}