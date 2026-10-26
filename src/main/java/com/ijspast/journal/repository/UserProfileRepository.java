package com.ijspast.journal.repository;

import com.ijspast.journal.entity.User;
import com.ijspast.journal.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {

    Optional<UserProfile> findByUser(User user);

    Optional<UserProfile> findByUserId(String userId);

    Optional<UserProfile> findByOrcid(String orcid);
}
