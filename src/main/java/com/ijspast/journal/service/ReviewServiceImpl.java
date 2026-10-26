package com.ijspast.journal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ijspast.journal.dto.request.SubmitReviewRequest;
import com.ijspast.journal.dto.response.ManuscriptResponseDTO;
import com.ijspast.journal.dto.response.ReviewResponseDTO;
import com.ijspast.journal.entity.Manuscript;
import com.ijspast.journal.entity.ManuscriptFile;
import com.ijspast.journal.entity.Review;
import com.ijspast.journal.entity.StatusHistoryItem;
import com.ijspast.journal.entity.User;
import com.ijspast.journal.exception.BadRequestException;
import com.ijspast.journal.exception.ResourceNotFoundException;
import com.ijspast.journal.exception.UnauthorizedException;
import com.ijspast.journal.mapper.ManuscriptMapper;
import com.ijspast.journal.mapper.ReviewMapper;
import com.ijspast.journal.repository.ManuscriptRepository;
import com.ijspast.journal.repository.ReviewAssignmentRepository;
import com.ijspast.journal.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ReviewRepository reviewRepository;
    private final ReviewAssignmentRepository reviewAssignmentRepository;
    private final ManuscriptRepository manuscriptRepository;
    private final UserService userService;
    private final ReviewMapper reviewMapper;
    private final ManuscriptMapper manuscriptMapper;
    private final EmailService emailService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             ReviewAssignmentRepository reviewAssignmentRepository,
                             ManuscriptRepository manuscriptRepository,
                             UserService userService,
                             ReviewMapper reviewMapper,
                             ManuscriptMapper manuscriptMapper,
                             EmailService emailService) {
        this.reviewRepository = reviewRepository;
        this.reviewAssignmentRepository = reviewAssignmentRepository;
        this.manuscriptRepository = manuscriptRepository;
        this.userService = userService;
        this.reviewMapper = reviewMapper;
        this.manuscriptMapper = manuscriptMapper;
        this.emailService = emailService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManuscriptResponseDTO> getReviewerAssignments() {
        User reviewer = userService.getCurrentAuthenticatedUser();
        List<Manuscript> assigned = manuscriptRepository.findByAssignedReviewerOrAssignedReviewer2OrderByCreatedAtDesc(reviewer);

        // Double-blind peer review: Author identity and details strictly masked in backend response
        return assigned.stream()
                .map(m -> manuscriptMapper.toDTO(m, true))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ManuscriptResponseDTO getReviewerAssignmentById(String id) {
        User reviewer = userService.getCurrentAuthenticatedUser();
        Manuscript manuscript = manuscriptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manuscript not found: " + id));

        // Enforce assignment authorization for either Reviewer 1 or Reviewer 2
        boolean isAssigned = (manuscript.getAssignedReviewer() != null && manuscript.getAssignedReviewer().getId().equals(reviewer.getId()))
                || (manuscript.getAssignedReviewer2() != null && manuscript.getAssignedReviewer2().getId().equals(reviewer.getId()));

        if (!isAssigned) {
            throw new UnauthorizedException("This manuscript is not assigned to you.");
        }

        // Double-blind peer review: Author identity and details strictly masked in backend response
        return manuscriptMapper.toDTO(manuscript, true);
    }

    @Override
    @Transactional
    public ReviewResponseDTO submitReview(String manuscriptId, SubmitReviewRequest request) {
        User reviewer = userService.getCurrentAuthenticatedUser();
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ResourceNotFoundException("Manuscript not found: " + manuscriptId));

        boolean isAssigned = (manuscript.getAssignedReviewer() != null && manuscript.getAssignedReviewer().getId().equals(reviewer.getId()))
                || (manuscript.getAssignedReviewer2() != null && manuscript.getAssignedReviewer2().getId().equals(reviewer.getId()));

        if (!isAssigned) {
            throw new UnauthorizedException("You are not assigned to review this manuscript.");
        }

        // Second-Round Review & Stage 2 Review enforcement:
        boolean isRevisionState = "Revision Submitted".equalsIgnoreCase(manuscript.getStatus())
                || (manuscript.getFiles() != null && manuscript.getFiles().stream().anyMatch(ManuscriptFile::isRevised))
                || (manuscript.getAuthorRevisionResponse() != null && !manuscript.getAuthorRevisionResponse().isBlank());

        boolean isForwardedOrReassigned = Boolean.TRUE.equals(manuscript.getRevisionForwardedToReviewer())
                || Boolean.TRUE.equals(manuscript.getReviewerReassignedAfterRevision())
                || Boolean.TRUE.equals(manuscript.getStage2EnabledByEditor())
                || (manuscript.getHistory() != null && manuscript.getHistory().stream().anyMatch(h ->
                        h.getComment() != null && (h.getComment().contains("forwarded to peer reviewer") || h.getComment().contains("two-stage peer review evaluation"))));

        int stage = request.getStage() > 0 ? request.getStage() : 1;

        if (isRevisionState && !isForwardedOrReassigned) {
            throw new BadRequestException("Review feedback is disabled for reviewers until the Editor-in-Chief either forwards revisions to reviewers or reassigns reviewers.");
        }
        if ((request.getRound() != null && request.getRound() == 2 || request.getStage() == 3) && !isForwardedOrReassigned) {
            throw new BadRequestException("Second-round review feedback is disabled for reviewers until the Editor-in-Chief forwards revisions or reassigns reviewers.");
        }

        int round = (request.getRound() != null && request.getRound() > 0)
                ? request.getRound()
                : (stage == 3 || Boolean.TRUE.equals(manuscript.getRevisionForwardedToReviewer()) ? 2 : (stage == 2 && isRevisionState ? 2 : 1));
        if (Boolean.TRUE.equals(manuscript.getRevisionForwardedToReviewer()) && stage != 1 && stage != 2) {
            stage = 3;
            round = 2;
        } else if (round == 2 && stage != 2) {
            stage = 3;
        }

        // ── Stage 2 Access Control ──
        // If Stage 1 review was submitted, Stage 2 submissions are gated until the Editor has explicitly enabled Stage 2.
        boolean stage1Exists = reviewRepository.findByManuscriptAndReviewerAndStage(manuscript, reviewer, 1).isPresent();
        if (stage == 2 && stage1Exists && !Boolean.TRUE.equals(manuscript.getStage2EnabledByEditor())) {
            throw new BadRequestException(
                    "Stage 2 review submission is not permitted yet. The Editor must first enable Stage 2 after performing 'Forward Revisions to Reviewers' or 'Reassign Reviewer'."
            );
        }

        String defaultStageName = (round == 2 || stage == 3)
                ? "Round 2: Revision Re-evaluation"
                : ((stage == 1) ? "Stage 1: Initial Assessment" : "Stage 2: Final Evaluation");
        String stageName = (request.getStageName() != null && !request.getStageName().isBlank())
                ? request.getStageName().trim()
                : defaultStageName;

        // Check if review for this stage already exists from this reviewer
        Optional<Review> existingReviewOpt = reviewRepository.findByManuscriptAndReviewerAndStage(manuscript, reviewer, stage);
        Review review = existingReviewOpt.orElseGet(Review::new);

        if (review.getId() == null) {
            review.setId("rev_rep_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        }
        review.setManuscript(manuscript);
        review.setReviewer(reviewer);
        review.setReviewerName(reviewer.getName());
        review.setStage(stage);
        review.setRound(round);
        review.setStageName(stageName);
        review.setRecommendation(request.getRecommendation());
        review.setTechnicalQuality(request.getTechnicalQuality());
        review.setOriginality(request.getOriginality());
        review.setClarity(request.getClarity());
        review.setCommentsForEditor(request.getCommentsForEditor().trim());
        review.setCommentsForAuthor(request.getCommentsForAuthor().trim());
        review.setConfidentialComments(request.getConfidentialComments() != null ? request.getConfidentialComments().trim() : null);

        // Store complete reviewer form data: 11-point rubric scores & overall score
        if (request.getRubricScores() != null && !request.getRubricScores().isEmpty()) {
            try {
                review.setRubricScores(objectMapper.writeValueAsString(request.getRubricScores()));
            } catch (Exception e) {
                review.setRubricScores(null);
            }
        }
        if (request.getOverallScore() != null) {
            review.setOverallScore(request.getOverallScore());
        } else if (request.getRubricScores() != null && !request.getRubricScores().isEmpty()) {
            double avg = request.getRubricScores().stream().mapToInt(Integer::intValue).average().orElse(0.0);
            review.setOverallScore(Math.round(avg * 10.0) / 10.0);
        }

        review.setSubmittedAt(LocalDateTime.now());

        final int finalRound = round;
        final int finalStage = stage;
        reviewAssignmentRepository.findByManuscriptAndReviewerAndRoundNumber(manuscript, reviewer, round)
                .ifPresent(asgn -> {
                    review.setAssignment(asgn);
                    if (finalRound == 2 || finalStage >= 2) {
                        asgn.setAssignmentStatus("COMPLETED");
                        reviewAssignmentRepository.save(asgn);
                    }
                });

        Review savedReview = reviewRepository.save(review);

        // Update Manuscript Status & Feedback
        if (round == 2 || stage >= 2) {
            manuscript.setStatus("Review Completed");
        } else if (!"Review Completed".equalsIgnoreCase(manuscript.getStatus())) {
            manuscript.setStatus("Under Review");
        }

        manuscript.setReviewerFeedbackForAuthor(request.getCommentsForAuthor().trim());
        manuscript.setReviewerFeedbackForEditor(request.getCommentsForEditor().trim());

        String nowStr = LocalDateTime.now().format(DATE_TIME_FORMAT);
        String historyStatus = (round == 2 || stage >= 2) ? "Review Completed" : "Under Review";
        String historyDesc = String.format("%s submitted to Editor by %s. Recommendation: %s. Technical quality: %d/5, Originality: %d/5, Clarity: %d/5.",
                stageName, reviewer.getName(), request.getRecommendation(), request.getTechnicalQuality(), request.getOriginality(), request.getClarity());

        StatusHistoryItem historyItem = new StatusHistoryItem(
                "hist_" + System.currentTimeMillis(),
                historyStatus,
                nowStr,
                reviewer.getName(),
                "Reviewer",
                historyDesc
        );
        manuscript.addHistoryItem(historyItem);
        manuscriptRepository.save(manuscript);

        // Dispatch notification email to Editor
        emailService.sendReviewCompletedEmail(
                "editor@ijspast.demo",
                reviewer.getName(),
                manuscript.getId(),
                String.format("[%s] %s", stageName, manuscript.getTitle()),
                request.getRecommendation()
        );

        return reviewMapper.toReviewResponseDTO(savedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> getReviewsByManuscriptId(String manuscriptId) {
        return reviewRepository.findByManuscriptIdOrderByStageAscSubmittedAtAsc(manuscriptId)
                .stream()
                .map(reviewMapper::toReviewResponseDTO)
                .collect(Collectors.toList());
    }
}
