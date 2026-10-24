package com.ijspast.journal.service;

import com.ijspast.journal.dto.request.CreateReviewerRequest;
import com.ijspast.journal.dto.response.ReviewerProfileDTO;
import com.ijspast.journal.entity.User;
import com.ijspast.journal.entity.UserRole;
import com.ijspast.journal.exception.DuplicateResourceException;
import com.ijspast.journal.exception.ResourceNotFoundException;
import com.ijspast.journal.exception.UnauthorizedException;
import com.ijspast.journal.mapper.UserMapper;
import com.ijspast.journal.repository.ManuscriptRepository;
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
    private final ManuscriptRepository manuscriptRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserServiceImpl(UserRepository userRepository,
                           ManuscriptRepository manuscriptRepository,
                           UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           EmailService emailService) {
        this.userRepository = userRepository;
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

        // Send invitation and credentials email
        emailService.sendReviewerInvitationEmail(
                email,
                savedReviewer.getName(),
                rawPassword,
                "IJSPAST-PEER-REVIEW",
                "General Peer Review Directory Appointment",
                14
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
