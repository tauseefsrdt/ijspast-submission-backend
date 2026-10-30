package com.ijspast.journal.service;

import com.ijspast.journal.dto.request.AuthorRegistrationRequest;
import com.ijspast.journal.dto.request.CreateEditorRequest;
import com.ijspast.journal.dto.request.CreateReviewerRequest;
import com.ijspast.journal.dto.request.UpdateProfileRequest;
import com.ijspast.journal.dto.request.UpdateReviewerRequest;
import com.ijspast.journal.dto.response.ReviewerProfileDTO;
import com.ijspast.journal.dto.response.UserResponseDTO;
import com.ijspast.journal.entity.Author;
import com.ijspast.journal.repository.AuthorRepository;
import com.ijspast.journal.util.PhoneNumberUtils;
import com.ijspast.journal.entity.Manuscript;
import com.ijspast.journal.entity.Review;
import com.ijspast.journal.entity.Reviewer;
import com.ijspast.journal.entity.User;
import com.ijspast.journal.entity.UserProfile;
import com.ijspast.journal.entity.UserRole;
import com.ijspast.journal.exception.DuplicateResourceException;
import com.ijspast.journal.exception.ResourceNotFoundException;
import com.ijspast.journal.exception.UnauthorizedException;
import com.ijspast.journal.mapper.UserMapper;
import com.ijspast.journal.repository.ManuscriptRepository;
import com.ijspast.journal.repository.ReviewRepository;
import com.ijspast.journal.repository.ReviewerRepository;
import com.ijspast.journal.repository.UserProfileRepository;
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
    private final UserProfileRepository userProfileRepository;
    private final ReviewerRepository reviewerRepository;
    private final AuthorRepository authorRepository;
    private final ReviewRepository reviewRepository;
    private final ManuscriptRepository manuscriptRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final UserUniquenessValidationService userUniquenessValidationService;

    public UserServiceImpl(UserRepository userRepository,
                           UserProfileRepository userProfileRepository,
                           ReviewerRepository reviewerRepository,
                           AuthorRepository authorRepository,
                           ReviewRepository reviewRepository,
                           ManuscriptRepository manuscriptRepository,
                           UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           EmailService emailService,
                           UserUniquenessValidationService userUniquenessValidationService) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.reviewerRepository = reviewerRepository;
        this.authorRepository = authorRepository;
        this.reviewRepository = reviewRepository;
        this.manuscriptRepository = manuscriptRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.userUniquenessValidationService = userUniquenessValidationService;
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

        String rawPhone = request.getPhone();
        String normalizedPhone = null;
        if (rawPhone != null && !rawPhone.isBlank()) {
            normalizedPhone = userUniquenessValidationService.sanitizePhone(rawPhone);
            Optional<User> conflict = userUniquenessValidationService.findConflictingUserByPhone(normalizedPhone, null);
            if (conflict.isPresent()) {
                throw new DuplicateResourceException("This phone number ('" + rawPhone.trim() + "') is already associated with another account in the system.");
            }
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
        reviewer.setPhone(normalizedPhone);
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

        // Synchronize with enterprise 3NF user_profiles table
        UserProfile userProfile = userProfileRepository.findByUserId(savedReviewer.getId()).orElse(new UserProfile());
        if (userProfile.getId() == null) {
            userProfile.setId("profile_" + savedReviewer.getId());
            userProfile.setUser(savedReviewer);
        }
        userProfile.setInstitution(savedReviewer.getInstitution());
        userProfile.setDepartment(savedReviewer.getDepartment());
        userProfile.setDomainExpertise(savedReviewer.getDomain());
        userProfile.setPhone(savedReviewer.getPhone());
        userProfileRepository.save(userProfile);

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

        String rawPhone = request.getPhone();
        String normalizedPhone = null;
        if (rawPhone != null && !rawPhone.isBlank()) {
            normalizedPhone = userUniquenessValidationService.sanitizePhone(rawPhone);
            Optional<User> conflict = userUniquenessValidationService.findConflictingUserByPhone(normalizedPhone, user.getId());
            if (conflict.isPresent()) {
                throw new DuplicateResourceException("This phone number ('" + rawPhone.trim() + "') is already associated with another account in the system.");
            }
        }

        user.setName(request.getName().trim());
        user.setEmail(newEmail);
        user.setInstitution(request.getInstitution().trim());
        if (request.getDepartment() != null && !request.getDepartment().isBlank()) {
            user.setDepartment(request.getDepartment().trim());
        }
        user.setDomain(request.getDomain().trim());
        user.setPhone(normalizedPhone);

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

        // Update enterprise 3NF user_profiles table
        UserProfile userProfile = userProfileRepository.findByUserId(savedUser.getId()).orElse(new UserProfile());
        if (userProfile.getId() == null) {
            userProfile.setId("profile_" + savedUser.getId());
            userProfile.setUser(savedUser);
        }
        userProfile.setInstitution(savedUser.getInstitution());
        userProfile.setDepartment(savedUser.getDepartment());
        userProfile.setDomainExpertise(savedUser.getDomain());
        userProfile.setPhone(savedUser.getPhone());
        userProfileRepository.save(userProfile);

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

    @Override
    @Transactional
    public UserResponseDTO createEditor(CreateEditorRequest request) {
        userUniquenessValidationService.validateNewEditor(request.getEmail(), request.getPhone());
        String email = userUniquenessValidationService.sanitizeEmail(request.getEmail());
        String normalizedPhone = userUniquenessValidationService.sanitizePhone(request.getPhone());

        String editorId = "editor_" + System.currentTimeMillis();
        User editor = new User(
                editorId,
                request.getName().trim(),
                email,
                passwordEncoder.encode(request.getPassword()),
                UserRole.ROLE_EDITOR
        );
        editor.setInstitution(request.getInstitution() != null ? request.getInstitution().trim() : "Office of the Editor");
        editor.setDepartment(request.getDepartment() != null ? request.getDepartment().trim() : "Editorial Board");
        editor.setPhone(normalizedPhone);
        editor.setEnabled(true);
        editor.setAccountSetupCompleted(true);

        User saved = userRepository.save(editor);

        UserProfile profile = new UserProfile();
        profile.setId("profile_" + saved.getId());
        profile.setUser(saved);
        profile.setInstitution(saved.getInstitution());
        profile.setDepartment(saved.getDepartment());
        profile.setCityCountry("Lucknow, India");
        profile.setDomainExpertise("Editorial Board");
        profile.setPhone(saved.getPhone());
        userProfileRepository.save(profile);

        return userMapper.toUserResponseDTO(saved);
    }

    @Override
    @Transactional
    public UserResponseDTO updateProfile(String userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        userUniquenessValidationService.validateUpdateUser(userId, request.getEmail(), request.getPhone());

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            user.setEmail(userUniquenessValidationService.sanitizeEmail(request.getEmail()));
        }
        if (request.getPhone() != null) {
            String normalized = userUniquenessValidationService.sanitizePhone(request.getPhone());
            user.setPhone(normalized);
        }
        if (request.getInstitution() != null) {
            user.setInstitution(request.getInstitution().trim());
        }
        if (request.getDepartment() != null) {
            user.setDepartment(request.getDepartment().trim());
        }
        if (request.getDomain() != null) {
            user.setDomain(request.getDomain().trim());
        }

        User saved = userRepository.save(user);

        UserProfile userProfile = userProfileRepository.findByUserId(saved.getId()).orElseGet(() -> {
            UserProfile up = new UserProfile();
            up.setId("profile_" + saved.getId());
            up.setUser(saved);
            return up;
        });
        userProfile.setInstitution(saved.getInstitution());
        userProfile.setDepartment(saved.getDepartment());
        userProfile.setPhone(saved.getPhone());
        if (request.getCityCountry() != null && !request.getCityCountry().isBlank()) {
            userProfile.setCityCountry(request.getCityCountry().trim());
        }
        if (request.getOrcid() != null && !request.getOrcid().isBlank()) {
            userProfile.setOrcid(request.getOrcid().trim());
        }
        if (saved.getDomain() != null) {
            userProfile.setDomainExpertise(saved.getDomain());
        }
        userProfileRepository.save(userProfile);

        if (saved.getRole() == UserRole.ROLE_AUTHOR) {
            authorRepository.findByUserId(saved.getId())
                    .or(() -> authorRepository.findByEmailIgnoreCase(saved.getEmail()))
                    .ifPresent(a -> {
                        a.setName(saved.getName());
                        a.setEmail(saved.getEmail());
                        a.setPhone(saved.getPhone());
                        if (saved.getInstitution() != null) a.setInstitution(saved.getInstitution());
                        if (saved.getDepartment() != null) a.setDepartment(saved.getDepartment());
                        if (request.getCityCountry() != null && !request.getCityCountry().isBlank()) {
                            a.setCityCountry(request.getCityCountry().trim());
                        }
                        if (request.getOrcid() != null && !request.getOrcid().isBlank()) {
                            a.setOrcid(request.getOrcid().trim());
                        }
                        authorRepository.save(a);
                    });
        }

        if (saved.getRole() == UserRole.ROLE_REVIEWER) {
            reviewerRepository.findByUserId(saved.getId())
                    .or(() -> reviewerRepository.findByEmailIgnoreCase(saved.getEmail()))
                    .ifPresent(r -> {
                        r.setName(saved.getName());
                        r.setEmail(saved.getEmail());
                        r.setPhone(saved.getPhone());
                        if (saved.getInstitution() != null) r.setInstitution(saved.getInstitution());
                        if (saved.getDepartment() != null) r.setDepartment(saved.getDepartment());
                        if (saved.getDomain() != null) r.setDomain(saved.getDomain());
                        reviewerRepository.save(r);
                    });
        }

        return userMapper.toUserResponseDTO(saved);
    }

    @Override
    @Transactional
    public UserResponseDTO registerAuthor(AuthorRegistrationRequest request) {
        userUniquenessValidationService.validateNewAuthor(request.getEmail(), request.getPhone());
        String email = userUniquenessValidationService.sanitizeEmail(request.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An account with email '" + request.getEmail().trim() + "' already exists in the system.");
        }
        String normalizedPhone = userUniquenessValidationService.sanitizePhone(request.getPhone());

        String authorId = "author_" + System.currentTimeMillis();
        User authorUser = new User(
                authorId,
                request.getName().trim(),
                email,
                passwordEncoder.encode(request.getPassword()),
                UserRole.ROLE_AUTHOR
        );
        authorUser.setInstitution(request.getInstitution().trim());
        authorUser.setDepartment(request.getDepartment());
        authorUser.setPhone(normalizedPhone);
        authorUser.setEnabled(true);
        authorUser.setAccountSetupCompleted(true);
        User savedUser = userRepository.save(authorUser);

        Author author = new Author();
        author.setId("auth_" + System.currentTimeMillis());
        author.setUser(savedUser);
        author.setName(savedUser.getName());
        author.setEmail(email);
        author.setInstitution(savedUser.getInstitution());
        author.setDepartment(savedUser.getDepartment());
        author.setCityCountry(request.getCityCountry().trim());
        author.setOrcid(request.getOrcid().trim());
        author.setPhone(normalizedPhone);
        authorRepository.save(author);

        UserProfile profile = new UserProfile();
        profile.setId("profile_" + savedUser.getId());
        profile.setUser(savedUser);
        profile.setInstitution(savedUser.getInstitution());
        profile.setDepartment(savedUser.getDepartment());
        profile.setCityCountry(request.getCityCountry().trim());
        profile.setOrcid(request.getOrcid().trim());
        profile.setPhone(normalizedPhone);
        userProfileRepository.save(profile);

        return userMapper.toUserResponseDTO(savedUser);
    }
}
