package com.ijspast.journal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ijspast.journal.dto.request.SubmitReviewRequest;
import com.ijspast.journal.dto.response.ManuscriptResponseDTO;
import com.ijspast.journal.dto.response.ReviewResponseDTO;
import com.ijspast.journal.entity.Manuscript;
import com.ijspast.journal.entity.Review;
import com.ijspast.journal.entity.StatusHistoryItem;
import com.ijspast.journal.entity.User;
import com.ijspast.journal.exception.ResourceNotFoundException;
import com.ijspast.journal.exception.UnauthorizedException;
import com.ijspast.journal.mapper.ManuscriptMapper;
import com.ijspast.journal.mapper.ReviewMapper;
import com.ijspast.journal.repository.ManuscriptRepository;
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
    private final ManuscriptRepository manuscriptRepository;
    private final UserService userService;
    private final ReviewMapper reviewMapper;
    private final ManuscriptMapper manuscriptMapper;
    private final EmailService emailService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             ManuscriptRepository manuscriptRepository,
                             UserService userService,
                             ReviewMapper reviewMapper,
                             ManuscriptMapper manuscriptMapper,
                             EmailService emailService) {
        this.reviewRepository = reviewRepository;
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

        int stage = request.getStage() > 0 ? request.getStage() : 1;
        String defaultStageName = (stage == 1) ? "Stage 1: Initial Assessment" : "Stage 2: Final Evaluation";
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

        Review savedReview = reviewRepository.save(review);

        // Update Manuscript Status & Feedback
        if (stage == 2) {
            manuscript.setStatus("Review Completed");
        } else if (!"Review Completed".equalsIgnoreCase(manuscript.getStatus())) {
            manuscript.setStatus("Under Review");
        }

        manuscript.setReviewerFeedbackForAuthor(request.getCommentsForAuthor().trim());
        manuscript.setReviewerFeedbackForEditor(request.getCommentsForEditor().trim());

        String nowStr = LocalDateTime.now().format(DATE_TIME_FORMAT);
        String historyStatus = (stage == 2) ? "Review Completed" : "Under Review";
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
