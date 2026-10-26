package com.ijspast.journal.controller;

import com.ijspast.journal.dto.request.AssignReviewerRequest;
import com.ijspast.journal.dto.request.CreateReviewerRequest;
import com.ijspast.journal.dto.request.EditorialDecisionRequest;
import com.ijspast.journal.dto.request.UpdateReviewerRequest;
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

    @PutMapping("/reviewers/{id}")
    public ResponseEntity<ApiResponse<ReviewerProfileDTO>> updateReviewer(
            @PathVariable String id,
            @Valid @RequestBody UpdateReviewerRequest request) {
        ReviewerProfileDTO updated = userService.updateReviewer(id, request);
        return ResponseEntity.ok(ApiResponse.success("Reviewer updated successfully", updated));
    }

    @DeleteMapping("/reviewers/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReviewer(@PathVariable String id) {
        userService.deleteReviewer(id);
        return ResponseEntity.ok(ApiResponse.success("Reviewer deleted successfully", null));
    }

    @PostMapping("/manuscripts/{id}/assign-reviewer")
    public ResponseEntity<ApiResponse<ManuscriptResponseDTO>> assignReviewer(
            @PathVariable String id,
            @Valid @RequestBody AssignReviewerRequest request) {
        ManuscriptResponseDTO updated = manuscriptService.assignReviewer(id, request);
        return ResponseEntity.ok(ApiResponse.success("Reviewer assigned successfully", updated));
    }

    @PostMapping("/manuscripts/{id}/enable-stage2")
    public ResponseEntity<ApiResponse<ManuscriptResponseDTO>> enableStage2(
            @PathVariable String id) {
        ManuscriptResponseDTO updated = manuscriptService.enableStage2Review(id);
        return ResponseEntity.ok(ApiResponse.success("Stage 2 review enabled for assigned reviewer(s)", updated));
    }

    @PostMapping("/manuscripts/{id}/forward-revision")
    public ResponseEntity<ApiResponse<ManuscriptResponseDTO>> forwardRevisionToReviewer(
            @PathVariable String id,
            @RequestBody(required = false) com.ijspast.journal.dto.request.ForwardRevisionRequest request) {
        ManuscriptResponseDTO updated = manuscriptService.forwardRevisionToReviewer(id, request);
        return ResponseEntity.ok(ApiResponse.success("Revision reviewed by Editor and forwarded to Reviewer successfully", updated));
    }

    @PostMapping("/manuscripts/{id}/decision")
    public ResponseEntity<ApiResponse<ManuscriptResponseDTO>> makeEditorialDecision(
            @PathVariable String id,
            @Valid @RequestBody EditorialDecisionRequest request) {
        ManuscriptResponseDTO updated = manuscriptService.makeEditorialDecision(id, request);
        return ResponseEntity.ok(ApiResponse.success("Editorial decision recorded and notifications sent", updated));
    }

    @PostMapping("/manuscripts/{id}/request-revision")
    public ResponseEntity<ApiResponse<ManuscriptResponseDTO>> requestRevisionDirect(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        EditorialDecisionRequest req = new EditorialDecisionRequest();
        req.setDecision("REQUEST_REVISION");
        req.setComments(body != null ? body.getOrDefault("comments", "") : "");
        ManuscriptResponseDTO updated = manuscriptService.makeEditorialDecision(id, req);
        return ResponseEntity.ok(ApiResponse.success("Consolidated feedback dispatched to author", updated));
    }

    @PostMapping("/manuscripts/{id}/accept")
    public ResponseEntity<ApiResponse<ManuscriptResponseDTO>> acceptDirect(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        EditorialDecisionRequest req = new EditorialDecisionRequest();
        req.setDecision("ACCEPT");
        if (body != null) {
            req.setComments(body.get("comment"));
        }
        ManuscriptResponseDTO updated = manuscriptService.makeEditorialDecision(id, req);
        return ResponseEntity.ok(ApiResponse.success("Manuscript accepted", updated));
    }

    @PostMapping("/manuscripts/{id}/reject")
    public ResponseEntity<ApiResponse<ManuscriptResponseDTO>> rejectDirect(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        EditorialDecisionRequest req = new EditorialDecisionRequest();
        req.setDecision("REJECT");
        req.setRejectionReason(body != null ? body.getOrDefault("reason", "") : "");
        ManuscriptResponseDTO updated = manuscriptService.makeEditorialDecision(id, req);
        return ResponseEntity.ok(ApiResponse.success("Manuscript rejected", updated));
    }

    @PostMapping("/manuscripts/{id}/publish")
    public ResponseEntity<ApiResponse<ManuscriptResponseDTO>> publishDirect(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        EditorialDecisionRequest req = new EditorialDecisionRequest();
        req.setDecision("PUBLISH");
        if (body != null) {
            req.setDoi(body.get("doi"));
            req.setVolume(body.get("volume"));
            req.setIssue(body.get("issue"));
            req.setPublishedDate(body.get("publishedDate"));
        }
        ManuscriptResponseDTO updated = manuscriptService.makeEditorialDecision(id, req);
        return ResponseEntity.ok(ApiResponse.success("Manuscript published", updated));
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

    @GetMapping("/authors-table")
    public ResponseEntity<ApiResponse<List<AuthorTableDTO>>> getAuthorsTable() {
        List<AuthorTableDTO> list = manuscriptService.getAuthorsTable();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/reviewers-table")
    public ResponseEntity<ApiResponse<List<ReviewerTableDTO>>> getReviewersTable() {
        List<ReviewerTableDTO> list = manuscriptService.getReviewersTable();
        return ResponseEntity.ok(ApiResponse.success(list));
    }
}
