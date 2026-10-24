package com.ijspast.journal.mapper;

import com.ijspast.journal.dto.response.ReviewResponseDTO;
import com.ijspast.journal.entity.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

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
        if (review.getSubmittedAt() != null) {
            dto.setSubmittedAt(review.getSubmittedAt().toString());
        }
        return dto;
    }
}
