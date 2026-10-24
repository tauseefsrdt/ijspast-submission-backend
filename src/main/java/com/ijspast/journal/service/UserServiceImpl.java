package com.ijspast.journal.service;

import com.ijspast.journal.dto.request.CreateReviewerRequest;
import com.ijspast.journal.dto.request.UpdateReviewerRequest;
import com.ijspast.journal.dto.response.ReviewerProfileDTO;
import com.ijspast.journal.entity.Manuscript;
import com.ijspast.journal.entity.Review;
import com.ijspast.journal.entity.Reviewer;
import com.ijspast.journal.entity.User;
import com.ijspast.journal.entity.UserRole;
import com.ijspast.journal.exception.DuplicateResourceException;
import com.ijspast.journal.exception.ResourceNotFoundException;
import com.ijspast.journal.exception.UnauthorizedException;
import com.ijspast.journal.mapper.UserMapper;
import com.ijspast.journal.repository.ManuscriptRepository;
import com.ijspast.journal.repository.ReviewRepository;
import com.ijspast.journal.repository.ReviewerRepository;
import com.ijspast.journal.repository.UserRepository;
import com.ijspast.journal.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ReviewerRepository reviewerRepository;
    private final ReviewRepository reviewRepository;
    private final ManuscriptRepository manuscriptRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserServiceImpl(UserRepository userRepository,
                           ReviewerRepository reviewerRepository,
                           ReviewRepository reviewRepository,
                           ManuscriptRepository manuscriptRepository,
                           UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           EmailService emailService) {
        this.userRepository = userRepository;
        this.reviewerRepository = reviewerRepository;
        this.reviewRepository = reviewRepository;
        this.manuscriptRepository = manuscriptRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewerProfileDTO> getReviewers(String search, String domain) {
        List<User> reviewers = userRepository.findByRoleOrderByNameAsc(UserRole.ROLE_REVIEWER);

        return reviewers.stream()
                .filter(r -> {
                    if (domain != null && !domain.isBlank()) {
                        if (r.getDomain() == null || !r.getDomain().equalsIgnoreCase(domain.trim())) {
                            return false;
                        }
                    }
                    if (search != null && !search.isBlank()) {
                        String s = search.toLowerCase();
                        boolean matchesName = r.getName().toLowerCase().contains(s);
                        boolean matchesEmail = r.getEmail().toLowerCase().contains(s);
                        boolean matchesInst = r.getInstitution() != null && r.getInstitution().toLowerCase().contains(s);
                        boolean matchesDom = r.getDomain() != null && r.getDomain().toLowerCase().contains(s);
                        return matchesName || matchesEmail || matchesInst || matchesDom;
                    }
                    return true;
                })
                .map(r -> {
                    long activeCount = manuscriptRepository.countActiveAssignmentsByReviewerId(r.getId());
                    long completedCount = manuscriptRepository.countCompletedReviewsByReviewerId(r.getId());
                    return userMapper.toReviewerProfileDTO(r, activeCount, completedCount);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> createReviewer(CreateReviewerRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An account with email " + email + " already exists in the system.");
        }

        String rawPassword = generateSecurePassword();
        String reviewerId = "rev_" + System.currentTimeMillis();

        User reviewer = new User(
                reviewerId,
                request.getName().trim(),
                email,
                passwordEncoder.encode(rawPassword),
                UserRole.ROLE_REVIEWER
        );
        reviewer.setInstitution(request.getInstitution().trim());
        reviewer.setDepartment("Academic Peer Review Board");
        reviewer.setDomain(request.getDomain().trim());
        reviewer.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        reviewer.setEnabled(true);
        reviewer.setAccountSetupCompleted(true);

        User savedReviewer = userRepository.save(reviewer);

        Reviewer reviewerEntity = new Reviewer();
        reviewerEntity.setId("rev_entity_" + System.currentTimeMillis());
        reviewerEntity.setUser(savedReviewer);
        reviewerEntity.setName(savedReviewer.getName());
        reviewerEntity.setEmail(savedReviewer.getEmail());
        reviewerEntity.setInstitution(savedReviewer.getInstitution());
        reviewerEntity.setDepartment(savedReviewer.getDepartment());
        reviewerEntity.setDomain(savedReviewer.getDomain());
        reviewerEntity.setPhone(savedReviewer.getPhone());
        reviewerRepository.save(reviewerEntity);

        // Send account creation and initial login credentials email
        emailService.sendReviewerAccountCreationEmail(
                email,
                savedReviewer.getName(),
                rawPassword
        );

        ReviewerProfileDTO profileDTO = userMapper.toReviewerProfileDTO(savedReviewer, 0, 0);

        Map<String, String> credentialsMap = new HashMap<>();
        credentialsMap.put("email", email);
        credentialsMap.put("password", rawPassword);

        Map<String, Object> result = new HashMap<>();
        result.put("reviewer", profileDTO);
        result.put("credentials", credentialsMap);
        return result;
    }

    @Override
    @Transactional
    public ReviewerProfileDTO updateReviewer(String id, UpdateReviewerRequest request) {
        User user = userRepository.findById(id)
                .orElseGet(() -> reviewerRepository.findById(id).map(Reviewer::getUser).orElse(null));
        if (user == null || user.getRole() != UserRole.ROLE_REVIEWER) {
            throw new ResourceNotFoundException("Reviewer not found with id: " + id);
        }

        String newEmail = request.getEmail().trim().toLowerCase();
        if (!newEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmailIgnoreCase(newEmail)) {
            throw new DuplicateResourceException("An account with email " + newEmail + " already exists in the system.");
        }

        user.setName(request.getName().trim());
        user.setEmail(newEmail);
        user.setInstitution(request.getInstitution().trim());
        if (request.getDepartment() != null && !request.getDepartment().isBlank()) {
            user.setDepartment(request.getDepartment().trim());
        }
        user.setDomain(request.getDomain().trim());
        user.setPhone(request.getPhone() != null && !request.getPhone().isBlank() ? request.getPhone().trim() : null);

        User savedUser = userRepository.save(user);

        // Update Reviewer entity in reviewers table
        Reviewer revEntity = reviewerRepository.findByUserId(savedUser.getId())
                .or(() -> reviewerRepository.findByEmailIgnoreCase(savedUser.getEmail()))
                .orElseGet(() -> {
                    Reviewer r = new Reviewer();
                    r.setId("rev_entity_" + System.currentTimeMillis());
                    r.setUser(savedUser);
                    return r;
                });
        revEntity.setName(savedUser.getName());
        revEntity.setEmail(savedUser.getEmail());
        revEntity.setInstitution(savedUser.getInstitution());
        revEntity.setDepartment(savedUser.getDepartment());
        revEntity.setDomain(savedUser.getDomain());
        revEntity.setPhone(savedUser.getPhone());
        reviewerRepository.save(revEntity);

        // Update reviewer name in any assigned manuscripts
        List<Manuscript> allManuscripts = manuscriptRepository.findAll();
        for (Manuscript m : allManuscripts) {
            boolean changed = false;
            if (m.getAssignedReviewer() != null && m.getAssignedReviewer().getId().equals(savedUser.getId())) {
                m.setAssignedReviewerName(savedUser.getName());
                changed = true;
            }
            if (m.getAssignedReviewer2() != null && m.getAssignedReviewer2().getId().equals(savedUser.getId())) {
                m.setAssignedReviewer2Name(savedUser.getName());
                changed = true;
            }
            if (changed) {
                manuscriptRepository.save(m);
            }
        }

        int activeCount = (int) manuscriptRepository.countActiveAssignmentsByReviewerId(savedUser.getId());
        int completedCount = (int) manuscriptRepository.countCompletedReviewsByReviewerId(savedUser.getId());
        return userMapper.toReviewerProfileDTO(savedUser, activeCount, completedCount);
    }

    @Override
    @Transactional
    public void deleteReviewer(String id) {
        User user = userRepository.findById(id)
                .orElseGet(() -> reviewerRepository.findById(id).map(Reviewer::getUser).orElse(null));
        if (user == null || user.getRole() != UserRole.ROLE_REVIEWER) {
            throw new ResourceNotFoundException("Reviewer not found with id: " + id);
        }

        // 1. Unassign reviewer from any manuscripts
        List<Manuscript> allManuscripts = manuscriptRepository.findAll();
        for (Manuscript m : allManuscripts) {
            boolean changed = false;
            if (m.getAssignedReviewer() != null && m.getAssignedReviewer().getId().equals(user.getId())) {
                m.setAssignedReviewer(null);
                m.setAssignedReviewerName(null);
                m.setAssignedReviewer1Ref(null);
                changed = true;
            }
            if (m.getAssignedReviewer2() != null && m.getAssignedReviewer2().getId().equals(user.getId())) {
                m.setAssignedReviewer2(null);
                m.setAssignedReviewer2Name(null);
                m.setAssignedReviewer2Ref(null);
                changed = true;
            }
            if (changed) {
                manuscriptRepository.save(m);
            }
        }

        // 2. Remove review reports submitted by this reviewer
        List<Review> reviews = reviewRepository.findByReviewerOrderBySubmittedAtDesc(user);
        if (reviews != null && !reviews.isEmpty()) {
            reviewRepository.deleteAll(reviews);
        }

        // 3. Remove from reviewers table
        reviewerRepository.findByUserId(user.getId()).ifPresent(reviewerRepository::delete);
        reviewerRepository.findByEmailIgnoreCase(user.getEmail()).ifPresent(reviewerRepository::delete);

        // 4. Remove user record
        userRepository.delete(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            throw new UnauthorizedException("No authenticated user found in session.");
        }

        if (authentication.getPrincipal() instanceof UserPrincipal principal) {
            return userRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Authenticated user could not be loaded from database."));
        }

        String email = authentication.getName();
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private String generateSecurePassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder("Rev@");
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
