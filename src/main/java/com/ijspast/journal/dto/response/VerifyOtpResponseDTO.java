package com.ijspast.journal.dto.response;

public class VerifyOtpResponseDTO {

    private String resetToken;
    private String email;
    private String message;

    public VerifyOtpResponseDTO() {
    }

    public VerifyOtpResponseDTO(String resetToken, String email, String message) {
        this.resetToken = resetToken;
        this.email = email;
        this.message = message;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
