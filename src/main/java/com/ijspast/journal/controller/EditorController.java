package com.ijspast.journal.controller;

import com.ijspast.journal.dto.request.AssignReviewerRequest;
import com.ijspast.journal.dto.request.CreateReviewerRequest;
import com.ijspast.journal.dto.request.EditorialDecisionRequest;
import com.ijspast.journal.dto.response.*;
import com.ijspast.journal.service.EmailService;
import com.ijspast.journal.service.ManuscriptService;
import com.ijspast.journal.service.ReviewService;
import com.ijspast.journal.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/editor")
public class EditorController {

    private final ManuscriptService manuscriptService;
    private final UserService userService;
    private final ReviewService reviewService;
    private final EmailService emailService;

    public EditorController(ManuscriptService manuscriptService,
                            UserService userService,
                            ReviewService reviewService,
                            EmailService emailService) {
        this.manuscriptService = manuscriptService;
        this.userService = userService;
        this.reviewService = reviewService;
        this.emailService = emailService;
    }

    @GetMapping("/manuscripts")
    public ResponseEntity<ApiResponse<List<ManuscriptResponseDTO>>> getAllManuscripts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String search) {
        List<ManuscriptResponseDTO> list = manuscriptService.getEditorManuscripts(status, domain, search);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/manuscripts/{id}")
    public ResponseEntity<ApiResponse<ManuscriptResponseDTO>> getManuscriptById(@PathVariable String id) {
        ManuscriptResponseDTO manuscript = manuscriptService.getEditorManuscriptById(id);
        return ResponseEntity.ok(ApiResponse.success(manuscript));
    }

    @GetMapping("/reviewers")
    public ResponseEntity<ApiResponse<List<ReviewerProfileDTO>>> getReviewers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String domain) {
        List<ReviewerProfileDTO> reviewers = userService.getReviewers(search, domain);
        return ResponseEntity.ok(ApiResponse.success(reviewers));
    }

    @PostMapping("/reviewers")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createReviewer(
            @Valid @RequestBody CreateReviewerRequest request) {
        Map<String, Object> created = userService.createReviewer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Reviewer created and invitation email dispatched", created));
    }

    @PostMapping("/manuscripts/{id}/assign-reviewer")
    public ResponseEntity<ApiResponse<ManuscriptResponseDTO>> assignReviewer(
            @PathVariable String id,
            @Valid @RequestBody AssignReviewerRequest request) {
        ManuscriptResponseDTO updated = manuscriptService.assignReviewer(id, request);
        return ResponseEntity.ok(ApiResponse.success("Reviewer assigned successfully", updated));
    }

    @PostMapping("/manuscripts/{id}/decision")
    public ResponseEntity<ApiResponse<ManuscriptResponseDTO>> makeEditorialDecision(
            @PathVariable String id,
            @Valid @RequestBody EditorialDecisionRequest request) {
        ManuscriptResponseDTO updated = manuscriptService.makeEditorialDecision(id, request);
        return ResponseEntity.ok(ApiResponse.success("Editorial decision recorded and notifications sent", updated));
    }

    @GetMapping("/manuscripts/{id}/reviews")
    public ResponseEntity<ApiResponse<List<ReviewResponseDTO>>> getManuscriptReviews(@PathVariable String id) {
        List<ReviewResponseDTO> reviews = reviewService.getReviewsByManuscriptId(id);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @GetMapping("/outbox")
    public ResponseEntity<ApiResponse<List<DispatchedEmailDTO>>> getOutbox(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search) {
        List<DispatchedEmailDTO> outbox = emailService.getOutboxEmails(type, search);
        return ResponseEntity.ok(ApiResponse.success(outbox));
    }
}
