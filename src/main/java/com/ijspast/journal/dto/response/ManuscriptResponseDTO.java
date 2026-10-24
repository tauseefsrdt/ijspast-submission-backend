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
    private String status;
    private String submissionDate;
    private String updatedAt;
    private Integer similarityScore;
    private String assignedReviewerId;
    private String assignedReviewerName;
    private String assignedDate;
    private String reviewDeadline;
    private String revisionRequestComments;
    private String authorRevisionResponse;
    private String reviewerFeedbackForAuthor;
    private String reviewerFeedbackForEditor;
    private String rejectionReason;
    private PublicationDetailsDTO publicationDetails;
    private List<StatusHistoryItemDTO> history = new ArrayList<>();

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
}
