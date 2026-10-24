package com.ijspast.journal.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ijspast.journal.dto.response.ReviewResponseDTO;
import com.ijspast.journal.entity.Review;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class ReviewMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReviewResponseDTO toReviewResponseDTO(Review review) {
        if (review == null) {
            return null;
        }
        ReviewResponseDTO dto = new ReviewResponseDTO();
        dto.setId(review.getId());
        if (review.getManuscript() != null) {
            dto.setManuscriptId(review.getManuscript().getId());
        }
        if (review.getReviewer() != null) {
            dto.setReviewerId(review.getReviewer().getId());
        }
        dto.setReviewerName(review.getReviewerName());
        dto.setRecommendation(review.getRecommendation());
        dto.setTechnicalQuality(review.getTechnicalQuality());
        dto.setOriginality(review.getOriginality());
        dto.setClarity(review.getClarity());
        dto.setCommentsForEditor(review.getCommentsForEditor());
        dto.setCommentsForAuthor(review.getCommentsForAuthor());
        dto.setConfidentialComments(review.getConfidentialComments());
        dto.setStage(review.getStage());
        dto.setStageName(review.getStageName());

        if (review.getRubricScores() != null && !review.getRubricScores().isBlank()) {
            try {
                dto.setRubricScores(objectMapper.readValue(review.getRubricScores(), new TypeReference<List<Integer>>() {}));
            } catch (Exception e) {
                dto.setRubricScores(Collections.emptyList());
            }
        } else {
            dto.setRubricScores(Collections.emptyList());
        }
        dto.setOverallScore(review.getOverallScore());

        if (review.getSubmittedAt() != null) {
            dto.setSubmittedAt(review.getSubmittedAt().toString());
        }
        return dto;
    }
}
