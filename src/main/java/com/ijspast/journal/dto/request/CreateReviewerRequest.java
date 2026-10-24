package com.ijspast.journal.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class CreateReviewerRequest {

    @NotBlank(message = "Reviewer full name is required")
    private String name;

    @NotBlank(message = "Reviewer email is required")
    @Email(message = "Valid email address is required")
    private String email;

    private String phone;

    @NotBlank(message = "Institution/University is required")
    private String institution;

    @NotBlank(message = "Domain of expertise is required")
    private String domain;

    public CreateReviewerRequest() {
    }

    public CreateReviewerRequest(String name, String email, String phone, String institution, String domain) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.institution = institution;
        this.domain = domain;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}
