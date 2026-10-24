package com.ijspast.journal.service;

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
        List<Manuscript> assigned = manuscriptRepository.findByAssignedReviewerOrderByCreatedAtDesc(reviewer);

        // Author details visible to assigned reviewer
        return assigned.stream()
                .map(m -> manuscriptMapper.toDTO(m, false))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ManuscriptResponseDTO getReviewerAssignmentById(String id) {
        User reviewer = userService.getCurrentAuthenticatedUser();
        Manuscript manuscript = manuscriptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manuscript not found: " + id));

        // Enforce assignment authorization
        if (manuscript.getAssignedReviewer() == null || !manuscript.getAssignedReviewer().getId().equals(reviewer.getId())) {
            throw new UnauthorizedException("This manuscript is not assigned to you.");
        }

        // Author details visible to assigned reviewer
        return manuscriptMapper.toDTO(manuscript, false);
    }

    @Override
    @Transactional
    public ReviewResponseDTO submitReview(String manuscriptId, SubmitReviewRequest request) {
        User reviewer = userService.getCurrentAuthenticatedUser();
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ResourceNotFoundException("Manuscript not found: " + manuscriptId));

        if (manuscript.getAssignedReviewer() == null || !manuscript.getAssignedReviewer().getId().equals(reviewer.getId())) {
            throw new UnauthorizedException("You are not assigned to review this manuscript.");
        }

        // Create Review Report
        String reviewId = "rev_rep_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Review review = new Review();
        review.setId(reviewId);
        review.setManuscript(manuscript);
        review.setReviewer(reviewer);
        review.setReviewerName(reviewer.getName());
        review.setRecommendation(request.getRecommendation());
        review.setTechnicalQuality(request.getTechnicalQuality());
        review.setOriginality(request.getOriginality());
        review.setClarity(request.getClarity());
        review.setCommentsForEditor(request.getCommentsForEditor().trim());
        review.setCommentsForAuthor(request.getCommentsForAuthor().trim());
        review.setConfidentialComments(request.getConfidentialComments() != null ? request.getConfidentialComments().trim() : null);

        Review savedReview = reviewRepository.save(review);

        // Update Manuscript Status & Feedback
        manuscript.setStatus("Review Completed");
        manuscript.setReviewerFeedbackForAuthor(request.getCommentsForAuthor().trim());
        manuscript.setReviewerFeedbackForEditor(request.getCommentsForEditor().trim());

        String nowStr = LocalDateTime.now().format(DATE_TIME_FORMAT);
        StatusHistoryItem historyItem = new StatusHistoryItem(
                "hist_" + System.currentTimeMillis(),
                "Review Completed",
                nowStr,
                reviewer.getName(),
                "Reviewer",
                String.format("Evaluation submitted to Editor. Recommendation: %s. Technical quality: %d/5, Originality: %d/5.",
                        request.getRecommendation(), request.getTechnicalQuality(), request.getOriginality())
        );
        manuscript.addHistoryItem(historyItem);
        manuscriptRepository.save(manuscript);

        // Dispatch notification email to Editor
        emailService.sendReviewCompletedEmail(
                "editor@ijspast.demo",
                reviewer.getName(),
                manuscript.getId(),
                manuscript.getTitle(),
                request.getRecommendation()
        );

        return reviewMapper.toReviewResponseDTO(savedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> getReviewsByManuscriptId(String manuscriptId) {
        return reviewRepository.findByManuscriptIdOrderBySubmittedAtDesc(manuscriptId)
                .stream()
                .map(reviewMapper::toReviewResponseDTO)
                .collect(Collectors.toList());
    }
}
