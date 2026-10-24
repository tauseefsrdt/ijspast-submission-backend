package com.ijspast.journal.dto.response;

import java.util.ArrayList;
import java.util.List;

public class AuthorTableDTO {
    private String id;
    private String name;
    private String email;
    private String institution;
    private String department;
    private String cityCountry;
    private String orcid;
    private String phone;
    private int totalPapers;
    private List<AuthorPaperItemDTO> papers = new ArrayList<>();

    public AuthorTableDTO() {}

    public static class AuthorPaperItemDTO {
        private String id;
        private String title;
        private String domain;
        private String status;
        private String submissionDate;
        private ReviewerBriefDTO reviewer1;
        private ReviewerBriefDTO reviewer2;

        public AuthorPaperItemDTO() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getSubmissionDate() { return submissionDate; }
        public void setSubmissionDate(String submissionDate) { this.submissionDate = submissionDate; }
        public ReviewerBriefDTO getReviewer1() { return reviewer1; }
        public void setReviewer1(ReviewerBriefDTO reviewer1) { this.reviewer1 = reviewer1; }
        public ReviewerBriefDTO getReviewer2() { return reviewer2; }
        public void setReviewer2(ReviewerBriefDTO reviewer2) { this.reviewer2 = reviewer2; }
    }

    public static class ReviewerBriefDTO {
        private String id;
        private String name;
        private String email;
        private String institution;
        private String domain;
        private boolean stage1Submitted;
        private boolean stage2Submitted;

        public ReviewerBriefDTO() {}

        public ReviewerBriefDTO(String id, String name, String email, String institution, String domain) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.institution = institution;
            this.domain = domain;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getInstitution() { return institution; }
        public void setInstitution(String institution) { this.institution = institution; }
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        public boolean isStage1Submitted() { return stage1Submitted; }
        public void setStage1Submitted(boolean stage1Submitted) { this.stage1Submitted = stage1Submitted; }
        public boolean isStage2Submitted() { return stage2Submitted; }
        public void setStage2Submitted(boolean stage2Submitted) { this.stage2Submitted = stage2Submitted; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getCityCountry() { return cityCountry; }
    public void setCityCountry(String cityCountry) { this.cityCountry = cityCountry; }
    public String getOrcid() { return orcid; }
    public void setOrcid(String orcid) { this.orcid = orcid; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public int getTotalPapers() { return totalPapers; }
    public void setTotalPapers(int totalPapers) { this.totalPapers = totalPapers; }
    public List<AuthorPaperItemDTO> getPapers() { return papers; }
    public void setPapers(List<AuthorPaperItemDTO> papers) { this.papers = papers; }
}
