package com.ijspast.journal.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "review_assignments", uniqueConstraints = {
    @UniqueConstraint(name = "uk_assignment_unique", columnNames = {"manuscript_id", "reviewer_user_id", "round_number"})
})
public class ReviewAssignment {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manuscript_id", nullable = false)
    private Manuscript manuscript;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_user_id", nullable = false)
    private User reviewer;

    @Column(name = "round_number", nullable = false)
    private int roundNumber = 1;

    @Column(name = "assignment_status", nullable = false, length = 32)
    private String assignmentStatus = "ASSIGNED";

    @Column(name = "assigned_date", nullable = false)
    private LocalDate assignedDate;

    @Column(name = "review_deadline", nullable = false)
    private LocalDate reviewDeadline;

    @Column(name = "stage2_enabled", nullable = false)
    private boolean stage2Enabled = false;

    @Column(name = "revision_forwarded", nullable = false)
    private boolean revisionForwarded = false;

    @Column(name = "slot_number")
    private Integer slotNumber = 1;

    @Column(name = "reassigned_date")
    private LocalDateTime reassignedDate;

    @Column(name = "reassignment_reason", columnDefinition = "TEXT")
    private String reassignmentReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "superseded_by_reviewer_id")
    private User supersededByReviewer;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Review> reports = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ReviewAssignment() {
    }

    public ReviewAssignment(String id, Manuscript manuscript, User reviewer, int roundNumber, LocalDate assignedDate, LocalDate reviewDeadline, String notes) {
        this.id = id;
        this.manuscript = manuscript;
        this.reviewer = reviewer;
        this.roundNumber = roundNumber;
        this.assignedDate = assignedDate;
        this.reviewDeadline = reviewDeadline;
        this.notes = notes;
        this.assignmentStatus = "ASSIGNED";
        this.slotNumber = 1;
    }

    public ReviewAssignment(String id, Manuscript manuscript, User reviewer, int roundNumber, LocalDate assignedDate, LocalDate reviewDeadline, String notes, int slotNumber) {
        this.id = id;
        this.manuscript = manuscript;
        this.reviewer = reviewer;
        this.roundNumber = roundNumber;
        this.assignedDate = assignedDate;
        this.reviewDeadline = reviewDeadline;
        this.notes = notes;
        this.assignmentStatus = "ASSIGNED";
        this.slotNumber = slotNumber;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.assignedDate == null) {
            this.assignedDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
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

    public int getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    public String getAssignmentStatus() {
        return assignmentStatus;
    }

    public void setAssignmentStatus(String assignmentStatus) {
        this.assignmentStatus = assignmentStatus;
    }

    public String getStatus() {
        return assignmentStatus;
    }

    public void setStatus(String status) {
        this.assignmentStatus = status;
    }

    public LocalDate getAssignedDate() {
        return assignedDate;
    }

    public void setAssignedDate(LocalDate assignedDate) {
        this.assignedDate = assignedDate;
    }

    public LocalDate getReviewDeadline() {
        return reviewDeadline;
    }

    public void setReviewDeadline(LocalDate reviewDeadline) {
        this.reviewDeadline = reviewDeadline;
    }

    public boolean isStage2Enabled() {
        return stage2Enabled;
    }

    public void setStage2Enabled(boolean stage2Enabled) {
        this.stage2Enabled = stage2Enabled;
    }

    public boolean isRevisionForwarded() {
        return revisionForwarded;
    }

    public void setRevisionForwarded(boolean revisionForwarded) {
        this.revisionForwarded = revisionForwarded;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<Review> getReports() {
        return reports;
    }

    public void setReports(List<Review> reports) {
        this.reports = reports;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(Integer slotNumber) {
        this.slotNumber = slotNumber;
    }

    public LocalDateTime getReassignedDate() {
        return reassignedDate;
    }

    public void setReassignedDate(LocalDateTime reassignedDate) {
        this.reassignedDate = reassignedDate;
    }

    public String getReassignmentReason() {
        return reassignmentReason;
    }

    public void setReassignmentReason(String reassignmentReason) {
        this.reassignmentReason = reassignmentReason;
    }

    public User getSupersededByReviewer() {
        return supersededByReviewer;
    }

    public void setSupersededByReviewer(User supersededByReviewer) {
        this.supersededByReviewer = supersededByReviewer;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
