package it.whodas.patient.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.whodas.patient.domain.Patient;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Patient findByFiscalCode(String fiscalCode);
    List<Patient> findByLastNameContaining(String lastName);
    List<Patient> findByFirstNameContaining(String firstName);
    boolean existsByFiscalCode(String fiscalCode);

    List<Patient> findAllByRegisteredUserUid(String registeredUserUid);
    Optional<Patient> findByIdAndRegisteredUserUid(Long id, String registeredUserUid);
    boolean existsByFiscalCodeAndRegisteredUserUid(String fiscalCode, String registeredUserUid);
    List<Patient> findByLastNameContainingAndRegisteredUserUid(String lastName, String registeredUserUid);
    List<Patient> findByFirstNameContainingAndRegisteredUserUid(String firstName, String registeredUserUid);
    boolean existsByIdAndRegisteredUserUid(Long id, String registeredUserUid);
}
