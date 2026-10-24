package com.ijspast.journal.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class SubmitReviewRequest {

    @NotBlank(message = "Review recommendation is required (Accept, Minor Revision, Major Revision, Reject)")
    private String recommendation;

    @Min(value = 1, message = "Technical quality must be between 1 and 5")
    @Max(value = 5, message = "Technical quality must be between 1 and 5")
    private int technicalQuality;

    @Min(value = 1, message = "Originality must be between 1 and 5")
    @Max(value = 5, message = "Originality must be between 1 and 5")
    private int originality;

    @Min(value = 1, message = "Clarity must be between 1 and 5")
    @Max(value = 5, message = "Clarity must be between 1 and 5")
    private int clarity;

    @NotBlank(message = "Comments for the editor are required")
    private String commentsForEditor;

    @NotBlank(message = "Comments for the author are required")
    private String commentsForAuthor;

    private String confidentialComments;

    private int stage = 1;

    private Integer round;

    private String stageName;

    private java.util.List<Integer> rubricScores;

    private Double overallScore;

    public SubmitReviewRequest() {
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

    public Integer getRound() {
        return round;
    }

    public void setRound(Integer round) {
        this.round = round;
    }
}
