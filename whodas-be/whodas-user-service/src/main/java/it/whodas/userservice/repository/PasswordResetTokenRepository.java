package it.whodas.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.whodas.userservice.domain.PasswordResetToken;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByUsernameAndUsedFalseAndExpiryDateAfter(String username, LocalDateTime now);

    void deleteByUsernameAndUsedFalseAndExpiryDateAfter(String username, LocalDateTime now);

    void deleteByExpiryDateBefore(LocalDateTime now);
}