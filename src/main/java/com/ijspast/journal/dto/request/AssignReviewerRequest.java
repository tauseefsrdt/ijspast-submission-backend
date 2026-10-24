package com.ijspast.journal.dto.request;

import jakarta.validation.constraints.NotBlank;

public class AssignReviewerRequest {

    @NotBlank(message = "Reviewer ID is required")
    private String reviewerId;

    private String reviewerName;

    private int deadlineDays = 14;

    private String notes;

    public AssignReviewerRequest() {
    }

    public AssignReviewerRequest(String reviewerId, String reviewerName, int deadlineDays) {
        this.reviewerId = reviewerId;
        this.reviewerName = reviewerName;
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
