package com.ijspast.journal.dto.response;

public class AuthorInfoDTO {

    private String name;
    private String email;
    private String institution;
    private String department;
    private String cityCountry;
    private String orcid;

    public AuthorInfoDTO() {
    }

    public AuthorInfoDTO(String name, String email, String institution, String department, String cityCountry, String orcid) {
        this.name = name;
        this.email = email;
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
        this.email = email;
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
