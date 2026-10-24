package com.ijspast.journal.dto.request;

import jakarta.validation.constraints.NotBlank;

public class RevisionSubmissionRequest {

    @NotBlank(message = "Author revision remarks and response are required")
    private String response;

    private FileMetadataRequest file;

    public RevisionSubmissionRequest() {
    }

    public RevisionSubmissionRequest(String response, FileMetadataRequest file) {
        this.response = response;
        this.file = file;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public FileMetadataRequest getFile() {
        return file;
    }

    public void setFile(FileMetadataRequest file) {
        this.file = file;
    }
}
