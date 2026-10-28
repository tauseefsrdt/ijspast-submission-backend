package com.ijspast.journal.repository;

import com.ijspast.journal.entity.Manuscript;
import com.ijspast.journal.entity.ReviewAssignment;
import com.ijspast.journal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewAssignmentRepository extends JpaRepository<ReviewAssignment, String> {

    List<ReviewAssignment> findByManuscriptOrderByRoundNumberAscAssignedDateAsc(Manuscript manuscript);

    List<ReviewAssignment> findByManuscriptIdOrderByRoundNumberAscAssignedDateAsc(String manuscriptId);

    List<ReviewAssignment> findByReviewerOrderByAssignedDateDesc(User reviewer);

    List<ReviewAssignment> findByReviewerAndAssignmentStatusOrderByAssignedDateDesc(User reviewer, String status);

    Optional<ReviewAssignment> findByManuscriptAndReviewerAndRoundNumber(Manuscript manuscript, User reviewer, int roundNumber);

    List<ReviewAssignment> findByManuscriptAndReviewer(Manuscript manuscript, User reviewer);

    Optional<ReviewAssignment> findByManuscriptIdAndReviewerIdAndRoundNumber(String manuscriptId, String reviewerId, int roundNumber);

    long countByReviewerAndAssignmentStatus(User reviewer, String status);
}
