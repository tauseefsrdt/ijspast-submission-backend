package com.ijspast.journal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "editorial_decisions")
public class EditorialDecision {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manuscript_id", nullable = false)
    private Manuscript manuscript;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "editor_user_id", nullable = false)
    private User editor;

    @Column(name = "round_number", nullable = false)
    private int roundNumber = 1;

    @Column(name = "decision_type", nullable = false, length = 32)
    private String decisionType; // REQUEST_REVISION, ACCEPT, REJECT, PUBLISH

    @Column(name = "consolidated_comments", columnDefinition = "TEXT")
    private String consolidatedComments;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "decided_at", updatable = false)
    private LocalDateTime decidedAt;

    public EditorialDecision() {
    }

    public EditorialDecision(String id, Manuscript manuscript, User editor, int roundNumber, String decisionType, String consolidatedComments, String rejectionReason) {
        this.id = id;
        this.manuscript = manuscript;
        this.editor = editor;
        this.roundNumber = roundNumber;
        this.decisionType = decisionType;
        this.consolidatedComments = consolidatedComments;
        this.rejectionReason = rejectionReason;
    }

    @PrePersist
    protected void onCreate() {
        if (this.decidedAt == null) {
            this.decidedAt = LocalDateTime.now();
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

    public User getEditor() {
        return editor;
    }

    public void setEditor(User editor) {
        this.editor = editor;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    public String getDecisionType() {
        return decisionType;
    }

    public void setDecisionType(String decisionType) {
        this.decisionType = decisionType;
    }

    public String getConsolidatedComments() {
        return consolidatedComments;
    }

    public void setConsolidatedComments(String consolidatedComments) {
        this.consolidatedComments = consolidatedComments;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }
}
