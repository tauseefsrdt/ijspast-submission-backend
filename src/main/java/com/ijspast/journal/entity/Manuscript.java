package com.ijspast.journal.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "manuscripts")
public class Manuscript {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false)
    private String domain;

    @Column(name = "abstract_text", nullable = false, columnDefinition = "TEXT")
    private String abstractText;

    @Column(nullable = false, length = 500)
    private String keywords;

    @Column(columnDefinition = "TEXT")
    private String coauthors;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User authorUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_ref_id")
    private Author author;

    @Column(name = "author_name", nullable = false)
    private String authorName;

    @Column(name = "author_email", nullable = false)
    private String authorEmail;

    @Column(name = "author_institution", nullable = false)
    private String authorInstitution;

    @Column(name = "author_department")
    private String authorDepartment;

    @Column(name = "author_city_country", nullable = false)
    private String authorCityCountry;

    @Column(name = "author_orcid", nullable = false, length = 64)
    private String authorOrcid;

    @Column(nullable = false, length = 64)
    private String status;

    @Column(name = "submission_date", nullable = false, length = 32)
    private String submissionDate;

    @Column(name = "similarity_score")
    private Integer similarityScore = 6;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_reviewer_id")
    private User assignedReviewer;

    @Column(name = "assigned_reviewer_name")
    private String assignedReviewerName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_reviewer2_id")
    private User assignedReviewer2;

    @Column(name = "assigned_reviewer2_name")
    private String assignedReviewer2Name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_reviewer1_ref_id")
    private Reviewer assignedReviewer1Ref;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_reviewer2_ref_id")
    private Reviewer assignedReviewer2Ref;

    @Column(name = "assigned_date", length = 32)
    private String assignedDate;

    @Column(name = "review_deadline", length = 32)
    private String reviewDeadline;

    @Column(name = "assigned_reviewer1_date")
    private LocalDate assignedReviewer1Date;

    @Column(name = "assigned_reviewer1_deadline")
    private LocalDate assignedReviewer1Deadline;

    @Column(name = "assigned_reviewer2_date")
    private LocalDate assignedReviewer2Date;

    @Column(name = "assigned_reviewer2_deadline")
    private LocalDate assignedReviewer2Deadline;

    @Column(name = "editor_comments", columnDefinition = "TEXT")
    private String editorComments;

    @Column(name = "revision_request_comments", columnDefinition = "TEXT")
    private String revisionRequestComments;

    @Column(name = "author_revision_response", columnDefinition = "TEXT")
    private String authorRevisionResponse;

    @Column(name = "revision_forwarded_to_reviewer")
    private Boolean revisionForwardedToReviewer = false;

    @Column(name = "stage2_enabled_by_editor")
    private Boolean stage2EnabledByEditor = false;

    @Column(name = "reviewer_reassigned_after_revision")
    private Boolean reviewerReassignedAfterRevision = false;

    @Column(name = "reviewer_feedback_for_author", columnDefinition = "TEXT")
    private String reviewerFeedbackForAuthor;

    @Column(name = "reviewer_feedback_for_editor", columnDefinition = "TEXT")
    private String reviewerFeedbackForEditor;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "publication_doi", length = 128)
    private String publicationDoi;

    @Column(name = "publication_volume", length = 64)
    private String publicationVolume;

    @Column(name = "publication_issue", length = 64)
    private String publicationIssue;

    @Column(name = "publication_date", length = 32)
    private String publicationDate;

    @OneToMany(mappedBy = "manuscript", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ManuscriptFile> files = new ArrayList<>();

    @OneToMany(mappedBy = "manuscript", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<StatusHistoryItem> history = new ArrayList<>();

    @OneToMany(mappedBy = "manuscript", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("roundNumber ASC, assignedDate ASC")
    private List<ReviewAssignment> assignments = new ArrayList<>();

    @OneToMany(mappedBy = "manuscript", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("roundNumber ASC, submittedAt ASC")
    private List<ManuscriptRevision> revisions = new ArrayList<>();

    @OneToMany(mappedBy = "manuscript", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("decidedAt DESC")
    private List<EditorialDecision> decisions = new ArrayList<>();

    @OneToMany(mappedBy = "manuscript", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private List<ManuscriptCoauthor> coauthorList = new ArrayList<>();

    @OneToMany(mappedBy = "manuscript", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sentAt ASC")
    private List<ManuscriptCommunication> communications = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Manuscript() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addFile(ManuscriptFile file) {
        files.add(file);
        file.setManuscript(this);
    }

    public void addHistoryItem(StatusHistoryItem item) {
        history.add(item);
        item.setManuscript(this);
    }

    public void addAssignment(ReviewAssignment assignment) {
        assignments.add(assignment);
        assignment.setManuscript(this);
    }

    public void addRevision(ManuscriptRevision revision) {
        revisions.add(revision);
        revision.setManuscript(this);
    }

    public void addDecision(EditorialDecision decision) {
        decisions.add(decision);
        decision.setManuscript(this);
    }

    public void addCoauthor(ManuscriptCoauthor coauthor) {
        coauthorList.add(coauthor);
        coauthor.setManuscript(this);
    }

    // Getters and Setters
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

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getCoauthors() {
        return coauthors;
    }

    public void setCoauthors(String coauthors) {
        this.coauthors = coauthors;
    }

    public User getAuthorUser() {
        return authorUser;
    }

    public void setAuthorUser(User authorUser) {
        this.authorUser = authorUser;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public String getAuthorInstitution() {
        return authorInstitution;
    }

    public void setAuthorInstitution(String authorInstitution) {
        this.authorInstitution = authorInstitution;
    }

    public String getAuthorDepartment() {
        return authorDepartment;
    }

    public void setAuthorDepartment(String authorDepartment) {
        this.authorDepartment = authorDepartment;
    }

    public String getAuthorCityCountry() {
        return authorCityCountry;
    }

    public void setAuthorCityCountry(String authorCityCountry) {
        this.authorCityCountry = authorCityCountry;
    }

    public String getAuthorOrcid() {
        return authorOrcid;
    }

    public void setAuthorOrcid(String authorOrcid) {
        this.authorOrcid = authorOrcid;
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

    public Integer getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(Integer similarityScore) {
        this.similarityScore = similarityScore;
    }

    public User getAssignedReviewer() {
        return assignedReviewer;
    }

    public void setAssignedReviewer(User assignedReviewer) {
        this.assignedReviewer = assignedReviewer;
    }

    public String getAssignedReviewerName() {
        return assignedReviewerName;
    }

    public void setAssignedReviewerName(String assignedReviewerName) {
        this.assignedReviewerName = assignedReviewerName;
    }

    public User getAssignedReviewer2() {
        return assignedReviewer2;
    }

    public void setAssignedReviewer2(User assignedReviewer2) {
        this.assignedReviewer2 = assignedReviewer2;
    }

    public String getAssignedReviewer2Name() {
        return assignedReviewer2Name;
    }

    public void setAssignedReviewer2Name(String assignedReviewer2Name) {
        this.assignedReviewer2Name = assignedReviewer2Name;
    }

    public Reviewer getAssignedReviewer1Ref() {
        return assignedReviewer1Ref;
    }

    public void setAssignedReviewer1Ref(Reviewer assignedReviewer1Ref) {
        this.assignedReviewer1Ref = assignedReviewer1Ref;
    }

    public Reviewer getAssignedReviewer2Ref() {
        return assignedReviewer2Ref;
    }

    public void setAssignedReviewer2Ref(Reviewer assignedReviewer2Ref) {
        this.assignedReviewer2Ref = assignedReviewer2Ref;
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

    public String getPublicationDoi() {
        return publicationDoi;
    }

    public void setPublicationDoi(String publicationDoi) {
        this.publicationDoi = publicationDoi;
    }

    public String getPublicationVolume() {
        return publicationVolume;
    }

    public void setPublicationVolume(String publicationVolume) {
        this.publicationVolume = publicationVolume;
    }

    public String getPublicationIssue() {
        return publicationIssue;
    }

    public void setPublicationIssue(String publicationIssue) {
        this.publicationIssue = publicationIssue;
    }

    public String getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(String publicationDate) {
        this.publicationDate = publicationDate;
    }

    public List<ManuscriptFile> getFiles() {
        return files;
    }

    public void setFiles(List<ManuscriptFile> files) {
        this.files = files;
    }

    public List<StatusHistoryItem> getHistory() {
        return history;
    }

    public void setHistory(List<StatusHistoryItem> history) {
        this.history = history;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<ReviewAssignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<ReviewAssignment> assignments) {
        this.assignments = assignments;
    }

    public List<ManuscriptRevision> getRevisions() {
        return revisions;
    }

    public void setRevisions(List<ManuscriptRevision> revisions) {
        this.revisions = revisions;
    }

    public List<EditorialDecision> getDecisions() {
        return decisions;
    }

    public void setDecisions(List<EditorialDecision> decisions) {
        this.decisions = decisions;
    }

    public List<ManuscriptCoauthor> getCoauthorList() {
        return coauthorList;
    }

    public void setCoauthorList(List<ManuscriptCoauthor> coauthorList) {
        this.coauthorList = coauthorList;
    }

    public List<ManuscriptCommunication> getCommunications() {
        return communications;
    }

    public void setCommunications(List<ManuscriptCommunication> communications) {
        this.communications = communications;
    }

    public void addCommunication(ManuscriptCommunication communication) {
        communications.add(communication);
        communication.setManuscript(this);
    }

    public LocalDate getAssignedReviewer1Date() {
        return assignedReviewer1Date;
    }

    public void setAssignedReviewer1Date(LocalDate assignedReviewer1Date) {
        this.assignedReviewer1Date = assignedReviewer1Date;
    }

    public LocalDate getAssignedReviewer1Deadline() {
        return assignedReviewer1Deadline;
    }

    public void setAssignedReviewer1Deadline(LocalDate assignedReviewer1Deadline) {
        this.assignedReviewer1Deadline = assignedReviewer1Deadline;
    }

    public LocalDate getAssignedReviewer2Date() {
        return assignedReviewer2Date;
    }

    public void setAssignedReviewer2Date(LocalDate assignedReviewer2Date) {
        this.assignedReviewer2Date = assignedReviewer2Date;
    }

    public LocalDate getAssignedReviewer2Deadline() {
        return assignedReviewer2Deadline;
    }

    public void setAssignedReviewer2Deadline(LocalDate assignedReviewer2Deadline) {
        this.assignedReviewer2Deadline = assignedReviewer2Deadline;
    }

    public String getEditorComments() {
        return editorComments;
    }

    public void setEditorComments(String editorComments) {
        this.editorComments = editorComments;
    }
}
