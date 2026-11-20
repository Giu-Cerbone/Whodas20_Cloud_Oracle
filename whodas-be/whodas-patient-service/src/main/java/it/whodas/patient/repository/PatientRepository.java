package it.whodas.patient.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.whodas.patient.domain.Patient;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Patient findByFiscalCode(String fiscalCode);
    java.util.List<Patient> findByLastNameContaining(String lastName);
    java.util.List<Patient> findByFirstNameContaining(String firstName);
    boolean existsByFiscalCode(String fiscalCode);
}