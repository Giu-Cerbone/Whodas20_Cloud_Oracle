package it.whodas.document.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.whodas.document.domain.DocumentImportItem;

@Repository
public interface DocumentImportItemRepository extends JpaRepository<DocumentImportItem, Long> {

    List<DocumentImportItem> findByImportIdOrderByIdAsc(Long importId);
}