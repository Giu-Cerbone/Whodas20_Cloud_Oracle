package it.whodas.assessment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import it.whodas.assessment.domain.TypeCompileQuest;

/**
 * Repository in sola lettura per la tabella tab_type_compile_quest.
 * Non permette operazioni di modifica diretta: i dati sono gestiti
 * dagli script SQL di seeding.
 */
@Repository
public interface TypeCompileQuestRepository extends CrudRepository<TypeCompileQuest, Long> {
    List<TypeCompileQuest> findAllByOrderByIdAsc();
    Optional<TypeCompileQuest> findById(Long id);
}