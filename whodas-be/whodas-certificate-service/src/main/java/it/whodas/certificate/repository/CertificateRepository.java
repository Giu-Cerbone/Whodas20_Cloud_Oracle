package it.whodas.certificate.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.whodas.certificate.domain.Certificate;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Certificate findByCertificateNumber(String certificateNumber);
    List<Certificate> findByPatientId(String patientId);
    List<Certificate> findByAssessmentId(Long assessmentId);
    List<Certificate> findByStatus(String status);

    List<Certificate> findAllByRegisteredUserUid(String registeredUserUid);
    Optional<Certificate> findByIdAndRegisteredUserUid(Long id, String registeredUserUid);
    List<Certificate> findByPatientIdAndRegisteredUserUid(String patientId, String registeredUserUid);
    List<Certificate> findByAssessmentIdAndRegisteredUserUid(Long assessmentId, String registeredUserUid);
    boolean existsByIdAndRegisteredUserUid(Long id, String registeredUserUid);
}
