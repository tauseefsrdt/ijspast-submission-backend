package com.ijspast.journal.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class ReassignReviewerRequest {

    @Min(value = 1, message = "Slot must be 1 or 2")
    @Max(value = 2, message = "Slot must be 1 or 2")
    private int slot = 1;

    @NotBlank(message = "Replacement reviewer ID is required")
    private String replacementReviewerId;

    private String reason;

    public ReassignReviewerRequest() {
    }

    public ReassignReviewerRequest(int slot, String replacementReviewerId, String reason) {
        this.slot = slot;
        this.replacementReviewerId = replacementReviewerId;
        this.reason = reason;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public String getReplacementReviewerId() {
        return replacementReviewerId;
    }

    public void setReplacementReviewerId(String replacementReviewerId) {
        this.replacementReviewerId = replacementReviewerId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
