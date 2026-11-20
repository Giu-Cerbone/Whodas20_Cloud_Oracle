package it.whodas.assessment.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.whodas.assessment.domain.TypeCompileQuest;
import it.whodas.assessment.repository.TypeCompileQuestRepository;
import it.whodas.common.dto.TypeCompileQuestDto;
import it.whodas.common.exception.WhodasException;

/**
 * Servizio di sola lettura per i tipi di compilazione del questionario.
 *
 * Espone i dati della tabella tab_type_compile_quest al front-end.
 * I dati sono popolati dagli script SQL di seeding (009_type_compile_quest.sql)
 * e non sono modificabili tramite API.
 */
@Service
@Transactional(readOnly = true)
public class TypeCompileQuestService {

    private final TypeCompileQuestRepository repository;

    public TypeCompileQuestService(TypeCompileQuestRepository repository) {
        this.repository = repository;
    }

    public List<TypeCompileQuestDto> findAll() {
        return repository.findAllByOrderByIdAsc().stream().map(this::toDto).toList();
    }

    public TypeCompileQuestDto findById(Long id) {
        return repository.findById(id).map(this::toDto)
                .orElseThrow(() -> new WhodasException("TypeCompileQuest not found: " + id, "TYPE_COMPILE_QUEST_NOT_FOUND"));
    }

    private TypeCompileQuestDto toDto(TypeCompileQuest entity) {
        TypeCompileQuestDto dto = new TypeCompileQuestDto();
        dto.setId(entity.getId());
        dto.setDescType(entity.getDescType());
        dto.setSpecificDesc(entity.getSpecificDesc());
        dto.setAdditionalNotes(entity.getAdditionalNotes());
        return dto;
    }
}