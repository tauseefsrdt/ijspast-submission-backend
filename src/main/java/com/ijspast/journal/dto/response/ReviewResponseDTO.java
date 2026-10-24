package com.ijspast.journal.dto.response;

public class ReviewResponseDTO {

    private String id;
    private String manuscriptId;
    private String reviewerId;
    private String reviewerName;
    private String recommendation;
    private int technicalQuality;
    private int originality;
    private int clarity;
    private String commentsForEditor;
    private String commentsForAuthor;
    private String confidentialComments;
    private int stage = 1;
    private String stageName;
    private java.util.List<Integer> rubricScores;
    private Double overallScore;
    private String submittedAt;

    public ReviewResponseDTO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getManuscriptId() {
        return manuscriptId;
    }

    public void setManuscriptId(String manuscriptId) {
        this.manuscriptId = manuscriptId;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public int getTechnicalQuality() {
        return technicalQuality;
    }

    public void setTechnicalQuality(int technicalQuality) {
        this.technicalQuality = technicalQuality;
    }

    public int getOriginality() {
        return originality;
    }

    public void setOriginality(int originality) {
        this.originality = originality;
    }

    public int getClarity() {
        return clarity;
    }

    public void setClarity(int clarity) {
        this.clarity = clarity;
    }

    public String getCommentsForEditor() {
        return commentsForEditor;
    }

    public void setCommentsForEditor(String commentsForEditor) {
        this.commentsForEditor = commentsForEditor;
    }

    public String getCommentsForAuthor() {
        return commentsForAuthor;
    }

    public void setCommentsForAuthor(String commentsForAuthor) {
        this.commentsForAuthor = commentsForAuthor;
    }

    public String getConfidentialComments() {
        return confidentialComments;
    }

    public void setConfidentialComments(String confidentialComments) {
        this.confidentialComments = confidentialComments;
    }

    public int getStage() {
        return stage;
    }

    public void setStage(int stage) {
        this.stage = stage;
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public String getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(String submittedAt) {
        this.submittedAt = submittedAt;
    }

    public java.util.List<Integer> getRubricScores() {
        return rubricScores;
    }

    public void setRubricScores(java.util.List<Integer> rubricScores) {
        this.rubricScores = rubricScores;
    }

    public Double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Double overallScore) {
        this.overallScore = overallScore;
    }
}
