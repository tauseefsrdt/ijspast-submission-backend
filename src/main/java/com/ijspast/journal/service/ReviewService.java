package com.ijspast.journal.service;

import com.ijspast.journal.dto.request.SubmitReviewRequest;
import com.ijspast.journal.dto.response.ManuscriptResponseDTO;
import com.ijspast.journal.dto.response.ReviewResponseDTO;

import java.util.List;

public interface ReviewService {

    List<ManuscriptResponseDTO> getReviewerAssignments();

    ManuscriptResponseDTO getReviewerAssignmentById(String id);

    ReviewResponseDTO submitReview(String manuscriptId, SubmitReviewRequest request);

    List<ReviewResponseDTO> getReviewsByManuscriptId(String manuscriptId);
}
