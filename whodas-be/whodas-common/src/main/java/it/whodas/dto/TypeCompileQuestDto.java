package it.whodas.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per la tabella tab_type_compile_quest.
 *
 * Trasferisce al front-end le informazioni sui tipi di compilazione
 * del questionario WHODAS 2.0: descrizione, specifica descrizione e note aggiuntive.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Compile type data transfer object")
public class TypeCompileQuestDto {

    @Schema(description = "Compile type ID", example = "1")
    private Long id;

    @Schema(description = "Description type", example = "SELF_ADMINISTERED")
    private String descType;

    @Schema(description = "Specific description", example = "Auto-compilazione da parte del paziente")
    private String specificDesc;

    @Schema(description = "Additional notes")
    private String additionalNotes;
}