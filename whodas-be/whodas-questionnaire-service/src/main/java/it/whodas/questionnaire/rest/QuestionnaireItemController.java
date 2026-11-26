package it.whodas.questionnaire.rest;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import it.whodas.common.dto.QuestionnaireItemDto;
import it.whodas.questionnaire.service.QuestionnaireItemService;

@RestController
@RequestMapping("/api/v1/questionnaire")
public class QuestionnaireItemController {

    private final QuestionnaireItemService service;

    public QuestionnaireItemController(QuestionnaireItemService service) {
        this.service = service;
    }

    @GetMapping("/items")
    public ResponseEntity<List<QuestionnaireItemDto>> getAllItems() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/items/{itemCode}")
    public ResponseEntity<QuestionnaireItemDto> getItem(@PathVariable String itemCode) {
        return ResponseEntity.ok(service.findByItemCode(itemCode));
    }

    @GetMapping("/items/domain/{domain}")
    public ResponseEntity<List<QuestionnaireItemDto>> getItemsByDomain(@PathVariable String domain) {
        return ResponseEntity.ok(service.findByDomain(domain));
    }

    @GetMapping("/items/domains")
    public ResponseEntity<List<QuestionnaireItemDto>> getItemsByDomains(@RequestParam List<String> domains) {
        return ResponseEntity.ok(service.findByDomains(domains));
    }

    @GetMapping("/items/group/{itemGroup}")
    public ResponseEntity<List<QuestionnaireItemDto>> getItemsByGroup(@PathVariable String itemGroup) {
        return ResponseEntity.ok(service.findByItemGroup(itemGroup));
    }

    @PostMapping("/items")
    public ResponseEntity<QuestionnaireItemDto> createItem(@Valid @RequestBody QuestionnaireItemDto dto) {
        return ResponseEntity.status(201).body(service.create(dto));
    }

    @PutMapping("/items/{itemCode}")
    public ResponseEntity<QuestionnaireItemDto> updateItem(@PathVariable String itemCode, @Valid @RequestBody QuestionnaireItemDto dto) {
        return ResponseEntity.ok(service.update(itemCode, dto));
    }

    @DeleteMapping("/items/{itemCode}")
    public ResponseEntity<Void> deleteItem(@PathVariable String itemCode) {
        service.delete(itemCode);
        return ResponseEntity.noContent().build();
    }
}