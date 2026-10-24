package com.ijspast.journal.repository;

import com.ijspast.journal.entity.AccountSetupToken;
import com.ijspast.journal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountSetupTokenRepository extends JpaRepository<AccountSetupToken, String> {

    Optional<AccountSetupToken> findByTokenHash(String tokenHash);

    Optional<AccountSetupToken> findByTokenHashAndUsedFalse(String tokenHash);

    void deleteByUser(User user);
}
