package com.ijspast.journal.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthorInfoRequest {

    @NotBlank(message = "Author name is required")
    private String name;

    @NotBlank(message = "Author email is required")
    @Email(message = "Author email must be valid")
    private String email;

    @NotBlank(message = "Author institution is required")
    private String institution;

    private String department;

    @NotBlank(message = "City and country are required")
    private String cityCountry;

    @NotBlank(message = "ORCID ID is required to submit a manuscript (e.g. 0000-0002-1825-0097)")
    private String orcid;

    private String phone;

    public AuthorInfoRequest() {
    }

    public AuthorInfoRequest(String name, String email, String institution, String department, String cityCountry, String orcid) {
        this(name, email, institution, department, cityCountry, orcid, null);
    }

    public AuthorInfoRequest(String name, String email, String institution, String department, String cityCountry, String orcid, String phone) {
        this.name = name;
        this.email = email != null ? email.trim() : null;
        this.institution = institution;
        this.department = department;
        this.cityCountry = cityCountry;
        this.orcid = orcid;
        this.phone = phone != null ? phone.trim() : null;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone != null ? phone.trim() : null;
    }
}
