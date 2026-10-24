package com.ijspast.journal.dto.response;

public class DispatchedEmailDTO {

    private String id;
    private String to;
    private String toName;
    private String subject;
    private String type;
    private String manuscriptId;
    private String content;
    private String sentAt;
    private String status;

    public DispatchedEmailDTO() {
    }

    public DispatchedEmailDTO(String id, String to, String toName, String subject, String type, String manuscriptId, String content, String sentAt, String status) {
        this.id = id;
        this.to = to;
        this.toName = toName;
        this.subject = subject;
        this.type = type;
        this.manuscriptId = manuscriptId;
        this.content = content;
        this.sentAt = sentAt;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getToName() {
        return toName;
    }

    public void setToName(String toName) {
        this.toName = toName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getManuscriptId() {
        return manuscriptId;
    }

    public void setManuscriptId(String manuscriptId) {
        this.manuscriptId = manuscriptId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSentAt() {
        return sentAt;
    }

    public void setSentAt(String sentAt) {
        this.sentAt = sentAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
