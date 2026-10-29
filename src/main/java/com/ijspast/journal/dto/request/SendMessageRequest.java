package com.ijspast.journal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SendMessageRequest {

    @NotBlank(message = "Message content cannot be blank")
    @Size(max = 5000, message = "Message content cannot exceed 5000 characters")
    private String message;

    private String attachmentName;
    private String attachmentUrl;
    private Long attachmentSize;

    public SendMessageRequest() {
    }

    public SendMessageRequest(String message) {
        this.message = message;
    }

    public SendMessageRequest(String message, String attachmentName, String attachmentUrl, Long attachmentSize) {
        this.message = message;
        this.attachmentName = attachmentName;
        this.attachmentUrl = attachmentUrl;
        this.attachmentSize = attachmentSize;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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
