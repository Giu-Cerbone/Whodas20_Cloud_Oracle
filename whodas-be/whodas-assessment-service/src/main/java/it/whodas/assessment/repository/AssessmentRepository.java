package it.whodas.assessment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.whodas.assessment.domain.Assessment;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    List<Assessment> findByPatientId(String patientId);
    List<Assessment> findByStatus(String status);
}
