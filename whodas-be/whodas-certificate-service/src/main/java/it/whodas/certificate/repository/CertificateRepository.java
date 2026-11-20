package it.whodas.certificate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.whodas.certificate.domain.Certificate;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Certificate findByCertificateNumber(String certificateNumber);
    java.util.List<Certificate> findByPatientId(String patientId);
    java.util.List<Certificate> findByAssessmentId(Long assessmentId);
    java.util.List<Certificate> findByStatus(String status);
}