package it.whodas.questionnaire.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.whodas.common.dto.QuestionnaireItemDto;
import it.whodas.common.exception.WhodasException;
import it.whodas.questionnaire.domain.QuestionnaireItem;
import it.whodas.questionnaire.mapper.QuestionnaireItemMapper;
import it.whodas.questionnaire.repository.QuestionnaireItemRepository;

@Service
@Transactional
public class QuestionnaireItemService {

    private static final String DEFAULT_RESPONSE_TYPE = "LIKERT_5";

    private final QuestionnaireItemRepository repository;
    private final QuestionnaireItemMapper mapper;

    public QuestionnaireItemService(QuestionnaireItemRepository repository, QuestionnaireItemMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public QuestionnaireItemDto create(QuestionnaireItemDto dto) {
        requireCode(dto);
        if (repository.findByItemCode(dto.getCode()) != null) {
            throw new WhodasException("Item code already exists: " + dto.getCode(), "ITEM_CODE_EXISTS");
        }
        QuestionnaireItem entity = mapper.toEntity(dto);
        if (entity.getResponseType() == null || entity.getResponseType().isBlank()) {
            entity.setResponseType(DEFAULT_RESPONSE_TYPE);
        }
        return mapper.toDto(repository.save(entity));
    }

    public QuestionnaireItemDto update(String itemCode, QuestionnaireItemDto dto) {
        QuestionnaireItem existing = requireExisting(itemCode);
        mapper.update(dto, existing);
        return mapper.toDto(repository.save(existing));
    }

    public void delete(String itemCode) {
        repository.delete(requireExisting(itemCode));
    }

    public QuestionnaireItemDto findByItemCode(String itemCode) {
        return mapper.toDto(requireExisting(itemCode));
    }

    public List<QuestionnaireItemDto> findByDomain(String domain) {
        return mapper.toDtoList(repository.findByDomain(domain));
    }

    public List<QuestionnaireItemDto> findByDomains(List<String> domains) {
        if (domains == null || domains.isEmpty()) {
            return List.of();
        }
        return mapper.toDtoList(repository.findByDomainIn(domains));
    }

    public List<QuestionnaireItemDto> findByItemGroup(String itemGroup) {
        return mapper.toDtoList(repository.findByItemGroup(itemGroup));
    }

    public List<QuestionnaireItemDto> findAll() {
        return mapper.toDtoList(repository.findAll());
    }

    public long countByDomain(String domain) {
        return repository.countByDomain(domain);
    }

    private void requireCode(QuestionnaireItemDto dto) {
        if (dto.getCode() == null || dto.getCode().isBlank()) {
            throw new WhodasException("Item code is required", "VALIDATION_ERROR");
        }
        if (dto.getText() == null || dto.getText().isBlank()) {
            throw new WhodasException("Question text is required", "VALIDATION_ERROR");
        }
    }

    private QuestionnaireItem requireExisting(String itemCode) {
        QuestionnaireItem existing = repository.findByItemCode(itemCode);
        if (existing == null) {
            throw new WhodasException("Questionnaire item not found: " + itemCode, "ITEM_NOT_FOUND");
        }
        return existing;
    }
}
