package it.whodas.document.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.whodas.document.domain.DocumentImport;

@Repository
public interface DocumentImportRepository extends JpaRepository<DocumentImport, Long> {

    List<DocumentImport> findAllByOrderByIdDesc(Pageable pageable);

    Optional<DocumentImport> findByIdAndPatientId(Long id, String patientId);
}
