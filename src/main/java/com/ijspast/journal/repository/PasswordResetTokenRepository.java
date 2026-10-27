package com.ijspast.journal.repository;

import com.ijspast.journal.entity.PasswordResetToken;
import com.ijspast.journal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    Optional<PasswordResetToken> findByTokenHashAndUsedFalse(String tokenHash);

    Optional<PasswordResetToken> findTopByUserAndUsedFalseOrderByCreatedAtDesc(User user);

    Optional<PasswordResetToken> findTopByUserAndOtpCodeAndUsedFalseOrderByCreatedAtDesc(User user, String otpCode);

    java.util.List<PasswordResetToken> findAllByUserAndUsedFalse(User user);

    void deleteByUser(User user);
}
