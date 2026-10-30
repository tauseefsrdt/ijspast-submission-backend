package com.ijspast.journal.mapper;

import com.ijspast.journal.dto.response.*;
import com.ijspast.journal.entity.Manuscript;
import com.ijspast.journal.entity.ManuscriptFile;
import com.ijspast.journal.entity.StatusHistoryItem;
import com.ijspast.journal.entity.Review;
import com.ijspast.journal.entity.User;
import com.ijspast.journal.repository.ManuscriptCommunicationRepository;
import com.ijspast.journal.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ManuscriptMapper {

    private final ReviewRepository reviewRepository;
    private final ManuscriptCommunicationRepository communicationRepository;

    public ManuscriptMapper(@Autowired(required = false) ReviewRepository reviewRepository,
                            @Autowired(required = false) ManuscriptCommunicationRepository communicationRepository) {
        this.reviewRepository = reviewRepository;
        this.communicationRepository = communicationRepository;
    }

    public ManuscriptResponseDTO toDTO(Manuscript manuscript, boolean maskAuthor) {
        return toDTO(manuscript, maskAuthor, false);
    }

    public ManuscriptResponseDTO toAuthorDTO(Manuscript manuscript) {
        return toDTO(manuscript, false, true);
    }

    public ManuscriptResponseDTO toDTO(Manuscript manuscript, boolean maskAuthor, boolean maskReviewer) {
        if (manuscript == null) {
            return null;
        }

        ManuscriptResponseDTO dto = new ManuscriptResponseDTO();
        dto.setId(manuscript.getId());
        dto.setTitle(manuscript.getTitle());
        dto.setDomain(manuscript.getDomain());
        dto.setAbstractText(manuscript.getAbstractText());
        dto.setKeywords(parseKeywords(manuscript.getKeywords()));
        dto.setStatus(manuscript.getStatus());
        dto.setSubmissionDate(manuscript.getSubmissionDate());
        if (manuscript.getUpdatedAt() != null) {
            dto.setUpdatedAt(manuscript.getUpdatedAt().toString());
        }
        dto.setSimilarityScore(manuscript.getSimilarityScore());
        dto.setCoauthors(manuscript.getCoauthors());

        dto.setRevisionForwardedToReviewer(manuscript.getRevisionForwardedToReviewer());
        dto.setStage2EnabledByEditor(manuscript.getStage2EnabledByEditor());
        dto.setReviewerReassignedAfterRevision(manuscript.getReviewerReassignedAfterRevision());

        // Author mapping with strict double-blind masking
        if (maskAuthor) {
            AuthorInfoDTO maskedAuthor = new AuthorInfoDTO();
            maskedAuthor.setName("[Masked for Double-Blind Review]");
            maskedAuthor.setEmail(null);
            maskedAuthor.setInstitution(null);
            maskedAuthor.setDepartment(null);
            maskedAuthor.setCityCountry(null);
            maskedAuthor.setOrcid(null);
            dto.setAuthor(maskedAuthor);
            dto.setCoauthors(null);

            // If author revision has not been explicitly forwarded by Editor, mask author response and revised files from reviewer
            if (!manuscript.getRevisionForwardedToReviewer()) {
                dto.setAuthorRevisionResponse(null);
            }
        } else {
            AuthorInfoDTO authorDTO = new AuthorInfoDTO();
            authorDTO.setName(manuscript.getAuthorName());
            authorDTO.setEmail(manuscript.getAuthorEmail());
            authorDTO.setInstitution(manuscript.getAuthorInstitution());
            authorDTO.setDepartment(manuscript.getAuthorDepartment());
            authorDTO.setCityCountry(manuscript.getAuthorCityCountry());
            authorDTO.setOrcid(manuscript.getAuthorOrcid());
            dto.setAuthor(authorDTO);
        }

        dto.setEditorComments(manuscript.getEditorComments());

        int activeRound = 1;
        if ("Revision Submitted".equalsIgnoreCase(manuscript.getStatus())
                || Boolean.TRUE.equals(manuscript.getRevisionForwardedToReviewer())
                || (manuscript.getFiles() != null && manuscript.getFiles().stream().anyMatch(ManuscriptFile::isRevised))
                || (manuscript.getAuthorRevisionResponse() != null && !manuscript.getAuthorRevisionResponse().isBlank())) {
            activeRound = 2;
        }

        int readyCount = 0;
        boolean hasRev1 = manuscript.getAssignedReviewer() != null;
        boolean hasRev2 = manuscript.getAssignedReviewer2() != null;

        // Assigned reviewer details
        if (maskReviewer) {
            dto.setAssignedReviewerId(null);
            dto.setAssignedReviewerName(null);
            dto.setAssignedReviewer2Id(null);
            dto.setAssignedReviewer2Name(null);
            dto.setReviewerFeedbackForEditor(null);
        } else {
            if (hasRev1) {
                User r1 = manuscript.getAssignedReviewer();
                dto.setAssignedReviewerId(r1.getId());
                dto.setAssignedReviewerName(manuscript.getAssignedReviewerName());

                LocalDate r1Assigned = manuscript.getAssignedReviewer1Date();
                if (r1Assigned == null && manuscript.getAssignedDate() != null) {
                    try { r1Assigned = LocalDate.parse(manuscript.getAssignedDate()); } catch (Exception ignored) {}
                }
                LocalDate r1Deadline = manuscript.getAssignedReviewer1Deadline();
                if (r1Deadline == null && manuscript.getReviewDeadline() != null) {
                    try { r1Deadline = LocalDate.parse(manuscript.getReviewDeadline()); } catch (Exception ignored) {}
                }
                if (r1Assigned != null && r1Deadline == null) {
                    r1Deadline = r1Assigned.plusDays(15);
                }

                if (r1Assigned != null) dto.setAssignedReviewer1Date(r1Assigned.toString());
                if (r1Deadline != null) dto.setAssignedReviewer1Deadline(r1Deadline.toString());

                List<Review> r1Reviews = reviewRepository != null 
                        ? reviewRepository.findByManuscriptAndReviewer(manuscript, r1)
                        : new ArrayList<>();
                int r1Count = r1Reviews.size();
                dto.setReviewer1SubmissionCount(r1Count);
                dto.setReviewer1RoleEnded(r1Count >= 2);

                final int r1ActiveRound = activeRound;
                boolean r1SubmittedActiveRound = r1Reviews.stream().anyMatch(r -> {
                    if (r1ActiveRound == 2) {
                        return (r.getRound() != null && r.getRound() >= 2) || r.getStage() >= 2 || (r.getStageName() != null && r.getStageName().toLowerCase().contains("round 2"));
                    } else {
                        return r.getRound() == null || r.getRound() == 1 || r.getStage() == 1 || r.getStage() == 2;
                    }
                });

                if (r1SubmittedActiveRound) {
                    dto.setReviewer1Status("Completed");
                    dto.setReviewer1CanReassign(false);
                    dto.setReviewer1DaysRemaining(0L);
                    readyCount++;
                } else {
                    long daysRem = r1Deadline != null ? ChronoUnit.DAYS.between(LocalDate.now(), r1Deadline) : 15L;
                    dto.setReviewer1DaysRemaining(daysRem);
                    if (daysRem < 0 || (r1Deadline != null && LocalDate.now().isAfter(r1Deadline))) {
                        dto.setReviewer1Status("Overdue");
                        dto.setReviewer1CanReassign(true);
                    } else {
                        dto.setReviewer1Status("Pending");
                        dto.setReviewer1CanReassign(false);
                    }
                }
            }

            if (hasRev2) {
                User r2 = manuscript.getAssignedReviewer2();
                dto.setAssignedReviewer2Id(r2.getId());
                dto.setAssignedReviewer2Name(manuscript.getAssignedReviewer2Name());

                LocalDate r2Assigned = manuscript.getAssignedReviewer2Date();
                if (r2Assigned == null && manuscript.getAssignedDate() != null) {
                    try { r2Assigned = LocalDate.parse(manuscript.getAssignedDate()); } catch (Exception ignored) {}
                }
                LocalDate r2Deadline = manuscript.getAssignedReviewer2Deadline();
                if (r2Deadline == null && manuscript.getReviewDeadline() != null) {
                    try { r2Deadline = LocalDate.parse(manuscript.getReviewDeadline()); } catch (Exception ignored) {}
                }
                if (r2Assigned != null && r2Deadline == null) {
                    r2Deadline = r2Assigned.plusDays(15);
                }

                if (r2Assigned != null) dto.setAssignedReviewer2Date(r2Assigned.toString());
                if (r2Deadline != null) dto.setAssignedReviewer2Deadline(r2Deadline.toString());

                List<Review> r2Reviews = reviewRepository != null 
                        ? reviewRepository.findByManuscriptAndReviewer(manuscript, r2)
                        : new ArrayList<>();
                int r2Count = r2Reviews.size();
                dto.setReviewer2SubmissionCount(r2Count);
                dto.setReviewer2RoleEnded(r2Count >= 2);

                final int r2ActiveRound = activeRound;
                boolean r2SubmittedActiveRound = r2Reviews.stream().anyMatch(r -> {
                    if (r2ActiveRound == 2) {
                        return (r.getRound() != null && r.getRound() >= 2) || r.getStage() >= 2 || (r.getStageName() != null && r.getStageName().toLowerCase().contains("round 2"));
                    } else {
                        return r.getRound() == null || r.getRound() == 1 || r.getStage() == 1 || r.getStage() == 2;
                    }
                });

                if (r2SubmittedActiveRound) {
                    dto.setReviewer2Status("Completed");
                    dto.setReviewer2CanReassign(false);
                    dto.setReviewer2DaysRemaining(0L);
                    readyCount++;
                } else {
                    long daysRem = r2Deadline != null ? ChronoUnit.DAYS.between(LocalDate.now(), r2Deadline) : 15L;
                    dto.setReviewer2DaysRemaining(daysRem);
                    if (daysRem < 0 || (r2Deadline != null && LocalDate.now().isAfter(r2Deadline))) {
                        dto.setReviewer2Status("Overdue");
                        dto.setReviewer2CanReassign(true);
                    } else {
                        dto.setReviewer2Status("Pending");
                        dto.setReviewer2CanReassign(false);
                    }
                }
            }
            dto.setReviewerFeedbackForEditor(manuscript.getReviewerFeedbackForEditor());
        }

        dto.setFeedbackReadyCount(readyCount);
        dto.setFeedbackProgress(readyCount + "/2 Ready");

        dto.setAssignedDate(manuscript.getAssignedDate());
        dto.setReviewDeadline(manuscript.getReviewDeadline());
        dto.setRevisionRequestComments(manuscript.getRevisionRequestComments());
        if (!maskAuthor || manuscript.getRevisionForwardedToReviewer()) {
            dto.setAuthorRevisionResponse(manuscript.getAuthorRevisionResponse());
        }
        dto.setReviewerFeedbackForAuthor(manuscript.getReviewerFeedbackForAuthor());
        dto.setRejectionReason(manuscript.getRejectionReason());

        // Publication details
        if (manuscript.getPublicationDoi() != null || manuscript.getPublicationVolume() != null) {
            PublicationDetailsDTO pub = new PublicationDetailsDTO();
            pub.setDoi(manuscript.getPublicationDoi());
            pub.setVolume(manuscript.getPublicationVolume());
            pub.setIssue(manuscript.getPublicationIssue());
            pub.setPublishedDate(manuscript.getPublicationDate());
            dto.setPublicationDetails(pub);
        }

        // Files mapping
        List<ManuscriptFile> files = manuscript.getFiles();
        if (files != null && !files.isEmpty()) {
            // Find base file (not revised or lowest version)
            ManuscriptFile primaryFile = files.stream()
                    .filter(f -> !f.isRevised())
                    .findFirst()
                    .orElse(files.get(0));

            dto.setFile(toFileDTO(primaryFile));

            // Only expose revised files to reviewer if Editor has explicitly forwarded the revision
            if (!maskAuthor || manuscript.getRevisionForwardedToReviewer()) {
                List<UploadedFileDTO> revisedDTOs = files.stream()
                        .filter(ManuscriptFile::isRevised)
                        .map(this::toFileDTO)
                        .collect(Collectors.toList());
                dto.setRevisedFiles(revisedDTOs);
            } else {
                dto.setRevisedFiles(new ArrayList<>());
            }
        }

        if (manuscript.getRevisions() != null && !manuscript.getRevisions().isEmpty()) {
            List<ManuscriptRevisionDTO> revisionDTOs = manuscript.getRevisions().stream()
                    .map(r -> new ManuscriptRevisionDTO(
                            r.getId(),
                            r.getRoundNumber(),
                            toFileDTO(r.getRevisedFile()),
                            r.getAuthorRebuttalResponse(),
                            r.getSubmittedAt() != null ? r.getSubmittedAt().toString() : null
                    ))
                    .collect(Collectors.toList());
            dto.setRevisions(revisionDTOs);
        }

        // Calculate and set actual author revision count
        int revCount = 0;
        if (manuscript.getFiles() != null) {
            revCount = (int) manuscript.getFiles().stream().filter(ManuscriptFile::isRevised).count();
        }
        if (manuscript.getRevisions() != null && manuscript.getRevisions().size() > revCount) {
            revCount = manuscript.getRevisions().size();
        }
        if (manuscript.getHistory() != null) {
            int histCount = (int) manuscript.getHistory().stream()
                    .filter(h -> "Revision Submitted".equalsIgnoreCase(h.getStatus()))
                    .count();
            if (histCount > revCount) {
                revCount = histCount;
            }
        }
        dto.setRevisionCount(revCount);

        // Decision & Action availability
        boolean hasAssignedReviewers = hasRev1 || hasRev2;
        boolean inActiveEvaluation = hasAssignedReviewers && (
                "Under Review".equalsIgnoreCase(manuscript.getStatus())
                || "Reviewer Assigned".equalsIgnoreCase(manuscript.getStatus())
                || ("Revision Submitted".equalsIgnoreCase(manuscript.getStatus()) && hasAssignedReviewers)
                || (readyCount < 2 && !"Accepted".equalsIgnoreCase(manuscript.getStatus()) && !"Published".equalsIgnoreCase(manuscript.getStatus()) && !"Rejected".equalsIgnoreCase(manuscript.getStatus()) && revCount < 2)
        );

        // Requirement 1: Reject button hidden during active reviewer evaluation
        dto.setCanReject(!inActiveEvaluation);

        // Requirement 2: Accept button disabled until both reviewers submit feedback (2/2 Ready)
        if (!hasAssignedReviewers) {
            dto.setCanAccept(false);
        } else if (hasRev1 && hasRev2) {
            dto.setCanAccept(readyCount >= 2 || revCount >= 2);
        } else {
            dto.setCanAccept(readyCount >= 1 || revCount >= 1);
        }

        // Requirement 6: Post-review communication stage
        boolean isPostReview = isPostReviewStage(manuscript);
        dto.setCanCommunicate(isPostReview);
        int exchanges = communicationRepository != null 
                ? (int) communicationRepository.countByManuscript(manuscript) 
                : (manuscript.getCommunications() != null ? manuscript.getCommunications().size() : 0);
        dto.setCommunicationExchanges(exchanges);

        // History items
        if (manuscript.getHistory() != null) {
            final String rawAuthorName = manuscript.getAuthorName();
            final String r1Name = manuscript.getAssignedReviewerName();
            final String r2Name = manuscript.getAssignedReviewer2Name();

            List<StatusHistoryItemDTO> historyDTOs = manuscript.getHistory().stream()
                    .filter(item -> {
                        if (!maskReviewer) return true;
                        // Requirement 1: Hide all reviewer-related information from Author status history
                        if (item.getRole() != null && item.getRole().equalsIgnoreCase("Reviewer")) {
                            return false;
                        }
                        if (item.getActor() != null && item.getActor().toLowerCase().contains("reviewer")) {
                            return false;
                        }
                        if (r1Name != null && !r1Name.isBlank() && item.getActor() != null && item.getActor().equalsIgnoreCase(r1Name)) {
                            return false;
                        }
                        if (r2Name != null && !r2Name.isBlank() && item.getActor() != null && item.getActor().equalsIgnoreCase(r2Name)) {
                            return false;
                        }
                        String c = item.getComment() != null ? item.getComment().toLowerCase() : "";
                        if (c.contains("submitted to editor by") && !"author".equalsIgnoreCase(item.getRole())) {
                            return false;
                        }
                        return true;
                    })
                    .map(item -> toHistoryDTO(item, maskAuthor, rawAuthorName, maskReviewer, r1Name, r2Name))
                    .collect(Collectors.toList());
            dto.setHistory(historyDTOs);
        }

        return dto;
    }

    public UploadedFileDTO toFileDTO(ManuscriptFile file) {
        if (file == null) {
            return null;
        }
        UploadedFileDTO dto = new UploadedFileDTO();
        dto.setName(file.getName());
        dto.setSize(file.getSize());
        dto.setType(file.getType());
        if (file.getUploadedAt() != null) {
            dto.setUploadedAt(file.getUploadedAt().toString());
        }
        dto.setVersion(file.getVersion());
        dto.setUrl("/api/v1/files/" + file.getId() + "/download");
        return dto;
    }

    public StatusHistoryItemDTO toHistoryDTO(StatusHistoryItem item, boolean maskAuthor, String rawAuthorName) {
        return toHistoryDTO(item, maskAuthor, rawAuthorName, false, null, null);
    }

    public StatusHistoryItemDTO toHistoryDTO(StatusHistoryItem item, boolean maskAuthor, String rawAuthorName, boolean maskReviewer, String r1Name, String r2Name) {
        if (item == null) {
            return null;
        }
        StatusHistoryItemDTO dto = new StatusHistoryItemDTO();
        dto.setId(item.getId());
        dto.setStatus(item.getStatus());
        dto.setDate(item.getDate());

        String actor = item.getActor();
        String role = item.getRole();
        String comment = item.getComment();

        if (maskAuthor && ("Author".equalsIgnoreCase(role) || (rawAuthorName != null && rawAuthorName.equalsIgnoreCase(actor)))) {
            dto.setActor("Author");
            dto.setRole("Author");
            if (comment != null && rawAuthorName != null) {
                comment = comment.replace(rawAuthorName, "Author");
            }
            dto.setComment(comment);
        } else {
            if (maskReviewer) {
                if (comment != null) {
                    if (comment.contains("Assigned to Reviewer")) {
                        comment = comment.replaceAll("Assigned to Reviewer.*?for", "Assigned for");
                    }
                    if (r1Name != null && !r1Name.isBlank()) {
                        comment = comment.replace(r1Name, "Peer Reviewer");
                    }
                    if (r2Name != null && !r2Name.isBlank()) {
                        comment = comment.replace(r2Name, "Peer Reviewer");
                    }
                }
            }
            dto.setActor(actor);
            dto.setRole(role);
            dto.setComment(comment);
        }
        return dto;
    }

    private List<String> parseKeywords(String keywordsStr) {
        if (keywordsStr == null || keywordsStr.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(keywordsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public boolean isPostReviewStage(Manuscript manuscript) {
        if (manuscript == null) return false;
        String status = manuscript.getStatus();
        if ("Review Completed".equalsIgnoreCase(status)
                || "Accepted".equalsIgnoreCase(status)
                || "Published".equalsIgnoreCase(status)
                || "Revision Requested".equalsIgnoreCase(status)
                || "Rejected".equalsIgnoreCase(status)) {
            return true;
        }
        if (manuscript.getHistory() != null) {
            return manuscript.getHistory().stream().anyMatch(h -> 
                "Review Completed".equalsIgnoreCase(h.getStatus()) || "Accepted".equalsIgnoreCase(h.getStatus())
            );
        }
        return false;
    }
}
