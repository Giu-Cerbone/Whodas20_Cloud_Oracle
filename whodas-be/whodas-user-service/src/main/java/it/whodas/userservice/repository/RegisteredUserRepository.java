package it.whodas.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.whodas.userservice.domain.RegisteredUser;

import java.util.Optional;

@Repository
public interface RegisteredUserRepository extends JpaRepository<RegisteredUser, Long> {
    Optional<RegisteredUser> findByUsername(String username);
    Optional<RegisteredUser> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByFiscalCode(String fiscalCode);
    boolean existsByEmail(String email);
    Optional<RegisteredUser> findByLdapUid(String ldapUid);
}
