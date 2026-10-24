package com.ijspast.journal.dto.response;

public class ReviewerProfileDTO {

    private String id;
    private String name;
    private String email;
    private String phone;
    private String institution;
    private String domain;
    private long activeAssignmentsCount;
    private long totalCompletedReviews;

    public ReviewerProfileDTO() {
    }

    public ReviewerProfileDTO(String id, String name, String email, String phone, String institution, String domain, long activeAssignmentsCount, long totalCompletedReviews) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.institution = institution;
        this.domain = domain;
        this.activeAssignmentsCount = activeAssignmentsCount;
        this.totalCompletedReviews = totalCompletedReviews;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public long getActiveAssignmentsCount() {
        return activeAssignmentsCount;
    }

    public void setActiveAssignmentsCount(long activeAssignmentsCount) {
        this.activeAssignmentsCount = activeAssignmentsCount;
    }

    public long getTotalCompletedReviews() {
        return totalCompletedReviews;
    }

    public void setTotalCompletedReviews(long totalCompletedReviews) {
        this.totalCompletedReviews = totalCompletedReviews;
    }
}
