package it.whodas.assessment.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import it.whodas.assessment.domain.PatientReference;

@Repository
public interface PatientReferenceRepository extends CrudRepository<PatientReference, Long> {
    Optional<PatientReference> findById(Long id);
}
