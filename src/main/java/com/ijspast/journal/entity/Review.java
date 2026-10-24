package com.ijspast.journal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "review_reports")
public class Review {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manuscript_id", nullable = false)
    private Manuscript manuscript;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @Column(name = "reviewer_name", nullable = false)
    private String reviewerName;

    @Column(nullable = false, length = 64)
    private String recommendation;

    @Column(name = "technical_quality", nullable = false)
    private int technicalQuality; // 1 - 5

    @Column(nullable = false)
    private int originality; // 1 - 5

    @Column(nullable = false)
    private int clarity; // 1 - 5

    @Column(name = "comments_for_editor", nullable = false, columnDefinition = "TEXT")
    private String commentsForEditor;

    @Column(name = "comments_for_author", nullable = false, columnDefinition = "TEXT")
    private String commentsForAuthor;

    @Column(name = "confidential_comments", columnDefinition = "TEXT")
    private String confidentialComments;

    @Column(name = "submitted_at", updatable = false)
    private LocalDateTime submittedAt;

    public Review() {
    }

    @PrePersist
    protected void onCreate() {
        if (this.submittedAt == null) {
            this.submittedAt = LocalDateTime.now();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Manuscript getManuscript() {
        return manuscript;
    }

    public void setManuscript(Manuscript manuscript) {
        this.manuscript = manuscript;
    }

    public User getReviewer() {
        return reviewer;
    }

    public void setReviewer(User reviewer) {
        this.reviewer = reviewer;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public int getTechnicalQuality() {
        return technicalQuality;
    }

    public void setTechnicalQuality(int technicalQuality) {
        this.technicalQuality = technicalQuality;
    }

    public int getOriginality() {
        return originality;
    }

    public void setOriginality(int originality) {
        this.originality = originality;
    }

    public int getClarity() {
        return clarity;
    }

    public void setClarity(int clarity) {
        this.clarity = clarity;
    }

    public String getCommentsForEditor() {
        return commentsForEditor;
    }

    public void setCommentsForEditor(String commentsForEditor) {
        this.commentsForEditor = commentsForEditor;
    }

    public String getCommentsForAuthor() {
        return commentsForAuthor;
    }

    public void setCommentsForAuthor(String commentsForAuthor) {
        this.commentsForAuthor = commentsForAuthor;
    }

    public String getConfidentialComments() {
        return confidentialComments;
    }

    public void setConfidentialComments(String confidentialComments) {
        this.confidentialComments = confidentialComments;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
