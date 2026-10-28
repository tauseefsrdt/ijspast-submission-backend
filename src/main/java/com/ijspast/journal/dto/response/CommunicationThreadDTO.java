package com.ijspast.journal.dto.response;

import java.util.ArrayList;
import java.util.List;

public class CommunicationThreadDTO {

    private String manuscriptId;
    private int exchangesUsed = 0;
    private int maxExchanges = 5;
    private boolean canSendMessage = false;
    private boolean isPostReviewStage = false;
    private String statusMessage;
    private List<CommunicationMessageDTO> messages = new ArrayList<>();

    public CommunicationThreadDTO() {
    }

    public CommunicationThreadDTO(String manuscriptId, int exchangesUsed, int maxExchanges, boolean canSendMessage, boolean isPostReviewStage, String statusMessage, List<CommunicationMessageDTO> messages) {
        this.manuscriptId = manuscriptId;
        this.exchangesUsed = exchangesUsed;
        this.maxExchanges = maxExchanges;
        this.canSendMessage = canSendMessage;
        this.isPostReviewStage = isPostReviewStage;
        this.statusMessage = statusMessage;
        this.messages = messages;
    }

    public CommunicationThreadDTO(String manuscriptId, int exchangesUsed, int maxExchanges, boolean canSendMessage, boolean isPostReviewStage, List<CommunicationMessageDTO> messages) {
        this.manuscriptId = manuscriptId;
        this.exchangesUsed = exchangesUsed;
        this.maxExchanges = maxExchanges;
        this.canSendMessage = canSendMessage;
        this.isPostReviewStage = isPostReviewStage;
        this.statusMessage = (exchangesUsed >= maxExchanges) ? "The maximum of five communication exchanges has been reached." : null;
        this.messages = messages;
    }

    public String getManuscriptId() {
        return manuscriptId;
    }

    public void setManuscriptId(String manuscriptId) {
        this.manuscriptId = manuscriptId;
    }

    public int getExchangesUsed() {
        return exchangesUsed;
    }

    public void setExchangesUsed(int exchangesUsed) {
        this.exchangesUsed = exchangesUsed;
    }

    public int getMaxExchanges() {
        return maxExchanges;
    }

    public void setMaxExchanges(int maxExchanges) {
        this.maxExchanges = maxExchanges;
    }

    public boolean isCanSendMessage() {
        return canSendMessage;
    }

    public void setCanSendMessage(boolean canSendMessage) {
        this.canSendMessage = canSendMessage;
    }

    public boolean isPostReviewStage() {
        return isPostReviewStage;
    }

    public void setPostReviewStage(boolean postReviewStage) {
        isPostReviewStage = postReviewStage;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public List<CommunicationMessageDTO> getMessages() {
        return messages;
    }

    public void setMessages(List<CommunicationMessageDTO> messages) {
        this.messages = messages;
    }
}
