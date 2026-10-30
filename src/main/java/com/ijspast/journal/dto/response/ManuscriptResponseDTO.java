package com.ijspast.journal.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class ManuscriptResponseDTO {

    private String id;
    private String title;
    private String domain;

    @JsonProperty("abstract")
    private String abstractText;

    private List<String> keywords = new ArrayList<>();
    private AuthorInfoDTO author;
    private String coauthors;
    private UploadedFileDTO file;
    private List<UploadedFileDTO> revisedFiles = new ArrayList<>();
    private List<ManuscriptRevisionDTO> revisions = new ArrayList<>();
    private String status;
    private String submissionDate;
    private String updatedAt;
    private Integer similarityScore;
    private String assignedReviewerId;
    private String assignedReviewerName;
    private String assignedReviewer2Id;
    private String assignedReviewer2Name;
    private String assignedDate;
    private String reviewDeadline;
    private String revisionRequestComments;
    private String authorRevisionResponse;
    private Boolean revisionForwardedToReviewer = false;
    private Boolean stage2EnabledByEditor = false;
    private Boolean reviewerReassignedAfterRevision = false;

    private String assignedReviewer1Date;
    private String assignedReviewer1Deadline;
    private String reviewer1Status; // "Pending", "Overdue", "Completed"
    private Long reviewer1DaysRemaining;
    private Boolean reviewer1CanReassign = false;
    private Integer reviewer1SubmissionCount = 0;
    private Boolean reviewer1RoleEnded = false;

    private String assignedReviewer2Date;
    private String assignedReviewer2Deadline;
    private String reviewer2Status;
    private Long reviewer2DaysRemaining;
    private Boolean reviewer2CanReassign = false;
    private Integer reviewer2SubmissionCount = 0;
    private Boolean reviewer2RoleEnded = false;

    private String editorComments;
    private String feedbackProgress; // "0/2 Ready", "1/2 Ready", "2/2 Ready"
    private Integer feedbackReadyCount = 0;
    private Boolean canAccept = false;
    private Boolean canReject = true;
    private Integer communicationExchanges = 0;
    private Boolean canCommunicate = false;

    private String reviewerFeedbackForAuthor;
    private String reviewerFeedbackForEditor;
    private String rejectionReason;
    private PublicationDetailsDTO publicationDetails;
    private List<StatusHistoryItemDTO> history = new ArrayList<>();
    private Integer revisionCount = 0;

    public ManuscriptResponseDTO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public AuthorInfoDTO getAuthor() {
        return author;
    }

    public void setAuthor(AuthorInfoDTO author) {
        this.author = author;
    }

    public String getCoauthors() {
        return coauthors;
    }

    public void setCoauthors(String coauthors) {
        this.coauthors = coauthors;
    }

    public UploadedFileDTO getFile() {
        return file;
    }

    public void setFile(UploadedFileDTO file) {
        this.file = file;
    }

    public List<UploadedFileDTO> getRevisedFiles() {
        return revisedFiles;
    }

    public void setRevisedFiles(List<UploadedFileDTO> revisedFiles) {
        this.revisedFiles = revisedFiles;
    }

    public List<ManuscriptRevisionDTO> getRevisions() {
        return revisions;
    }

    public void setRevisions(List<ManuscriptRevisionDTO> revisions) {
        this.revisions = revisions;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(String submissionDate) {
        this.submissionDate = submissionDate;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(Integer similarityScore) {
        this.similarityScore = similarityScore;
    }

    public String getAssignedReviewerId() {
        return assignedReviewerId;
    }

    public void setAssignedReviewerId(String assignedReviewerId) {
        this.assignedReviewerId = assignedReviewerId;
    }

    public String getAssignedReviewerName() {
        return assignedReviewerName;
    }

    public void setAssignedReviewerName(String assignedReviewerName) {
        this.assignedReviewerName = assignedReviewerName;
    }

    public String getAssignedReviewer2Id() {
        return assignedReviewer2Id;
    }

    public void setAssignedReviewer2Id(String assignedReviewer2Id) {
        this.assignedReviewer2Id = assignedReviewer2Id;
    }

    public String getAssignedReviewer2Name() {
        return assignedReviewer2Name;
    }

    public void setAssignedReviewer2Name(String assignedReviewer2Name) {
        this.assignedReviewer2Name = assignedReviewer2Name;
    }

    public String getAssignedDate() {
        return assignedDate;
    }

    public void setAssignedDate(String assignedDate) {
        this.assignedDate = assignedDate;
    }

    public String getReviewDeadline() {
        return reviewDeadline;
    }

    public void setReviewDeadline(String reviewDeadline) {
        this.reviewDeadline = reviewDeadline;
    }

    public String getRevisionRequestComments() {
        return revisionRequestComments;
    }

    public void setRevisionRequestComments(String revisionRequestComments) {
        this.revisionRequestComments = revisionRequestComments;
    }

    public String getAuthorRevisionResponse() {
        return authorRevisionResponse;
    }

    public void setAuthorRevisionResponse(String authorRevisionResponse) {
        this.authorRevisionResponse = authorRevisionResponse;
    }

    public Boolean getRevisionForwardedToReviewer() {
        return revisionForwardedToReviewer != null && revisionForwardedToReviewer;
    }

    public void setRevisionForwardedToReviewer(Boolean revisionForwardedToReviewer) {
        this.revisionForwardedToReviewer = revisionForwardedToReviewer;
    }

    public Boolean getStage2EnabledByEditor() {
        return stage2EnabledByEditor != null && stage2EnabledByEditor;
    }

    public void setStage2EnabledByEditor(Boolean stage2EnabledByEditor) {
        this.stage2EnabledByEditor = stage2EnabledByEditor;
    }

    public Boolean getReviewerReassignedAfterRevision() {
        return reviewerReassignedAfterRevision != null && reviewerReassignedAfterRevision;
    }

    public void setReviewerReassignedAfterRevision(Boolean reviewerReassignedAfterRevision) {
        this.reviewerReassignedAfterRevision = reviewerReassignedAfterRevision;
    }

    public String getReviewerFeedbackForAuthor() {
        return reviewerFeedbackForAuthor;
    }

    public void setReviewerFeedbackForAuthor(String reviewerFeedbackForAuthor) {
        this.reviewerFeedbackForAuthor = reviewerFeedbackForAuthor;
    }

    public String getReviewerFeedbackForEditor() {
        return reviewerFeedbackForEditor;
    }

    public void setReviewerFeedbackForEditor(String reviewerFeedbackForEditor) {
        this.reviewerFeedbackForEditor = reviewerFeedbackForEditor;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public PublicationDetailsDTO getPublicationDetails() {
        return publicationDetails;
    }

    public void setPublicationDetails(PublicationDetailsDTO publicationDetails) {
        this.publicationDetails = publicationDetails;
    }

    public List<StatusHistoryItemDTO> getHistory() {
        return history;
    }

    public void setHistory(List<StatusHistoryItemDTO> history) {
        this.history = history;
    }

    public Integer getRevisionCount() {
        return revisionCount != null ? revisionCount : 0;
    }

    public void setRevisionCount(Integer revisionCount) {
        this.revisionCount = revisionCount;
    }

    public String getAssignedReviewer1Date() {
        return assignedReviewer1Date;
    }

    public void setAssignedReviewer1Date(String assignedReviewer1Date) {
        this.assignedReviewer1Date = assignedReviewer1Date;
    }

    public String getAssignedReviewer1Deadline() {
        return assignedReviewer1Deadline;
    }

    public void setAssignedReviewer1Deadline(String assignedReviewer1Deadline) {
        this.assignedReviewer1Deadline = assignedReviewer1Deadline;
    }

    public String getReviewer1Status() {
        return reviewer1Status;
    }

    public void setReviewer1Status(String reviewer1Status) {
        this.reviewer1Status = reviewer1Status;
    }

    public Long getReviewer1DaysRemaining() {
        return reviewer1DaysRemaining;
    }

    public void setReviewer1DaysRemaining(Long reviewer1DaysRemaining) {
        this.reviewer1DaysRemaining = reviewer1DaysRemaining;
    }

    public Boolean getReviewer1CanReassign() {
        return reviewer1CanReassign;
    }

    public void setReviewer1CanReassign(Boolean reviewer1CanReassign) {
        this.reviewer1CanReassign = reviewer1CanReassign;
    }

    public Integer getReviewer1SubmissionCount() {
        return reviewer1SubmissionCount;
    }

    public void setReviewer1SubmissionCount(Integer reviewer1SubmissionCount) {
        this.reviewer1SubmissionCount = reviewer1SubmissionCount;
    }

    public Boolean getReviewer1RoleEnded() {
        return reviewer1RoleEnded;
    }

    public void setReviewer1RoleEnded(Boolean reviewer1RoleEnded) {
        this.reviewer1RoleEnded = reviewer1RoleEnded;
    }

    public String getAssignedReviewer2Date() {
        return assignedReviewer2Date;
    }

    public void setAssignedReviewer2Date(String assignedReviewer2Date) {
        this.assignedReviewer2Date = assignedReviewer2Date;
    }

    public String getAssignedReviewer2Deadline() {
        return assignedReviewer2Deadline;
    }

    public void setAssignedReviewer2Deadline(String assignedReviewer2Deadline) {
        this.assignedReviewer2Deadline = assignedReviewer2Deadline;
    }

    public String getReviewer2Status() {
        return reviewer2Status;
    }

    public void setReviewer2Status(String reviewer2Status) {
        this.reviewer2Status = reviewer2Status;
    }

    public Long getReviewer2DaysRemaining() {
        return reviewer2DaysRemaining;
    }

    public void setReviewer2DaysRemaining(Long reviewer2DaysRemaining) {
        this.reviewer2DaysRemaining = reviewer2DaysRemaining;
    }

    public Boolean getReviewer2CanReassign() {
        return reviewer2CanReassign;
    }

    public void setReviewer2CanReassign(Boolean reviewer2CanReassign) {
        this.reviewer2CanReassign = reviewer2CanReassign;
    }

    public Integer getReviewer2SubmissionCount() {
        return reviewer2SubmissionCount;
    }

    public void setReviewer2SubmissionCount(Integer reviewer2SubmissionCount) {
        this.reviewer2SubmissionCount = reviewer2SubmissionCount;
    }

    public Boolean getReviewer2RoleEnded() {
        return reviewer2RoleEnded;
    }

    public void setReviewer2RoleEnded(Boolean reviewer2RoleEnded) {
        this.reviewer2RoleEnded = reviewer2RoleEnded;
    }

    public String getEditorComments() {
        return editorComments;
    }

    public void setEditorComments(String editorComments) {
        this.editorComments = editorComments;
    }

    public String getFeedbackProgress() {
        return feedbackProgress;
    }

    public void setFeedbackProgress(String feedbackProgress) {
        this.feedbackProgress = feedbackProgress;
    }

    public Integer getFeedbackReadyCount() {
        return feedbackReadyCount;
    }

    public void setFeedbackReadyCount(Integer feedbackReadyCount) {
        this.feedbackReadyCount = feedbackReadyCount;
    }

    public Boolean getCanAccept() {
        return canAccept;
    }

    public void setCanAccept(Boolean canAccept) {
        this.canAccept = canAccept;
    }

    public Boolean getCanReject() {
        return canReject;
    }

    public void setCanReject(Boolean canReject) {
        this.canReject = canReject;
    }

    public Integer getCommunicationExchanges() {
        return communicationExchanges;
    }

    public void setCommunicationExchanges(Integer communicationExchanges) {
        this.communicationExchanges = communicationExchanges;
    }

    public Boolean getCanCommunicate() {
        return canCommunicate;
    }

    public void setCanCommunicate(Boolean canCommunicate) {
        this.canCommunicate = canCommunicate;
    }
}
