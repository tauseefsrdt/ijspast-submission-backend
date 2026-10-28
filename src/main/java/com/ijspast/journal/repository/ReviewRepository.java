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

    List<Review> findByManuscriptIdOrderByStageAscSubmittedAtAsc(String manuscriptId);

    List<Review> findByReviewerOrderBySubmittedAtDesc(User reviewer);

    List<Review> findByManuscriptAndReviewer(Manuscript manuscript, User reviewer);

    List<Review> findByManuscriptAndReviewerOrderByStageAsc(Manuscript manuscript, User reviewer);

    Optional<Review> findByManuscriptAndReviewerAndStage(Manuscript manuscript, User reviewer, int stage);

    long countByManuscriptAndReviewer(Manuscript manuscript, User reviewer);
}
