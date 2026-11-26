package it.whodas.assessment.rest;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import it.whodas.assessment.service.TypeCompileQuestService;
import it.whodas.common.dto.TypeCompileQuestDto;

/**
 * Controller REST per la lettura dei tipi di compilazione del questionario.
 *
 * Espone i dati di tab_type_compile_quest al front-end.
 * Le operazioni sono di sola lettura: i dati sono gestiti dagli script SQL.
 */
@RestController
@RequestMapping("/api/v1/type-compile-quest")
public class TypeCompileQuestController {

    private final TypeCompileQuestService service;

    public TypeCompileQuestController(TypeCompileQuestService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<TypeCompileQuestDto>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TypeCompileQuestDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }
}