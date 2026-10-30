package com.ijspast.journal.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthorRegistrationRequest {

    @NotBlank(message = "Author name is required")
    private String name;

    @NotBlank(message = "Author email is required")
    @Email(message = "Author email must be valid")
    private String email;

    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Institution is required")
    private String institution;

    private String department;

    @NotBlank(message = "City and country is required")
    private String cityCountry;

    @NotBlank(message = "ORCID ID is required")
    private String orcid;

    public AuthorRegistrationRequest() {
    }

    public AuthorRegistrationRequest(String name, String email, String phone, String password, String institution, String department, String cityCountry, String orcid) {
        this.name = name;
        this.email = email != null ? email.trim() : null;
        this.phone = phone != null ? phone.trim() : null;
        this.password = password;
        this.institution = institution;
        this.department = department;
        this.cityCountry = cityCountry;
        this.orcid = orcid;
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
        this.email = email != null ? email.trim() : null;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone != null ? phone.trim() : null;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getCityCountry() {
        return cityCountry;
    }

    public void setCityCountry(String cityCountry) {
        this.cityCountry = cityCountry;
    }

    public String getOrcid() {
        return orcid;
    }

    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }
}
