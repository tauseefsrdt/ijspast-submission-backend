package com.ijspast.journal.dto.response;

public class StatusHistoryItemDTO {

    private String id;
    private String status;
    private String date;
    private String actor;
    private String role;
    private String comment;

    public StatusHistoryItemDTO() {
    }

    public StatusHistoryItemDTO(String id, String status, String date, String actor, String role, String comment) {
        this.id = id;
        this.status = status;
        this.date = date;
        this.actor = actor;
        this.role = role;
        this.comment = comment;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
