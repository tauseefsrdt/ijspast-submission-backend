package com.ijspast.journal.dto.response;

public class ManuscriptRevisionDTO {

    private String id;
    private int roundNumber;
    private UploadedFileDTO file;
    private String authorRebuttalResponse;
    private String submittedAt;

    public ManuscriptRevisionDTO() {
    }

    public ManuscriptRevisionDTO(String id, int roundNumber, UploadedFileDTO file, String authorRebuttalResponse, String submittedAt) {
        this.id = id;
        this.roundNumber = roundNumber;
        this.file = file;
        this.authorRebuttalResponse = authorRebuttalResponse;
        this.submittedAt = submittedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    public UploadedFileDTO getFile() {
        return file;
    }

    public void setFile(UploadedFileDTO file) {
        this.file = file;
    }

    public String getAuthorRebuttalResponse() {
        return authorRebuttalResponse;
    }

    public void setAuthorRebuttalResponse(String authorRebuttalResponse) {
        this.authorRebuttalResponse = authorRebuttalResponse;
    }

    public String getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(String submittedAt) {
        this.submittedAt = submittedAt;
    }
}

