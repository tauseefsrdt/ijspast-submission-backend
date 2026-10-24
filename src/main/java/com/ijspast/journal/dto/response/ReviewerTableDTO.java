package com.ijspast.journal.dto.response;

import java.util.ArrayList;
import java.util.List;

public class ReviewerTableDTO {
    private String id;
    private String name;
    private String email;
    private String institution;
    private String department;
    private String domain;
    private String phone;
    private int activeAssignmentsCount;
    private int completedReviewsCount;
    private int totalAssignedPapers;
    private List<ReviewerPaperAssignmentDTO> assignedPapers = new ArrayList<>();

    public ReviewerTableDTO() {}

    public static class ReviewerPaperAssignmentDTO {
        private String paperId;
        private String paperTitle;
        private String domain;
        private String status;
        private String assignedRole; // "Reviewer 1" or "Reviewer 2"
        private String assignedDate;
        private String reviewDeadline;
        private boolean stage1Completed;
        private boolean stage2Completed;
        
        // Assigned Author details
        private AuthorBriefDTO author;

        // Co-Reviewer details (the other assigned reviewer)
        private CoReviewerBriefDTO coReviewer;

        public ReviewerPaperAssignmentDTO() {}

        public String getPaperId() { return paperId; }
        public void setPaperId(String paperId) { this.paperId = paperId; }
        public String getPaperTitle() { return paperTitle; }
        public void setPaperTitle(String paperTitle) { this.paperTitle = paperTitle; }
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getAssignedRole() { return assignedRole; }
        public void setAssignedRole(String assignedRole) { this.assignedRole = assignedRole; }
        public String getAssignedDate() { return assignedDate; }
        public void setAssignedDate(String assignedDate) { this.assignedDate = assignedDate; }
        public String getReviewDeadline() { return reviewDeadline; }
        public void setReviewDeadline(String reviewDeadline) { this.reviewDeadline = reviewDeadline; }
        public boolean isStage1Completed() { return stage1Completed; }
        public void setStage1Completed(boolean stage1Completed) { this.stage1Completed = stage1Completed; }
        public boolean isStage2Completed() { return stage2Completed; }
        public void setStage2Completed(boolean stage2Completed) { this.stage2Completed = stage2Completed; }
        public AuthorBriefDTO getAuthor() { return author; }
        public void setAuthor(AuthorBriefDTO author) { this.author = author; }
        public CoReviewerBriefDTO getCoReviewer() { return coReviewer; }
        public void setCoReviewer(CoReviewerBriefDTO coReviewer) { this.coReviewer = coReviewer; }
    }

    public static class AuthorBriefDTO {
        private String id;
        private String name;
        private String email;
        private String institution;
        private String cityCountry;
        private String orcid;

        public AuthorBriefDTO() {}

        public AuthorBriefDTO(String id, String name, String email, String institution, String cityCountry, String orcid) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.institution = institution;
            this.cityCountry = cityCountry;
            this.orcid = orcid;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getInstitution() { return institution; }
        public void setInstitution(String institution) { this.institution = institution; }
        public String getCityCountry() { return cityCountry; }
        public void setCityCountry(String cityCountry) { this.cityCountry = cityCountry; }
        public String getOrcid() { return orcid; }
        public void setOrcid(String orcid) { this.orcid = orcid; }
    }

    public static class CoReviewerBriefDTO {
        private String id;
        private String name;
        private String email;
        private String institution;

        public CoReviewerBriefDTO() {}

        public CoReviewerBriefDTO(String id, String name, String email, String institution) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.institution = institution;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getInstitution() { return institution; }
        public void setInstitution(String institution) { this.institution = institution; }
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
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public int getActiveAssignmentsCount() { return activeAssignmentsCount; }
    public void setActiveAssignmentsCount(int activeAssignmentsCount) { this.activeAssignmentsCount = activeAssignmentsCount; }
    public int getCompletedReviewsCount() { return completedReviewsCount; }
    public void setCompletedReviewsCount(int completedReviewsCount) { this.completedReviewsCount = completedReviewsCount; }
    public int getTotalAssignedPapers() { return totalAssignedPapers; }
    public void setTotalAssignedPapers(int totalAssignedPapers) { this.totalAssignedPapers = totalAssignedPapers; }
    public List<ReviewerPaperAssignmentDTO> getAssignedPapers() { return assignedPapers; }
    public void setAssignedPapers(List<ReviewerPaperAssignmentDTO> assignedPapers) { this.assignedPapers = assignedPapers; }
}
