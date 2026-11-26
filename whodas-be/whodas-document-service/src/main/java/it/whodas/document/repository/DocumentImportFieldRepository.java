package it.whodas.document.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.whodas.document.domain.DocumentImportField;

@Repository
public interface DocumentImportFieldRepository extends JpaRepository<DocumentImportField, Long> {

    List<DocumentImportField> findByImportIdOrderByIdAsc(Long importId);
}
