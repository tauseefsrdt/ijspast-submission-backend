package com.ijspast.journal.dto.response;

public class CommunicationMessageDTO {

    private String id;
    private String senderId;
    private String senderName;
    private String senderRole; // "EDITOR" or "AUTHOR"
    private String message;
    private String sentAt;
    private String attachmentName;
    private String attachmentUrl;
    private Long attachmentSize;

    public CommunicationMessageDTO() {
    }

    public CommunicationMessageDTO(String id, String senderId, String senderName, String senderRole, String message, String sentAt) {
        this.id = id;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderRole = senderRole;
        this.message = message;
        this.sentAt = sentAt;
    }

    public CommunicationMessageDTO(String id, String senderId, String senderName, String senderRole, String message, String sentAt, String attachmentName, String attachmentUrl, Long attachmentSize) {
        this.id = id;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderRole = senderRole;
        this.message = message;
        this.sentAt = sentAt;
        this.attachmentName = attachmentName;
        this.attachmentUrl = attachmentUrl;
        this.attachmentSize = attachmentSize;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderRole() {
        return senderRole;
    }

    public void setSenderRole(String senderRole) {
        this.senderRole = senderRole;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSentAt() {
        return sentAt;
    }

    public void setSentAt(String sentAt) {
        this.sentAt = sentAt;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public void setAttachmentName(String attachmentName) {
        this.attachmentName = attachmentName;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public Long getAttachmentSize() {
        return attachmentSize;
    }

    public void setAttachmentSize(Long attachmentSize) {
        this.attachmentSize = attachmentSize;
    }
}
