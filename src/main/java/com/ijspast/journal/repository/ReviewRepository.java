package com.ijspast.journal.repository;

import com.ijspast.journal.entity.Manuscript;
import com.ijspast.journal.entity.Review;
import com.ijspast.journal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, String> {

    List<Review> findByManuscriptOrderBySubmittedAtDesc(Manuscript manuscript);

    List<Review> findByManuscriptIdOrderBySubmittedAtDesc(String manuscriptId);

    List<Review> findByReviewerOrderBySubmittedAtDesc(User reviewer);

    Optional<Review> findByManuscriptAndReviewer(Manuscript manuscript, User reviewer);
}
