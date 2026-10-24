package com.ijspast.journal.mapper;

import com.ijspast.journal.dto.response.*;
import com.ijspast.journal.entity.Manuscript;
import com.ijspast.journal.entity.ManuscriptFile;
import com.ijspast.journal.entity.StatusHistoryItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ManuscriptMapper {

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

        // Assigned reviewer details
        if (maskReviewer) {
            dto.setAssignedReviewerId(null);
            dto.setAssignedReviewerName(null);
            dto.setAssignedReviewer2Id(null);
            dto.setAssignedReviewer2Name(null);
            dto.setReviewerFeedbackForEditor(null);
        } else {
            if (manuscript.getAssignedReviewer() != null) {
                dto.setAssignedReviewerId(manuscript.getAssignedReviewer().getId());
            }
            dto.setAssignedReviewerName(manuscript.getAssignedReviewerName());

            if (manuscript.getAssignedReviewer2() != null) {
                dto.setAssignedReviewer2Id(manuscript.getAssignedReviewer2().getId());
            }
            dto.setAssignedReviewer2Name(manuscript.getAssignedReviewer2Name());
            dto.setReviewerFeedbackForEditor(manuscript.getReviewerFeedbackForEditor());
        }

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
}
