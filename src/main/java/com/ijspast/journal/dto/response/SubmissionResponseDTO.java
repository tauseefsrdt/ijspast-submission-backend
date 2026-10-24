package com.ijspast.journal.dto.response;

public class SubmissionResponseDTO {

    private String manuscriptId;
    private GeneratedCredentials generatedCredentials;
    private DispatchedEmailDTO dispatchedEmail;
    private String authorName;
    private ManuscriptResponseDTO manuscript;
    private boolean isResubmission;
    private String message;

    public SubmissionResponseDTO() {
    }

    public SubmissionResponseDTO(String manuscriptId, GeneratedCredentials generatedCredentials, DispatchedEmailDTO dispatchedEmail, String authorName, ManuscriptResponseDTO manuscript) {
        this.manuscriptId = manuscriptId;
        this.generatedCredentials = generatedCredentials;
        this.dispatchedEmail = dispatchedEmail;
        this.authorName = authorName;
        this.manuscript = manuscript;
        this.isResubmission = false;
    }

    public SubmissionResponseDTO(String manuscriptId, GeneratedCredentials generatedCredentials, DispatchedEmailDTO dispatchedEmail, String authorName, ManuscriptResponseDTO manuscript, boolean isResubmission, String message) {
        this.manuscriptId = manuscriptId;
        this.generatedCredentials = generatedCredentials;
        this.dispatchedEmail = dispatchedEmail;
        this.authorName = authorName;
        this.manuscript = manuscript;
        this.isResubmission = isResubmission;
        this.message = message;
    }

    public static class GeneratedCredentials {
        private String email;
        private String password;

        public GeneratedCredentials() {
        }

        public GeneratedCredentials(String email, String password) {
            this.email = email;
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public String getManuscriptId() {
        return manuscriptId;
    }

    public void setManuscriptId(String manuscriptId) {
        this.manuscriptId = manuscriptId;
    }

    public GeneratedCredentials getGeneratedCredentials() {
        return generatedCredentials;
    }

    public void setGeneratedCredentials(GeneratedCredentials generatedCredentials) {
        this.generatedCredentials = generatedCredentials;
    }

    public DispatchedEmailDTO getDispatchedEmail() {
        return dispatchedEmail;
    }

    public void setDispatchedEmail(DispatchedEmailDTO dispatchedEmail) {
        this.dispatchedEmail = dispatchedEmail;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public ManuscriptResponseDTO getManuscript() {
        return manuscript;
    }

    public void setManuscript(ManuscriptResponseDTO manuscript) {
        this.manuscript = manuscript;
    }

    public boolean isResubmission() {
        return isResubmission;
    }

    public void setResubmission(boolean resubmission) {
        isResubmission = resubmission;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
