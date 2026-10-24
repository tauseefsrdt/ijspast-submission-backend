package com.ijspast.journal.repository;

import com.ijspast.journal.entity.Reviewer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewerRepository extends JpaRepository<Reviewer, String> {

    Optional<Reviewer> findByEmailIgnoreCase(String email);

    Optional<Reviewer> findByUserId(String userId);

    boolean existsByEmailIgnoreCase(String email);

    List<Reviewer> findAllByOrderByNameAsc();

    @Query("SELECT DISTINCT r FROM Reviewer r LEFT JOIN FETCH r.manuscriptsAsReviewer1 LEFT JOIN FETCH r.manuscriptsAsReviewer2 ORDER BY r.name ASC")
    List<Reviewer> findAllWithManuscripts();
}
