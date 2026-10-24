package com.ijspast.journal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reviewers")
public class Reviewer {

    @Id
    @Column(length = 64)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String institution;

    @Column
    private String department;

    @Column(nullable = false)
    private String domain;

    @Column(length = 64)
    private String phone;

    @Column(name = "active_assignments_count")
    private int activeAssignmentsCount = 0;

    @Column(name = "completed_reviews_count")
    private int completedReviewsCount = 0;

    @OneToMany(mappedBy = "assignedReviewer1Ref", fetch = FetchType.LAZY)
    private List<Manuscript> manuscriptsAsReviewer1 = new ArrayList<>();

    @OneToMany(mappedBy = "assignedReviewer2Ref", fetch = FetchType.LAZY)
    private List<Manuscript> manuscriptsAsReviewer2 = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Reviewer() {
    }

    public Reviewer(String id, String name, String email, String institution, String domain) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.institution = institution;
        this.domain = domain;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public List<Manuscript> getAllAssignedManuscripts() {
        List<Manuscript> all = new ArrayList<>();
        if (manuscriptsAsReviewer1 != null) all.addAll(manuscriptsAsReviewer1);
        if (manuscriptsAsReviewer2 != null) all.addAll(manuscriptsAsReviewer2);
        return all;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getActiveAssignmentsCount() {
        return activeAssignmentsCount;
    }

    public void setActiveAssignmentsCount(int activeAssignmentsCount) {
        this.activeAssignmentsCount = activeAssignmentsCount;
    }

    public int getCompletedReviewsCount() {
        return completedReviewsCount;
    }

    public void setCompletedReviewsCount(int completedReviewsCount) {
        this.completedReviewsCount = completedReviewsCount;
    }

    public List<Manuscript> getManuscriptsAsReviewer1() {
        return manuscriptsAsReviewer1;
    }

    public void setManuscriptsAsReviewer1(List<Manuscript> manuscriptsAsReviewer1) {
        this.manuscriptsAsReviewer1 = manuscriptsAsReviewer1;
    }

    public List<Manuscript> getManuscriptsAsReviewer2() {
        return manuscriptsAsReviewer2;
    }

    public void setManuscriptsAsReviewer2(List<Manuscript> manuscriptsAsReviewer2) {
        this.manuscriptsAsReviewer2 = manuscriptsAsReviewer2;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
