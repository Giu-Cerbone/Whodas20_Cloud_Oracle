package it.whodas.assessment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.whodas.assessment.domain.Assessment;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    List<Assessment> findByPatientId(String patientId);
    List<Assessment> findByStatus(String status);

    List<Assessment> findAllByRegisteredUserUid(String registeredUserUid);
    Optional<Assessment> findByIdAndRegisteredUserUid(Long id, String registeredUserUid);
    List<Assessment> findByPatientIdAndRegisteredUserUid(String patientId, String registeredUserUid);
    boolean existsByIdAndRegisteredUserUid(Long id, String registeredUserUid);
}
