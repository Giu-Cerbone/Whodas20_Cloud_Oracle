package it.whodas.questionnaire.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import it.whodas.common.dto.QuestionnaireItemDto;
import it.whodas.questionnaire.domain.QuestionnaireItem;

/**
 * Converte QuestionnaireItem <-> QuestionnaireItemDto.
 *
 * Serve perche' i nomi non coincidono: l'entita' usa itemCode/questionText
 * mentre il DTO espone code/text (nomi usati dal front-end). Con
 * ModelMapper in modalita' STRICT i campi venivano scartati silenziosamente,
 * quindi POST /api/v1/questionnaire/items falliva sul NOT NULL di item_code.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface QuestionnaireItemMapper {

    @Mapping(target = "code", source = "itemCode")
    @Mapping(target = "text", source = "questionText")
    QuestionnaireItemDto toDto(QuestionnaireItem entity);

    List<QuestionnaireItemDto> toDtoList(List<QuestionnaireItem> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "itemCode", source = "code")
    @Mapping(target = "questionText", source = "text")
    QuestionnaireItem toEntity(QuestionnaireItemDto dto);

    /**
     * Aggiorna l'entita' esistente lasciando intatti i campi assenti nel DTO.
     * itemCode non e' aggiornabile: e' la chiave indicata dal path della richiesta.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "itemCode", ignore = true)
    @Mapping(target = "questionText", source = "text",
             nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "domain", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "domainLabel", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "domainLabelIt", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "questionTextIt", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "icfCode", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "itemGroup", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "orderNum", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "responseType", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(QuestionnaireItemDto dto, @MappingTarget QuestionnaireItem entity);
}
