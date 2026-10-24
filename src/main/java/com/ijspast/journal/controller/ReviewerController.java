package com.ijspast.journal.controller;

import com.ijspast.journal.dto.request.SubmitReviewRequest;
import com.ijspast.journal.dto.response.ApiResponse;
import com.ijspast.journal.dto.response.ManuscriptResponseDTO;
import com.ijspast.journal.dto.response.ReviewResponseDTO;
import com.ijspast.journal.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviewer")
public class ReviewerController {

    private final ReviewService reviewService;

    public ReviewerController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/assignments")
    public ResponseEntity<ApiResponse<List<ManuscriptResponseDTO>>> getMyAssignments() {
        List<ManuscriptResponseDTO> assignments = reviewService.getReviewerAssignments();
        return ResponseEntity.ok(ApiResponse.success(assignments));
    }

    @GetMapping("/assignments/{id}")
    public ResponseEntity<ApiResponse<ManuscriptResponseDTO>> getAssignmentById(@PathVariable String id) {
        ManuscriptResponseDTO assignment = reviewService.getReviewerAssignmentById(id);
        return ResponseEntity.ok(ApiResponse.success(assignment));
    }

    @PostMapping("/assignments/{id}/reviews")
    public ResponseEntity<ApiResponse<ReviewResponseDTO>> submitReview(
            @PathVariable String id,
            @Valid @RequestBody SubmitReviewRequest request) {
        ReviewResponseDTO report = reviewService.submitReview(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Peer review scorecard submitted to Editor successfully", report));
    }

    @GetMapping("/assignments/{id}/reviews")
    public ResponseEntity<ApiResponse<List<ReviewResponseDTO>>> getMyReviewsForManuscript(@PathVariable String id) {
        List<ReviewResponseDTO> reviews = reviewService.getReviewsByManuscriptId(id);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }
}
