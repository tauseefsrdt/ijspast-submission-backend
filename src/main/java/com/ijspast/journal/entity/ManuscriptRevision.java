package com.ijspast.journal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "manuscript_revisions")
public class ManuscriptRevision {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manuscript_id", nullable = false)
    private Manuscript manuscript;

    @Column(name = "round_number", nullable = false)
    private int roundNumber = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revised_file_id")
    private ManuscriptFile revisedFile;

    @Column(name = "author_rebuttal_response", nullable = false, columnDefinition = "TEXT")
    private String authorRebuttalResponse;

    @Column(name = "submitted_at", updatable = false)
    private LocalDateTime submittedAt;

    public ManuscriptRevision() {
    }

    public ManuscriptRevision(String id, Manuscript manuscript, int roundNumber, ManuscriptFile revisedFile, String authorRebuttalResponse) {
        this.id = id;
        this.manuscript = manuscript;
        this.roundNumber = roundNumber;
        this.revisedFile = revisedFile;
        this.authorRebuttalResponse = authorRebuttalResponse;
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

    public int getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    public ManuscriptFile getRevisedFile() {
        return revisedFile;
    }

    public void setRevisedFile(ManuscriptFile revisedFile) {
        this.revisedFile = revisedFile;
    }

    public String getAuthorRebuttalResponse() {
        return authorRebuttalResponse;
    }

    public void setAuthorRebuttalResponse(String authorRebuttalResponse) {
        this.authorRebuttalResponse = authorRebuttalResponse;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
