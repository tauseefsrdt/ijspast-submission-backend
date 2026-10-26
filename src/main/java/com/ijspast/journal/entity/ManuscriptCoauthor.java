package com.ijspast.journal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "manuscript_coauthors")
public class ManuscriptCoauthor {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manuscript_id", nullable = false)
    private Manuscript manuscript;

    @Column(nullable = false)
    private String name;

    @Column
    private String email;

    @Column
    private String institution;

    @Column(length = 64)
    private String orcid;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 1;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public ManuscriptCoauthor() {
    }

    public ManuscriptCoauthor(String id, Manuscript manuscript, String name, String email, String institution, String orcid, int displayOrder) {
        this.id = id;
        this.manuscript = manuscript;
        this.name = name;
        this.email = email;
        this.institution = institution;
        this.orcid = orcid;
        this.displayOrder = displayOrder;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getOrcid() {
        return orcid;
    }

    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
