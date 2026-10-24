package com.ijspast.journal.dto.request;

public class ForwardRevisionRequest {

    private int deadlineDays = 7;
    private String notes;

    public ForwardRevisionRequest() {
    }

    public ForwardRevisionRequest(int deadlineDays, String notes) {
        this.deadlineDays = deadlineDays;
        this.notes = notes;
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
