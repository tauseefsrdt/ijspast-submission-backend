package com.ijspast.journal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ijspast.journal.dto.request.SubmitReviewRequest;
import com.ijspast.journal.dto.response.ManuscriptResponseDTO;
import com.ijspast.journal.dto.response.ReviewResponseDTO;
import com.ijspast.journal.entity.Manuscript;
import com.ijspast.journal.entity.ManuscriptFile;
import com.ijspast.journal.entity.Review;
import com.ijspast.journal.entity.ReviewAssignment;
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

        boolean isCurrentReviewer1 = manuscript.getAssignedReviewer() != null && manuscript.getAssignedReviewer().getId().equals(reviewer.getId());
        boolean isCurrentReviewer2 = manuscript.getAssignedReviewer2() != null && manuscript.getAssignedReviewer2().getId().equals(reviewer.getId());

        if (!isCurrentReviewer1 && !isCurrentReviewer2) {
            throw new UnauthorizedException("You are not currently assigned to review this manuscript. Your assignment may have been superseded or reassigned.");
        }

        // Verify assignment status is not superseded
        List<ReviewAssignment> reviewerAssignments = reviewAssignmentRepository.findByManuscriptAndReviewer(manuscript, reviewer);
        boolean hasActiveAssignment = reviewerAssignments.isEmpty() || reviewerAssignments.stream().anyMatch(a ->
                "ASSIGNED".equalsIgnoreCase(a.getAssignmentStatus()) || "PENDING".equalsIgnoreCase(a.getAssignmentStatus())
        );
        boolean isSuperseded = reviewerAssignments.stream().anyMatch(a -> "SUPERSEDED".equalsIgnoreCase(a.getAssignmentStatus()));
        if (isSuperseded && !hasActiveAssignment) {
            throw new UnauthorizedException("Your assignment for this manuscript has been superseded or reassigned due to deadline expiration.");
        }

        // Requirement 5: Each reviewer may submit feedback a maximum of two times for the same manuscript
        List<Review> pastReviews = reviewRepository.findByManuscriptAndReviewer(manuscript, reviewer);
        int submissionCount = pastReviews.size();

        if (submissionCount >= 2) {
            throw new BadRequestException("You have completed the maximum permitted feedback submissions (2/2) for this manuscript. Your reviewer role has ended.");
        }

        // First submission vs Second submission:
        if (submissionCount == 1) {
            // Second submission:
            // "The second feedback form must remain unavailable until the editor explicitly forwards the revised manuscript to the reviewer for the second review round."
            // "Do not automatically activate the second form merely because the first feedback has been submitted."
            boolean isForwarded = Boolean.TRUE.equals(manuscript.getRevisionForwardedToReviewer());
            if (!isForwarded) {
                throw new BadRequestException("Second feedback submission is locked until the author submits revisions and the Editor-in-Chief explicitly forwards the revised manuscript for the second review round.");
            }
        }

        int round = (submissionCount == 1 || Boolean.TRUE.equals(manuscript.getRevisionForwardedToReviewer())) ? 2 : 1;
        int stage = (round == 2) ? 2 : (request.getStage() > 0 ? request.getStage() : 1);

        String defaultStageName = (round == 2)
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
        final int finalSubmissionCount = submissionCount + 1;
        reviewAssignmentRepository.findByManuscriptAndReviewer(manuscript, reviewer)
                .forEach(asgn -> {
                    review.setAssignment(asgn);
                    if (finalSubmissionCount >= 2 || finalRound == 2) {
                        asgn.setAssignmentStatus("COMPLETED");
                        reviewAssignmentRepository.save(asgn);
                    }
                });

        Review savedReview = reviewRepository.save(review);

        // Update Manuscript Status & Feedback
        if (stage >= 2 || finalSubmissionCount >= 2 || finalRound == 2) {
            manuscript.setStatus("Review Completed");
        } else if (!"Review Completed".equalsIgnoreCase(manuscript.getStatus())) {
            manuscript.setStatus("Under Review");
        }

        manuscript.setReviewerFeedbackForAuthor(request.getCommentsForAuthor().trim());
        manuscript.setReviewerFeedbackForEditor(request.getCommentsForEditor().trim());

        String nowStr = LocalDateTime.now().format(DATE_TIME_FORMAT);
        String historyStatus = (stage >= 2 || finalSubmissionCount >= 2 || finalRound == 2) ? "Review Completed" : "Under Review";
        String historyDesc = String.format("%s (Feedback %d/2) submitted to Editor by %s. Recommendation: %s. Technical quality: %d/5, Originality: %d/5, Clarity: %d/5.",
                stageName, finalSubmissionCount, reviewer.getName(), request.getRecommendation(), request.getTechnicalQuality(), request.getOriginality(), request.getClarity());

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
