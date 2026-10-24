package com.ijspast.journal.dto.request;

import jakarta.validation.constraints.NotBlank;

public class AssignReviewerRequest {

    @NotBlank(message = "Reviewer ID is required")
    private String reviewerId;

    private String reviewerName;

    private String reviewer2Id;

    private String reviewer2Name;

    private int deadlineDays = 14;

    private String notes;

    public AssignReviewerRequest() {
    }

    public AssignReviewerRequest(String reviewerId, String reviewerName, int deadlineDays) {
        this.reviewerId = reviewerId;
        this.reviewerName = reviewerName;
        this.deadlineDays = deadlineDays;
    }

    public AssignReviewerRequest(String reviewerId, String reviewerName, String reviewer2Id, String reviewer2Name, int deadlineDays) {
        this.reviewerId = reviewerId;
        this.reviewerName = reviewerName;
        this.reviewer2Id = reviewer2Id;
        this.reviewer2Name = reviewer2Name;
        this.deadlineDays = deadlineDays;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public String getReviewer2Id() {
        return reviewer2Id;
    }

    public void setReviewer2Id(String reviewer2Id) {
        this.reviewer2Id = reviewer2Id;
    }

    public String getReviewer2Name() {
        return reviewer2Name;
    }

    public void setReviewer2Name(String reviewer2Name) {
        this.reviewer2Name = reviewer2Name;
    }

    public int getDeadlineDays() {
        return deadlineDays;
    }

    public void setDeadlineDays(int deadlineDays) {
        this.deadlineDays = deadlineDays;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
