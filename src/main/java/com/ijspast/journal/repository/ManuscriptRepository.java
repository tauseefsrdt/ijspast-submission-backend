package com.ijspast.journal.repository;

import com.ijspast.journal.entity.Manuscript;
import com.ijspast.journal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ManuscriptRepository extends JpaRepository<Manuscript, String> {

    List<Manuscript> findByAuthorUserOrderByCreatedAtDesc(User author);

    List<Manuscript> findByAuthorEmailIgnoreCaseOrderByCreatedAtDesc(String authorEmail);

    Optional<Manuscript> findFirstByAuthorEmailIgnoreCaseAndTitleIgnoreCase(String authorEmail, String title);

    Optional<Manuscript> findFirstByAuthorEmailIgnoreCaseOrderByCreatedAtDesc(String authorEmail);

    List<Manuscript> findByAssignedReviewerOrderByCreatedAtDesc(User reviewer);

    @Query("SELECT m FROM Manuscript m WHERE (m.assignedReviewer = :reviewer OR m.assignedReviewer2 = :reviewer) ORDER BY m.createdAt DESC")
    List<Manuscript> findByAssignedReviewerOrAssignedReviewer2OrderByCreatedAtDesc(@Param("reviewer") User reviewer);

    List<Manuscript> findByStatusIgnoreCase(String status);

    List<Manuscript> findByStatusOrderByCreatedAtDesc(String status);

    List<Manuscript> findAllByOrderByCreatedAtDesc();

    @Query("SELECT COUNT(m) FROM Manuscript m")
    long countTotalManuscripts();

    @Query("SELECT COUNT(m) FROM Manuscript m WHERE (m.assignedReviewer.id = :reviewerId OR m.assignedReviewer2.id = :reviewerId) AND m.status IN ('Under Review', 'Reviewer Assigned')")
    long countActiveAssignmentsByReviewerId(@Param("reviewerId") String reviewerId);

    @Query("SELECT COUNT(m) FROM Manuscript m WHERE (m.assignedReviewer.id = :reviewerId OR m.assignedReviewer2.id = :reviewerId) AND m.status IN ('Review Completed', 'Revision Requested', 'Accepted', 'Published')")
    long countCompletedReviewsByReviewerId(@Param("reviewerId") String reviewerId);
}
