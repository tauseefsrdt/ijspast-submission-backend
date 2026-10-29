package com.ijspast.journal.service;

import com.ijspast.journal.dto.request.AssignReviewerRequest;
import com.ijspast.journal.dto.request.EditorialDecisionRequest;
import com.ijspast.journal.dto.request.ManuscriptSubmissionRequest;
import com.ijspast.journal.dto.request.ReassignReviewerRequest;
import com.ijspast.journal.dto.request.RevisionSubmissionRequest;
import com.ijspast.journal.dto.request.SendMessageRequest;
import com.ijspast.journal.dto.response.AuthorTableDTO;
import com.ijspast.journal.dto.response.CommunicationMessageDTO;
import com.ijspast.journal.dto.response.CommunicationThreadDTO;
import com.ijspast.journal.dto.response.DispatchedEmailDTO;
import com.ijspast.journal.dto.response.ManuscriptResponseDTO;
import com.ijspast.journal.dto.response.ReviewerTableDTO;
import com.ijspast.journal.dto.response.SubmissionResponseDTO;
import com.ijspast.journal.entity.*;
import com.ijspast.journal.exception.BadRequestException;
import com.ijspast.journal.exception.ResourceNotFoundException;
import com.ijspast.journal.exception.UnauthorizedException;
import com.ijspast.journal.mapper.ManuscriptMapper;
import com.ijspast.journal.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ManuscriptServiceImpl implements ManuscriptService {

    private static final Logger log = LoggerFactory.getLogger(ManuscriptServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ManuscriptRepository manuscriptRepository;
    private final UserRepository userRepository;
    private final AuthorRepository authorRepository;
    private final ReviewerRepository reviewerRepository;
    private final ReviewRepository reviewRepository;
    private final ManuscriptFileRepository fileRepository;
    private final ReviewAssignmentRepository reviewAssignmentRepository;
    private final UserProfileRepository userProfileRepository;
    private final ManuscriptRevisionRepository manuscriptRevisionRepository;
    private final EditorialDecisionRepository editorialDecisionRepository;
    private final ManuscriptMapper manuscriptMapper;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final ManuscriptCommunicationRepository communicationRepository;

    @Value("${app.editor.email:${EDITOR_EMAIL:}}")
    private String editorEmail;

    @Value("${app.editor.name:${EDITOR_NAME:Prof. (Dr.) Vijay Tiwari (Editor-in-Chief)}}")
    private String editorName;

    public ManuscriptServiceImpl(ManuscriptRepository manuscriptRepository,
                                 UserRepository userRepository,
                                 AuthorRepository authorRepository,
                                 ReviewerRepository reviewerRepository,
                                 ReviewRepository reviewRepository,
                                 ManuscriptFileRepository fileRepository,
                                 ReviewAssignmentRepository reviewAssignmentRepository,
                                 UserProfileRepository userProfileRepository,
                                 ManuscriptRevisionRepository manuscriptRevisionRepository,
                                 EditorialDecisionRepository editorialDecisionRepository,
                                 ManuscriptMapper manuscriptMapper,
                                 FileStorageService fileStorageService,
                                 EmailService emailService,
                                 PasswordEncoder passwordEncoder,
                                 ManuscriptCommunicationRepository communicationRepository) {
        this.manuscriptRepository = manuscriptRepository;
        this.userRepository = userRepository;
        this.authorRepository = authorRepository;
        this.reviewerRepository = reviewerRepository;
        this.reviewRepository = reviewRepository;
        this.fileRepository = fileRepository;
        this.reviewAssignmentRepository = reviewAssignmentRepository;
        this.userProfileRepository = userProfileRepository;
        this.manuscriptRevisionRepository = manuscriptRevisionRepository;
        this.editorialDecisionRepository = editorialDecisionRepository;
        this.manuscriptMapper = manuscriptMapper;
        this.fileStorageService = fileStorageService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.communicationRepository = communicationRepository;
    }

    private String getAuthenticatedUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return auth.getName();
    }

    private User getAuthenticatedUser() {
        String email = getAuthenticatedUserEmail();
        if (email == null) {
            throw new UnauthorizedException("Authentication is required.");
        }
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));
    }

    @Override
    @Transactional
    public SubmissionResponseDTO submitManuscript(ManuscriptSubmissionRequest request, MultipartFile multipartFile) {
        String email = request.getAuthor().getEmail().trim().toLowerCase();
        String authorName = request.getAuthor().getName().trim();

        // 1. Check if author user already exists
        Optional<User> existingUserOpt = userRepository.findByEmailIgnoreCase(email);
        User authorUser;
        String rawPassword = null;
        boolean isNewUser = false;

        if (existingUserOpt.isPresent()) {
            authorUser = existingUserOpt.get();
            // Retain author credentials completely unchanged.
            // Update optional profile details if supplied.
            if (request.getAuthor().getInstitution() != null && !request.getAuthor().getInstitution().isBlank()) {
                authorUser.setInstitution(request.getAuthor().getInstitution().trim());
            }
            if (request.getAuthor().getDepartment() != null && !request.getAuthor().getDepartment().isBlank()) {
                authorUser.setDepartment(request.getAuthor().getDepartment().trim());
            }
            userRepository.save(authorUser);
        } else {
            isNewUser = true;
            rawPassword = generateAuthorPassword();
            String authorId = "author_" + System.currentTimeMillis();
            authorUser = new User(
                    authorId,
                    authorName,
                    email,
                    passwordEncoder.encode(rawPassword),
                    UserRole.ROLE_AUTHOR
            );
            authorUser.setInstitution(request.getAuthor().getInstitution().trim());
            authorUser.setDepartment(request.getAuthor().getDepartment());
            authorUser.setEnabled(true);
            authorUser.setAccountSetupCompleted(true);
            authorUser = userRepository.save(authorUser);
        }

        // 2. Check whether submission already exists for this author (Correction & Resubmission)
        Optional<Manuscript> existingManuscriptOpt = Optional.empty();
        if (!isNewUser) {
            existingManuscriptOpt = findExistingManuscriptForAuthor(request, email);
        }

        if (existingManuscriptOpt.isPresent()) {
            // ====================================================================
            // WORKFLOW: Correction and Resubmission
            // - Update existing submission record in database
            // - Do not create a new submission ID or duplicate records
            // - Do not generate new login credentials or send account email
            // - Existing credentials remain completely unchanged
            // ====================================================================
            Manuscript existing = existingManuscriptOpt.get();
            log.info("Author [{}] resubmitting corrections for existing manuscript [{}]", email, existing.getId());

            existing.setTitle(request.getTitle().trim());
            existing.setDomain(request.getDomain().trim());
            existing.setAbstractText(request.getAbstractText().trim());
            existing.setKeywords(request.getKeywords().trim());
            existing.setCoauthors(request.getCoauthors() != null ? request.getCoauthors().trim() : null);

            existing.setAuthorName(authorName);
            existing.setAuthorInstitution(request.getAuthor().getInstitution().trim());
            existing.setAuthorDepartment(request.getAuthor().getDepartment());
            existing.setAuthorCityCountry(request.getAuthor().getCityCountry().trim());
            existing.setAuthorOrcid(request.getAuthor().getOrcid().trim());

            Author authorEntity = getOrCreateAuthorEntity(authorUser, authorName, email,
                    request.getAuthor().getInstitution().trim(),
                    request.getAuthor().getDepartment(),
                    request.getAuthor().getCityCountry().trim(),
                    request.getAuthor().getOrcid().trim());
            existing.setAuthor(authorEntity);

            // Handle file upload if a new/revised file is provided
            boolean fileUploaded = false;
            String fileName = null;
            long fileSize = 0L;
            String fileType = "application/pdf";
            String fileKey = null;

            if (multipartFile != null && !multipartFile.isEmpty()) {
                fileName = multipartFile.getOriginalFilename();
                fileSize = multipartFile.getSize();
                fileType = multipartFile.getContentType() != null ? multipartFile.getContentType() : "application/pdf";
                fileKey = fileStorageService.storeFile(multipartFile);
                fileUploaded = true;
            } else if (request.getFile() != null && request.getFile().getName() != null && !request.getFile().getName().isBlank()) {
                fileName = request.getFile().getName();
                fileSize = request.getFile().getSize() > 0 ? request.getFile().getSize() : 3500000L;
                fileType = request.getFile().getType() != null ? request.getFile().getType() : "application/pdf";
                fileKey = fileStorageService.storeFile(
                        ("Simulated corrected manuscript payload for " + existing.getId()).getBytes(), fileName
                );
                fileUploaded = true;
            }

            if (fileUploaded && fileKey != null) {
                int nextVersion = existing.getFiles().size() + 1;
                String fileId = "file_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                ManuscriptFile manuscriptFile = new ManuscriptFile(fileId, fileName, fileSize, fileType, fileKey, nextVersion, true);
                existing.addFile(manuscriptFile);
            }

            // Append status history entry for author corrections
            String nowStr = LocalDateTime.now().format(DATE_TIME_FORMAT);
            StatusHistoryItem historyItem = new StatusHistoryItem(
                    "hist_" + System.currentTimeMillis(),
                    existing.getStatus(),
                    nowStr,
                    authorName,
                    "Author",
                    fileUploaded
                            ? "Manuscript details and document updated by author (Correction / Resubmission)."
                            : "Manuscript details updated by author (Correction / Resubmission)."
            );
            existing.addHistoryItem(historyItem);

            Manuscript savedManuscript = manuscriptRepository.save(existing);
            ManuscriptResponseDTO manuscriptDTO = manuscriptMapper.toDTO(savedManuscript, false);

            SubmissionResponseDTO.GeneratedCredentials credentials = new SubmissionResponseDTO.GeneratedCredentials(
                    email,
                    null // No new credentials generated; existing login remains active
            );

            SubmissionResponseDTO response = new SubmissionResponseDTO(
                    savedManuscript.getId(),
                    credentials,
                    null, // No credentials email sent on resubmission
                    authorName,
                    manuscriptDTO
            );
            response.setResubmission(true);
            response.setMessage("Existing submission record updated with author corrections. Existing credentials remain unchanged.");
            return response;
        }

        // ========================================================================
        // WORKFLOW: First Submission (or New Manuscript by existing author)
        // ========================================================================
        long count = manuscriptRepository.countTotalManuscripts() + 1;
        String currentYear = String.valueOf(LocalDate.now().getYear());
        String manuscriptId = String.format("IJSPAST-%s-%04d", currentYear, count);

        // Create Manuscript entity
        Manuscript manuscript = new Manuscript();
        manuscript.setId(manuscriptId);
        manuscript.setTitle(request.getTitle().trim());
        manuscript.setDomain(request.getDomain().trim());
        manuscript.setAbstractText(request.getAbstractText().trim());
        manuscript.setKeywords(request.getKeywords().trim());
        manuscript.setCoauthors(request.getCoauthors() != null ? request.getCoauthors().trim() : null);

        manuscript.setAuthorUser(authorUser);
        manuscript.setAuthorName(authorName);
        manuscript.setAuthorEmail(email);
        manuscript.setAuthorInstitution(request.getAuthor().getInstitution().trim());
        manuscript.setAuthorDepartment(request.getAuthor().getDepartment());
        manuscript.setAuthorCityCountry(request.getAuthor().getCityCountry().trim());
        manuscript.setAuthorOrcid(request.getAuthor().getOrcid().trim());

        Author authorEntity = getOrCreateAuthorEntity(authorUser, authorName, email,
                request.getAuthor().getInstitution().trim(),
                request.getAuthor().getDepartment(),
                request.getAuthor().getCityCountry().trim(),
                request.getAuthor().getOrcid().trim());
        manuscript.setAuthor(authorEntity);

        // Sync normalized UserProfile
        String profId = "prof_" + authorUser.getId();
        UserProfile userProfile = userProfileRepository.findByUser(authorUser)
                .orElse(new UserProfile(profId, authorUser, request.getAuthor().getInstitution().trim(),
                        request.getAuthor().getDepartment(), request.getAuthor().getCityCountry().trim(),
                        request.getAuthor().getOrcid().trim(), null, authorUser.getPhone()));
        userProfile.setInstitution(request.getAuthor().getInstitution().trim());
        userProfile.setDepartment(request.getAuthor().getDepartment());
        userProfile.setCityCountry(request.getAuthor().getCityCountry().trim());
        userProfile.setOrcid(request.getAuthor().getOrcid().trim());
        userProfileRepository.save(userProfile);

        manuscript.setStatus("Submitted");
        String todayStr = LocalDate.now().toString();
        manuscript.setSubmissionDate(todayStr);
        manuscript.setSimilarityScore(4 + new Random().nextInt(5)); // Under 10% benchmark

        // Handle manuscript file upload
        String fileName = "IJSPAST_Manuscript_" + manuscriptId + ".pdf";
        long fileSize = 3500000L;
        String fileType = "application/pdf";
        String fileKey;

        if (multipartFile != null && !multipartFile.isEmpty()) {
            fileName = multipartFile.getOriginalFilename();
            fileSize = multipartFile.getSize();
            fileType = multipartFile.getContentType() != null ? multipartFile.getContentType() : "application/pdf";
            fileKey = fileStorageService.storeFile(multipartFile);
        } else if (request.getFile() != null && request.getFile().getName() != null) {
            fileName = request.getFile().getName();
            fileSize = request.getFile().getSize() > 0 ? request.getFile().getSize() : 3500000L;
            fileType = request.getFile().getType() != null ? request.getFile().getType() : "application/pdf";
            fileKey = fileStorageService.storeFile(
                    ("Simulated manuscript payload for " + manuscriptId).getBytes(), fileName
            );
        } else {
            fileKey = fileStorageService.storeFile(
                    ("Simulated manuscript payload for " + manuscriptId).getBytes(), fileName
            );
        }

        String fileId = "file_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        ManuscriptFile manuscriptFile = new ManuscriptFile(fileId, fileName, fileSize, fileType, fileKey, 1, false);
        manuscript.addFile(manuscriptFile);

        // Initial History Item
        String nowStr = LocalDateTime.now().format(DATE_TIME_FORMAT);
        StatusHistoryItem initialHistory = new StatusHistoryItem(
                "hist_" + System.currentTimeMillis(),
                "Submitted",
                nowStr,
                authorName,
                "Author",
                "New manuscript submitted with confirmed declaration."
        );
        manuscript.addHistoryItem(initialHistory);

        // Save Manuscript
        Manuscript savedManuscript = manuscriptRepository.save(manuscript);

        // Send credentials email ONLY on first submission (new user)
        DispatchedEmailDTO dispatchedEmail = null;
        if (isNewUser) {
            dispatchedEmail = emailService.sendAuthorCredentialsEmail(
                    email,
                    authorName,
                    rawPassword,
                    manuscriptId,
                    savedManuscript.getTitle()
            );
        }

        // Construct Response
        SubmissionResponseDTO.GeneratedCredentials credentials = new SubmissionResponseDTO.GeneratedCredentials(
                email,
                isNewUser ? rawPassword : null
        );

        ManuscriptResponseDTO manuscriptDTO = manuscriptMapper.toDTO(savedManuscript, false);

        SubmissionResponseDTO response = new SubmissionResponseDTO(
                manuscriptId,
                credentials,
                dispatchedEmail,
                authorName,
                manuscriptDTO
        );
        response.setResubmission(false);
        response.setMessage(isNewUser
                ? "Manuscript submitted successfully. Login credentials generated and emailed."
                : "New manuscript submitted successfully under existing author account.");
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManuscriptResponseDTO> getAuthorManuscripts(String status, String search) {
        User author = getAuthenticatedUser();
        List<Manuscript> list = manuscriptRepository.findByAuthorEmailIgnoreCaseOrderByCreatedAtDesc(author.getEmail());

        return filterManuscripts(list, status, null, search, false, true);
    }

    @Override
    @Transactional(readOnly = true)
    public ManuscriptResponseDTO getAuthorManuscriptById(String id) {
        User author = getAuthenticatedUser();
        Manuscript manuscript = manuscriptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manuscript not found: " + id));

        // Enforce ownership check
        if (!manuscript.getAuthorEmail().equalsIgnoreCase(author.getEmail())) {
            throw new UnauthorizedException("You do not have access to view this manuscript.");
        }

        return manuscriptMapper.toAuthorDTO(manuscript);
    }

    @Override
    @Transactional
    public ManuscriptResponseDTO submitAuthorRevision(String id, RevisionSubmissionRequest request, MultipartFile multipartFile) {
        User author = getAuthenticatedUser();
        Manuscript manuscript = manuscriptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manuscript not found: " + id));

        if (!manuscript.getAuthorEmail().equalsIgnoreCase(author.getEmail())) {
            throw new UnauthorizedException("You do not have permission to submit revisions for this manuscript.");
        }

        // Store revised file
        String fileName = "Revised_Manuscript_" + id + ".pdf";
        long fileSize = 3600000L;
        String fileType = "application/pdf";
        String fileKey;

        if (multipartFile != null && !multipartFile.isEmpty()) {
            fileName = multipartFile.getOriginalFilename();
            fileSize = multipartFile.getSize();
            fileType = multipartFile.getContentType() != null ? multipartFile.getContentType() : "application/pdf";
            fileKey = fileStorageService.storeFile(multipartFile);
        } else if (request.getFile() != null && request.getFile().getName() != null) {
            fileName = request.getFile().getName();
            fileSize = request.getFile().getSize();
            fileType = request.getFile().getType();
            fileKey = fileStorageService.storeFile(("Revised payload for " + id).getBytes(), fileName);
        } else {
            fileKey = fileStorageService.storeFile(("Revised payload for " + id).getBytes(), fileName);
        }

        int revisedVersion = (int) manuscript.getFiles().stream().filter(ManuscriptFile::isRevised).count() + 1;
        String fileId = "file_rev_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        ManuscriptFile revisedFile = new ManuscriptFile(fileId, fileName, fileSize, fileType, fileKey, revisedVersion, true);
        manuscript.addFile(revisedFile);

        // Update Manuscript Status
        manuscript.setStatus("Revision Submitted");
        manuscript.setRevisionForwardedToReviewer(false);
        manuscript.setStage2EnabledByEditor(false);
        manuscript.setReviewerReassignedAfterRevision(false);
        manuscript.setAuthorRevisionResponse(request.getResponse().trim());

        String nowStr = LocalDateTime.now().format(DATE_TIME_FORMAT);
        StatusHistoryItem hist = new StatusHistoryItem(
                "hist_" + System.currentTimeMillis(),
                "Revision Submitted",
                nowStr,
                author.getName(),
                "Author",
                "Revised manuscript uploaded: " + fileName + ". Submitted to Editor for assessment. Author remarks: " + request.getResponse().trim()
        );
        manuscript.addHistoryItem(hist);

        Manuscript saved = manuscriptRepository.save(manuscript);

        // Record normalized ManuscriptRevision
        String revId = "rev_" + saved.getId() + "_v" + revisedVersion;
        ManuscriptRevision revisionRecord = new ManuscriptRevision(
                revId,
                saved,
                revisedVersion,
                revisedFile,
                request.getResponse().trim()
        );
        manuscriptRevisionRepository.save(revisionRecord);

        // Notify Editor ONLY (Author resubmissions are reviewed by Editor before explicit forwarding to Reviewer)
        String targetEditorEmail = (editorEmail != null && !editorEmail.isBlank()) ? editorEmail : "";
        String targetEditorName = (editorName != null && !editorName.isBlank()) ? editorName : "Editor-in-Chief";
        emailService.sendRevisionSubmittedEmail(
                targetEditorEmail,
                targetEditorName,
                "Editor",
                saved.getId(),
                saved.getTitle(),
                request.getResponse().trim()
        );

        return manuscriptMapper.toAuthorDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManuscriptResponseDTO> getEditorManuscripts(String status, String domain, String search) {
        List<Manuscript> list = manuscriptRepository.findAllByOrderByCreatedAtDesc();
        return filterManuscripts(list, status, domain, search, false);
    }

    @Override
    @Transactional(readOnly = true)
    public ManuscriptResponseDTO getEditorManuscriptById(String id) {
        Manuscript manuscript = manuscriptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manuscript not found: " + id));
        return manuscriptMapper.toDTO(manuscript, false);
    }

    private int calculateRevisionCount(Manuscript manuscript) {
        int count = 0;
        if (manuscript.getFiles() != null) {
            count = (int) manuscript.getFiles().stream().filter(ManuscriptFile::isRevised).count();
        }
        if (manuscript.getRevisions() != null && manuscript.getRevisions().size() > count) {
            count = manuscript.getRevisions().size();
        }
        if (manuscript.getHistory() != null) {
            int histCount = (int) manuscript.getHistory().stream()
                    .filter(h -> "Revision Submitted".equalsIgnoreCase(h.getStatus()))
                    .count();
            if (histCount > count) {
                count = histCount;
            }
        }
        return count;
    }

    @Override
    @Transactional
    public ManuscriptResponseDTO assignReviewer(String manuscriptId, AssignReviewerRequest request) {
        User editor = getAuthenticatedUser();
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ResourceNotFoundException("Manuscript not found: " + manuscriptId));

        if (calculateRevisionCount(manuscript) >= 2) {
            throw new BadRequestException("Reviewers cannot be reassigned: Author has submitted the revised manuscript twice. At this stage, the Editorial Board must make a final decision: Accept or Reject.");
        }

        User reviewer1 = userRepository.findById(request.getReviewerId())
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer 1 not found: " + request.getReviewerId()));

        if (reviewer1.getRole() != UserRole.ROLE_REVIEWER) {
            throw new BadRequestException("Reviewer 1 is not registered as a Reviewer.");
        }

        User reviewer2 = null;
        if (request.getReviewer2Id() != null && !request.getReviewer2Id().isBlank()) {
            if (request.getReviewer2Id().equals(reviewer1.getId())) {
                throw new BadRequestException("Paper must be assigned to two distinct reviewers.");
            }
            reviewer2 = userRepository.findById(request.getReviewer2Id())
                    .orElseThrow(() -> new ResourceNotFoundException("Reviewer 2 not found: " + request.getReviewer2Id()));
            if (reviewer2.getRole() != UserRole.ROLE_REVIEWER) {
                throw new BadRequestException("Reviewer 2 is not registered as a Reviewer.");
            }
        }

        int deadlineDays = request.getDeadlineDays() > 0 ? request.getDeadlineDays() : 15;
        LocalDate assignedDateObj = LocalDate.now();
        LocalDate reviewDeadlineObj = assignedDateObj.plusDays(deadlineDays);
        String deadlineStr = reviewDeadlineObj.toString();
        String dateStr = assignedDateObj.toString();

        manuscript.setStatus("Under Review");
        manuscript.setAssignedReviewer(reviewer1);
        manuscript.setAssignedReviewerName(reviewer1.getName());
        Reviewer reviewer1Ref = getOrCreateReviewerEntity(reviewer1);
        manuscript.setAssignedReviewer1Ref(reviewer1Ref);
        manuscript.setAssignedReviewer1Date(assignedDateObj);
        manuscript.setAssignedReviewer1Deadline(reviewDeadlineObj);

        if (reviewer2 != null) {
            manuscript.setAssignedReviewer2(reviewer2);
            manuscript.setAssignedReviewer2Name(reviewer2.getName());
            Reviewer reviewer2Ref = getOrCreateReviewerEntity(reviewer2);
            manuscript.setAssignedReviewer2Ref(reviewer2Ref);
            manuscript.setAssignedReviewer2Date(assignedDateObj);
            manuscript.setAssignedReviewer2Deadline(reviewDeadlineObj);
        }

        manuscript.setAssignedDate(dateStr);
        manuscript.setReviewDeadline(deadlineStr);
        if ("Revision Submitted".equalsIgnoreCase(manuscript.getStatus())
                || "Revision Requested".equalsIgnoreCase(manuscript.getStatus())
                || (manuscript.getAuthorRevisionResponse() != null && !manuscript.getAuthorRevisionResponse().isBlank())
                || (manuscript.getFiles() != null && manuscript.getFiles().stream().anyMatch(ManuscriptFile::isRevised))) {
            manuscript.setReviewerReassignedAfterRevision(true);
        }

        String nowStr = LocalDateTime.now().format(DATE_TIME_FORMAT);
        String historyComment = (reviewer2 != null)
                ? String.format("Assigned to Reviewer 1 (%s) and Reviewer 2 (%s) for peer review evaluation. 15-day deadline: %s.",
                        reviewer1.getName(), reviewer2.getName(), deadlineStr)
                : String.format("Assigned to %s for peer review evaluation. 15-day deadline: %s.",
                        reviewer1.getName(), deadlineStr);

        StatusHistoryItem historyItem = new StatusHistoryItem(
                "hist_" + System.currentTimeMillis(),
                "Under Review",
                nowStr,
                editor.getName(),
                "Editor",
                historyComment
        );
        manuscript.addHistoryItem(historyItem);

        Manuscript saved = manuscriptRepository.save(manuscript);

        // Sync normalized ReviewAssignment records
        String asgnId1 = "asgn_" + saved.getId() + "_" + reviewer1.getId() + "_r1";
        ReviewAssignment assignment1 = reviewAssignmentRepository
                .findByManuscriptAndReviewerAndRoundNumber(saved, reviewer1, 1)
                .orElse(new ReviewAssignment(asgnId1, saved, reviewer1, 1, assignedDateObj, reviewDeadlineObj, request.getNotes(), 1));
        assignment1.setSlotNumber(1);
        assignment1.setAssignedDate(assignedDateObj);
        assignment1.setReviewDeadline(reviewDeadlineObj);
        assignment1.setNotes(request.getNotes());
        assignment1.setAssignmentStatus("ASSIGNED");
        reviewAssignmentRepository.save(assignment1);

        if (reviewer2 != null) {
            String asgnId2 = "asgn_" + saved.getId() + "_" + reviewer2.getId() + "_r1";
            ReviewAssignment assignment2 = reviewAssignmentRepository
                    .findByManuscriptAndReviewerAndRoundNumber(saved, reviewer2, 1)
                    .orElse(new ReviewAssignment(asgnId2, saved, reviewer2, 1, assignedDateObj, reviewDeadlineObj, request.getNotes(), 2));
            assignment2.setSlotNumber(2);
            assignment2.setAssignedDate(assignedDateObj);
            assignment2.setReviewDeadline(reviewDeadlineObj);
            assignment2.setNotes(request.getNotes());
            assignment2.setAssignmentStatus("ASSIGNED");
            reviewAssignmentRepository.save(assignment2);
        }

        // Send peer review assignment email notifications (No credentials generated or resent)
        emailService.sendReviewerAssignmentEmail(
                reviewer1.getEmail(),
                reviewer1.getName(),
                saved.getId(),
                saved.getTitle(),
                deadlineDays,
                request.getNotes()
        );

        if (reviewer2 != null) {
            emailService.sendReviewerAssignmentEmail(
                    reviewer2.getEmail(),
                    reviewer2.getName(),
                    saved.getId(),
                    saved.getTitle(),
                    deadlineDays,
                    request.getNotes()
            );
        }

        return manuscriptMapper.toDTO(saved, false);
    }

    @Override
    @Transactional
    public ManuscriptResponseDTO enableStage2Review(String manuscriptId) {
        User editor = getAuthenticatedUser();
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ResourceNotFoundException("Manuscript not found: " + manuscriptId));

        if (calculateRevisionCount(manuscript) >= 2) {
            throw new BadRequestException("Stage 2 review cannot be enabled: Author has submitted the revised manuscript twice. At this stage, the Editorial Board must make a final decision: Accept or Reject.");
        }

        if (manuscript.getAssignedReviewer() == null && manuscript.getAssignedReviewer2() == null) {
            throw new BadRequestException("No reviewer is assigned to this manuscript. Assign a reviewer before enabling Stage 2.");
        }

        // ── Donor Reviewer Feedback Prerequisite ──
        // Stage 2 must remain disabled until ALL assigned reviewers have submitted their Stage 1 feedback.
        // This is a mandatory prerequisite — donor reviewer feedback must be submitted first.
        boolean hasReviewer1 = manuscript.getAssignedReviewer() != null;
        boolean hasReviewer2 = manuscript.getAssignedReviewer2() != null;

        boolean rev1Stage1Submitted = !hasReviewer1 ||
                reviewRepository.findByManuscriptAndReviewerAndStage(manuscript, manuscript.getAssignedReviewer(), 1).isPresent();
        boolean rev2Stage1Submitted = !hasReviewer2 ||
                reviewRepository.findByManuscriptAndReviewerAndStage(manuscript, manuscript.getAssignedReviewer2(), 1).isPresent();

        if (!rev1Stage1Submitted || !rev2Stage1Submitted) {
            String pending;
            if (!rev1Stage1Submitted && !rev2Stage1Submitted) {
                pending = "both Reviewer 1 (" + manuscript.getAssignedReviewerName() + ") and Reviewer 2 (" + manuscript.getAssignedReviewer2Name() + ")";
            } else if (!rev1Stage1Submitted) {
                pending = "Reviewer 1 (" + manuscript.getAssignedReviewerName() + ")";
            } else {
                pending = "Reviewer 2 (" + manuscript.getAssignedReviewer2Name() + ")";
            }
            throw new BadRequestException(
                    "Cannot enable Stage 2: Donor reviewer feedback (Stage 1) has not been submitted by " + pending +
                    ". All assigned reviewers must submit their Stage 1 feedback before Stage 2 can be enabled."
            );
        }

        // ── Consolidated Feedback & Author Resubmission Enforcement ──
        // Keep Stage 2 disabled until the Editor reviews & merges consolidated feedback,
        // sends it to the Author, and the Author resubmits the revised paper.
        boolean hasSentConsolidatedFeedback = (manuscript.getRevisionRequestComments() != null && !manuscript.getRevisionRequestComments().isBlank())
                || "Revision Requested".equalsIgnoreCase(manuscript.getStatus())
                || (manuscript.getHistory() != null && manuscript.getHistory().stream().anyMatch(h -> "Revision Requested".equalsIgnoreCase(h.getStatus())));

        if (!hasSentConsolidatedFeedback) {
            throw new BadRequestException(
                    "Cannot enable Stage 2: Consolidated feedback must first be reviewed, merged, and sent to the Author."
            );
        }

        boolean hasAuthorResubmitted = "Revision Submitted".equalsIgnoreCase(manuscript.getStatus())
                || (manuscript.getAuthorRevisionResponse() != null && !manuscript.getAuthorRevisionResponse().isBlank())
                || (manuscript.getFiles() != null && manuscript.getFiles().stream().anyMatch(ManuscriptFile::isRevised))
                || (manuscript.getRevisions() != null && !manuscript.getRevisions().isEmpty())
                || (manuscript.getHistory() != null && manuscript.getHistory().stream().anyMatch(h -> "Revision Submitted".equalsIgnoreCase(h.getStatus())));

        if (!hasAuthorResubmitted) {
            throw new BadRequestException(
                    "Cannot enable Stage 2: Author has not yet resubmitted the revised paper. Stage 2 can only be enabled after the author resubmits."
            );
        }

        // ── Forward Revisions or Reassign Reviewer Prerequisite ──
        // Keep Stage 2 disabled until the editor performs either Forward Revisions to Reviewers or Reassign Reviewer.
        boolean isForwardedOrReassigned = Boolean.TRUE.equals(manuscript.getRevisionForwardedToReviewer())
                || Boolean.TRUE.equals(manuscript.getReviewerReassignedAfterRevision())
                || (manuscript.getHistory() != null && manuscript.getHistory().stream().anyMatch(h -> 
                        h.getComment() != null && h.getComment().contains("forwarded to peer reviewer")));

        if (!isForwardedOrReassigned) {
            throw new BadRequestException(
                    "Cannot enable Stage 2: The Stage 2 review remains disabled until the Editor-in-Chief performs either 'Forward Revisions to Reviewers' or 'Reassign Reviewer'."
            );
        }

        manuscript.setStage2EnabledByEditor(true);

        String nowStr = LocalDateTime.now().format(DATE_TIME_FORMAT);
        StatusHistoryItem hist = new StatusHistoryItem(
                "hist_" + System.currentTimeMillis(),
                manuscript.getStatus(),
                nowStr,
                editor.getName(),
                "Editor",
                "Stage 2 Final Evaluation enabled by Editor for assigned reviewer(s). Reviewer(s) may now proceed to the comprehensive Stage 2 scorecard."
        );
        manuscript.addHistoryItem(hist);

        Manuscript saved = manuscriptRepository.save(manuscript);

        reviewAssignmentRepository.findByManuscriptIdOrderByRoundNumberAscAssignedDateAsc(manuscriptId)
                .forEach(a -> {
                    a.setStage2Enabled(true);
                    reviewAssignmentRepository.save(a);
                });

        return manuscriptMapper.toDTO(saved, false);
    }

    @Override
    @Transactional
    public ManuscriptResponseDTO forwardRevisionToReviewer(String manuscriptId, com.ijspast.journal.dto.request.ForwardRevisionRequest request) {
        User editor = getAuthenticatedUser();
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ResourceNotFoundException("Manuscript not found: " + manuscriptId));

        if (calculateRevisionCount(manuscript) >= 2) {
            throw new BadRequestException("Revisions cannot be forwarded to reviewers: Author has submitted the revised manuscript twice. At this stage, the Editorial Board must make a final decision: Accept or Reject.");
        }

        if (!"Revision Submitted".equalsIgnoreCase(manuscript.getStatus()) && !"Revision Requested".equalsIgnoreCase(manuscript.getStatus())) {
            throw new BadRequestException("Manuscript is not currently in Revision Submitted status.");
        }

        if (manuscript.getAssignedReviewer() == null && manuscript.getAssignedReviewer2() == null) {
            throw new BadRequestException("No reviewer assigned to this manuscript.");
        }

        int deadlineDays = (request != null && request.getDeadlineDays() > 0) ? request.getDeadlineDays() : 7;
        String deadlineStr = LocalDate.now().plusDays(deadlineDays).toString();
        manuscript.setReviewDeadline(deadlineStr);
        manuscript.setRevisionForwardedToReviewer(true);
        manuscript.setStage2EnabledByEditor(true);
        manuscript.setStatus("Under Review");

        String nowStr = LocalDateTime.now().format(DATE_TIME_FORMAT);
        String remarks = (request != null && request.getNotes() != null) ? request.getNotes().trim() : "";
        String historyComment = "Author revised submission and rebuttal reviewed by Editor (" + editor.getName()
                + ") and explicitly forwarded to peer reviewer for re-evaluation. Stage 2 review feedback forms automatically activated. Due date: " + deadlineStr + "."
                + (remarks.isEmpty() ? "" : " Notes: " + remarks);

        StatusHistoryItem hist = new StatusHistoryItem(
                "hist_" + System.currentTimeMillis(),
                "Under Review",
                nowStr,
                editor.getName(),
                "Editor",
                historyComment
        );
        manuscript.addHistoryItem(hist);

        Manuscript saved = manuscriptRepository.save(manuscript);

        reviewAssignmentRepository.findByManuscriptIdOrderByRoundNumberAscAssignedDateAsc(manuscriptId)
                .forEach(a -> {
                    a.setRevisionForwarded(true);
                    a.setStage2Enabled(true);
                    reviewAssignmentRepository.save(a);
                });

        // Notify Reviewer 1
        if (saved.getAssignedReviewer() != null) {
            emailService.sendRevisionForwardedToReviewerEmail(
                    saved.getAssignedReviewer().getEmail(),
                    saved.getAssignedReviewerName(),
                    saved.getId(),
                    saved.getTitle(),
                    deadlineDays,
                    remarks
            );
        }

        // Notify Reviewer 2 if assigned
        if (saved.getAssignedReviewer2() != null) {
            emailService.sendRevisionForwardedToReviewerEmail(
                    saved.getAssignedReviewer2().getEmail(),
                    saved.getAssignedReviewer2Name(),
                    saved.getId(),
                    saved.getTitle(),
                    deadlineDays,
                    remarks
            );
        }

        return manuscriptMapper.toDTO(saved, false);
    }

    @Override
    @Transactional
    public ManuscriptResponseDTO makeEditorialDecision(String manuscriptId, EditorialDecisionRequest request) {
        User editor = getAuthenticatedUser();
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ResourceNotFoundException("Manuscript not found: " + manuscriptId));

        String action = request.getDecision().toUpperCase().trim();
        String nowStr = LocalDateTime.now().format(DATE_TIME_FORMAT);

        switch (action) {
            case "REQUEST_REVISION" -> {
                if (calculateRevisionCount(manuscript) >= 2) {
                    throw new BadRequestException("Cannot request further revisions: The author has already submitted the revised manuscript twice. At this stage, the Editorial Board can only Accept or Reject the manuscript.");
                }

                if (request.getComments() == null || request.getComments().isBlank()) {
                    throw new BadRequestException("Consolidated revision comments for the author are required.");
                }

                // Enforce dual reviewer feedback requirement: Editor must wait until both Reviewer 1 and Reviewer 2 submit
                boolean hasReviewer1 = manuscript.getAssignedReviewer() != null || (manuscript.getAssignedReviewerName() != null && !manuscript.getAssignedReviewerName().isBlank());
                boolean hasReviewer2 = manuscript.getAssignedReviewer2() != null || (manuscript.getAssignedReviewer2Name() != null && !manuscript.getAssignedReviewer2Name().isBlank());

                if (hasReviewer1 || hasReviewer2) {
                    List<Review> manuscriptReviews = reviewRepository.findByManuscriptOrderBySubmittedAtDesc(manuscript);

                    boolean rev1Submitted = !hasReviewer1 || manuscriptReviews.stream().anyMatch(r ->
                            (manuscript.getAssignedReviewer() != null && r.getReviewer() != null && r.getReviewer().getId().equals(manuscript.getAssignedReviewer().getId())) ||
                            (manuscript.getAssignedReviewerName() != null && r.getReviewerName() != null && r.getReviewerName().equalsIgnoreCase(manuscript.getAssignedReviewerName()))
                    );

                    boolean rev2Submitted = !hasReviewer2 || manuscriptReviews.stream().anyMatch(r ->
                            (manuscript.getAssignedReviewer2() != null && r.getReviewer() != null && r.getReviewer().getId().equals(manuscript.getAssignedReviewer2().getId())) ||
                            (manuscript.getAssignedReviewer2Name() != null && r.getReviewerName() != null && r.getReviewerName().equalsIgnoreCase(manuscript.getAssignedReviewer2Name()))
                    );

                    if (hasReviewer1 && hasReviewer2 && (!rev1Submitted || !rev2Submitted)) {
                        String pending = (!rev1Submitted && !rev2Submitted)
                                ? "both Reviewer 1 (" + manuscript.getAssignedReviewerName() + ") and Reviewer 2 (" + manuscript.getAssignedReviewer2Name() + ")"
                                : (!rev1Submitted ? "Reviewer 1 (" + manuscript.getAssignedReviewerName() + ")" : "Reviewer 2 (" + manuscript.getAssignedReviewer2Name() + ")");
                        throw new BadRequestException("Cannot send consolidated feedback to the Author: Both Reviewer 1 and Reviewer 2 must submit their evaluations before consolidated feedback can be sent. Waiting for " + pending + " to submit feedback.");
                    } else if (!rev1Submitted && !rev2Submitted) {
                        throw new BadRequestException("Cannot send review feedback to the Author: Waiting for assigned reviewer to submit evaluation.");
                    }

                    // Next Review Cycle Enforcement:
                    // If consolidated feedback was already dispatched, keep disabled until new reviewer feedback becomes available for the next cycle
                    boolean alreadySentConsolidated = (manuscript.getRevisionRequestComments() != null && !manuscript.getRevisionRequestComments().isBlank())
                            || (manuscript.getHistory() != null && manuscript.getHistory().stream().anyMatch(h -> "Revision Requested".equalsIgnoreCase(h.getStatus())));

                    if (alreadySentConsolidated) {
                        boolean rev1NewSubmitted = !hasReviewer1 || manuscriptReviews.stream().anyMatch(r -> {
                            boolean isR1 = (manuscript.getAssignedReviewer() != null && r.getReviewer() != null && r.getReviewer().getId().equals(manuscript.getAssignedReviewer().getId()))
                                    || (manuscript.getAssignedReviewerName() != null && r.getReviewerName() != null && r.getReviewerName().equalsIgnoreCase(manuscript.getAssignedReviewerName()));
                            return isR1 && (r.getStage() >= 2 || (r.getRound() != null && r.getRound() >= 2));
                        });

                        boolean rev2NewSubmitted = !hasReviewer2 || manuscriptReviews.stream().anyMatch(r -> {
                            boolean isR2 = (manuscript.getAssignedReviewer2() != null && r.getReviewer() != null && r.getReviewer().getId().equals(manuscript.getAssignedReviewer2().getId()))
                                    || (manuscript.getAssignedReviewer2Name() != null && r.getReviewerName() != null && r.getReviewerName().equalsIgnoreCase(manuscript.getAssignedReviewer2Name()));
                            return isR2 && (r.getStage() >= 2 || (r.getRound() != null && r.getRound() >= 2));
                        });

                        if (!rev1NewSubmitted || !rev2NewSubmitted) {
                            String pending = (!rev1NewSubmitted && !rev2NewSubmitted)
                                    ? "both Reviewer 1 (" + manuscript.getAssignedReviewerName() + ") and Reviewer 2 (" + manuscript.getAssignedReviewer2Name() + ")"
                                    : (!rev1NewSubmitted ? "Reviewer 1 (" + manuscript.getAssignedReviewerName() + ")" : "Reviewer 2 (" + manuscript.getAssignedReviewer2Name() + ")");
                            throw new BadRequestException("Cannot dispatch consolidated feedback: Consolidated feedback has already been sent to the Author. Waiting for new reviewer evaluations for the next review cycle from " + pending + ".");
                        }
                    }
                }

                manuscript.setStatus("Revision Requested");
                manuscript.setRevisionRequestComments(request.getComments().trim());
                if (request.getEditorComments() != null && !request.getEditorComments().isBlank()) {
                    manuscript.setEditorComments(request.getEditorComments().trim());
                }

                manuscript.addHistoryItem(new StatusHistoryItem(
                        "hist_" + System.currentTimeMillis(),
                        "Revision Requested",
                        nowStr,
                        editor.getName(),
                        "Editor",
                        "Consolidated editorial revision requested: " + request.getComments().trim()
                ));
                emailService.sendRevisionRequestEmail(
                        manuscript.getAuthorEmail(),
                        manuscript.getAuthorName(),
                        manuscript.getId(),
                        manuscript.getTitle(),
                        request.getComments().trim()
                );
            }
            case "ACCEPT" -> {
                boolean hasReviewer1 = manuscript.getAssignedReviewer() != null || (manuscript.getAssignedReviewerName() != null && !manuscript.getAssignedReviewerName().isBlank());
                boolean hasReviewer2 = manuscript.getAssignedReviewer2() != null || (manuscript.getAssignedReviewer2Name() != null && !manuscript.getAssignedReviewer2Name().isBlank());

                if (!hasReviewer1 && !hasReviewer2) {
                    throw new BadRequestException("Cannot accept manuscript: Reviewers have not been assigned.");
                }

                int readyCount = computeReadyCount(manuscript);
                int revCount = calculateRevisionCount(manuscript);
                if (hasReviewer1 && hasReviewer2) {
                    if (readyCount < 2 && revCount < 2) {
                        throw new BadRequestException("Cannot accept manuscript: Both assigned reviewers must submit their required feedback for the current review round (" + readyCount + "/2 Ready).");
                    }
                } else if (hasReviewer1 || hasReviewer2) {
                    if (readyCount < 1 && revCount == 0) {
                        throw new BadRequestException("Cannot accept manuscript: The assigned reviewer must submit required feedback before acceptance.");
                    }
                }

                manuscript.setStatus("Accepted");
                manuscript.addHistoryItem(new StatusHistoryItem(
                        "hist_" + System.currentTimeMillis(),
                        "Accepted",
                        nowStr,
                        editor.getName(),
                        "Editor",
                        request.getComments() != null && !request.getComments().isBlank()
                                ? request.getComments().trim()
                                : "Manuscript formally accepted for publication by the Editorial Board."
                ));
            }
            case "REJECT" -> {
                if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
                    throw new BadRequestException("Rejection justification reason is required.");
                }

                boolean hasAssignedReviewers = (manuscript.getAssignedReviewer() != null || (manuscript.getAssignedReviewerName() != null && !manuscript.getAssignedReviewerName().isBlank()))
                        || (manuscript.getAssignedReviewer2() != null || (manuscript.getAssignedReviewer2Name() != null && !manuscript.getAssignedReviewer2Name().isBlank()));

                int revCount = calculateRevisionCount(manuscript);
                int readyCount = computeReadyCount(manuscript);

                boolean inActiveEvaluation = hasAssignedReviewers && (
                        "Under Review".equalsIgnoreCase(manuscript.getStatus())
                        || "Reviewer Assigned".equalsIgnoreCase(manuscript.getStatus())
                        || ("Revision Submitted".equalsIgnoreCase(manuscript.getStatus()) && hasAssignedReviewers)
                        || (readyCount < 2 && revCount < 2)
                );

                if (inActiveEvaluation) {
                    throw new BadRequestException("Manuscript cannot be rejected during the active reviewer evaluation process. Rejection is only permitted prior to reviewer assignment or after reviews have been completed.");
                }

                manuscript.setStatus("Rejected");
                manuscript.setRejectionReason(request.getRejectionReason().trim());
                manuscript.addHistoryItem(new StatusHistoryItem(
                        "hist_" + System.currentTimeMillis(),
                        "Rejected",
                        nowStr,
                        editor.getName(),
                        "Editor",
                        "Manuscript rejected. Reason: " + request.getRejectionReason().trim()
                ));
            }
            case "PUBLISH" -> {
                String doi = (request.getDoi() != null && !request.getDoi().isBlank())
                        ? request.getDoi().trim()
                        : "10.5555/ijspast." + LocalDate.now().getYear() + "." + manuscript.getId().substring(Math.max(0, manuscript.getId().length() - 4));
                String volume = (request.getVolume() != null && !request.getVolume().isBlank()) ? request.getVolume().trim() : "Vol. 14";
                String issue = (request.getIssue() != null && !request.getIssue().isBlank()) ? request.getIssue().trim() : "Issue 4";
                String pubDate = (request.getPublishedDate() != null && !request.getPublishedDate().isBlank())
                        ? request.getPublishedDate().trim()
                        : LocalDate.now().toString();

                manuscript.setStatus("Published");
                manuscript.setPublicationDoi(doi);
                manuscript.setPublicationVolume(volume);
                manuscript.setPublicationIssue(issue);
                manuscript.setPublicationDate(pubDate);

                manuscript.addHistoryItem(new StatusHistoryItem(
                        "hist_" + System.currentTimeMillis(),
                        "Published",
                        nowStr,
                        editor.getName(),
                        "Editor",
                        "Published online in " + volume + ", " + issue + " with DOI: " + doi + "."
                ));

                emailService.sendPublicationEmail(
                        manuscript.getAuthorEmail(),
                        manuscript.getAuthorName(),
                        manuscript.getId(),
                        manuscript.getTitle(),
                        doi, volume, issue
                );
            }
            default -> throw new BadRequestException("Unsupported editorial decision action: " + action);
        }

        Manuscript saved = manuscriptRepository.save(manuscript);

        // Record normalized EditorialDecision
        String decId = "dec_" + saved.getId() + "_" + System.currentTimeMillis();
        EditorialDecision decisionRecord = new EditorialDecision(
                decId,
                saved,
                editor,
                1,
                action,
                request.getComments() != null ? request.getComments().trim() : null,
                request.getRejectionReason() != null ? request.getRejectionReason().trim() : null
        );
        if (request.getEditorComments() != null && !request.getEditorComments().isBlank()) {
            decisionRecord.setEditorComments(request.getEditorComments().trim());
        }
        editorialDecisionRepository.save(decisionRecord);

        return manuscriptMapper.toDTO(saved, false);
    }

    private int computeReadyCount(Manuscript manuscript) {
        int revCount = calculateRevisionCount(manuscript);
        int activeRound = (revCount >= 1 || manuscript.getRevisionForwardedToReviewer()) ? 2 : 1;
        int readyCount = 0;

        User r1 = manuscript.getAssignedReviewer();
        if (r1 != null) {
            List<Review> r1Reviews = reviewRepository.findByManuscriptAndReviewer(manuscript, r1);
            boolean r1SubmittedActiveRound = r1Reviews.stream().anyMatch(r -> {
                if (activeRound == 2) {
                    return (r.getRound() != null && r.getRound() >= 2) || r.getStage() >= 2 || (r.getStageName() != null && r.getStageName().toLowerCase().contains("round 2"));
                } else {
                    return r.getRound() == null || r.getRound() == 1 || r.getStage() == 1 || r.getStage() == 2;
                }
            });
            if (r1SubmittedActiveRound) readyCount++;
        }

        User r2 = manuscript.getAssignedReviewer2();
        if (r2 != null) {
            List<Review> r2Reviews = reviewRepository.findByManuscriptAndReviewer(manuscript, r2);
            boolean r2SubmittedActiveRound = r2Reviews.stream().anyMatch(r -> {
                if (activeRound == 2) {
                    return (r.getRound() != null && r.getRound() >= 2) || r.getStage() >= 2 || (r.getStageName() != null && r.getStageName().toLowerCase().contains("round 2"));
                } else {
                    return r.getRound() == null || r.getRound() == 1 || r.getStage() == 1 || r.getStage() == 2;
                }
            });
            if (r2SubmittedActiveRound) readyCount++;
        }

        return readyCount;
    }

    @Override
    @Transactional
    public ManuscriptResponseDTO reassignReviewer(String manuscriptId, ReassignReviewerRequest request) {
        User editor = getAuthenticatedUser();
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ResourceNotFoundException("Manuscript not found: " + manuscriptId));

        int slot = request.getSlot();
        if (slot != 1 && slot != 2) {
            throw new BadRequestException("Reviewer slot must be 1 or 2.");
        }

        User currentReviewer = (slot == 1) ? manuscript.getAssignedReviewer() : manuscript.getAssignedReviewer2();
        LocalDate assignedDate = (slot == 1) ? manuscript.getAssignedReviewer1Date() : manuscript.getAssignedReviewer2Date();
        LocalDate deadline = (slot == 1) ? manuscript.getAssignedReviewer1Deadline() : manuscript.getAssignedReviewer2Deadline();

        if (assignedDate == null && manuscript.getAssignedDate() != null) {
            try { assignedDate = LocalDate.parse(manuscript.getAssignedDate()); } catch (Exception ignored) {}
        }
        if (deadline == null && assignedDate != null) {
            deadline = assignedDate.plusDays(15);
        }

        if (deadline != null && LocalDate.now().isBefore(deadline)) {
            long remaining = ChronoUnit.DAYS.between(LocalDate.now(), deadline);
            throw new BadRequestException("Cannot reassign Reviewer " + slot + ": Reassignment is only permitted after 15 calendar days without feedback. Remaining days: " + remaining);
        }

        // Check if current reviewer already submitted feedback for active round
        int revCount = calculateRevisionCount(manuscript);
        int activeRound = (revCount >= 1 || manuscript.getRevisionForwardedToReviewer()) ? 2 : 1;
        if (currentReviewer != null) {
            List<Review> existingReviews = reviewRepository.findByManuscriptAndReviewer(manuscript, currentReviewer);
            boolean alreadySubmitted = existingReviews.stream().anyMatch(r -> {
                if (activeRound == 2) {
                    return (r.getRound() != null && r.getRound() >= 2) || r.getStage() >= 2 || (r.getStageName() != null && r.getStageName().toLowerCase().contains("round 2"));
                } else {
                    return r.getRound() == null || r.getRound() == 1 || r.getStage() == 1 || r.getStage() == 2;
                }
            });
            if (alreadySubmitted) {
                throw new BadRequestException("Cannot reassign Reviewer " + slot + ": The reviewer has already submitted required feedback for the current round.");
            }
        }

        // Validate replacement reviewer
        User replacement = userRepository.findById(request.getReplacementReviewerId())
                .orElseThrow(() -> new ResourceNotFoundException("Replacement reviewer not found: " + request.getReplacementReviewerId()));

        if (!"ROLE_REVIEWER".equalsIgnoreCase(replacement.getRole().getName())) {
            throw new BadRequestException("User " + replacement.getName() + " is not an eligible reviewer.");
        }

        // Must not be the author
        if ((manuscript.getAuthorUser() != null && manuscript.getAuthorUser().getId().equals(replacement.getId()))
                || manuscript.getAuthorEmail().equalsIgnoreCase(replacement.getEmail())) {
            throw new BadRequestException("Cannot assign the author as a reviewer.");
        }

        // Must not already be assigned in the other slot
        User otherReviewer = (slot == 1) ? manuscript.getAssignedReviewer2() : manuscript.getAssignedReviewer();
        if (otherReviewer != null && otherReviewer.getId().equals(replacement.getId())) {
            throw new BadRequestException("Reviewer " + replacement.getName() + " is already assigned to the other reviewer slot on this manuscript.");
        }

        // Must not have already completed 2 reviews on this manuscript
        long replacementPrevReviews = reviewRepository.countByManuscriptAndReviewer(manuscript, replacement);
        if (replacementPrevReviews >= 2) {
            throw new BadRequestException("Reviewer " + replacement.getName() + " has already completed the permitted 2 feedback submissions for this manuscript.");
        }

        // Supersede old assignment
        if (currentReviewer != null) {
            List<ReviewAssignment> assignments = reviewAssignmentRepository.findByManuscriptAndReviewer(manuscript, currentReviewer);
            for (ReviewAssignment asgn : assignments) {
                if (!"SUPERSEDED".equalsIgnoreCase(asgn.getStatus()) && !"COMPLETED".equalsIgnoreCase(asgn.getStatus())) {
                    asgn.setStatus("SUPERSEDED");
                    asgn.setReassignedDate(LocalDateTime.now());
                    asgn.setReassignmentReason(request.getReason() != null ? request.getReason().trim() : "Overdue 15 days without feedback");
                    asgn.setSupersededByReviewer(replacement);
                    reviewAssignmentRepository.save(asgn);
                }
            }
        }

        // Create new assignment with fresh 15-day deadline
        LocalDate newAssignedDate = LocalDate.now();
        LocalDate newDeadline = newAssignedDate.plusDays(15);
        ReviewAssignment newAsgn = new ReviewAssignment();
        newAsgn.setId("asgn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        newAsgn.setManuscript(manuscript);
        newAsgn.setReviewer(replacement);
        newAsgn.setRoundNumber(activeRound);
        newAsgn.setAssignedDate(newAssignedDate);
        newAsgn.setReviewDeadline(newDeadline);
        newAsgn.setSlotNumber(slot);
        newAsgn.setAssignmentStatus("ASSIGNED");
        newAsgn.setNotes(request.getReason() != null ? request.getReason().trim() : "Manuscript peer review assignment (Reassigned)");
        reviewAssignmentRepository.save(newAsgn);

        // Update manuscript slot
        if (slot == 1) {
            manuscript.setAssignedReviewer(replacement);
            manuscript.setAssignedReviewerName(replacement.getName());
            manuscript.setAssignedReviewer1Date(newAssignedDate);
            manuscript.setAssignedReviewer1Deadline(newDeadline);
        } else {
            manuscript.setAssignedReviewer2(replacement);
            manuscript.setAssignedReviewer2Name(replacement.getName());
            manuscript.setAssignedReviewer2Date(newAssignedDate);
            manuscript.setAssignedReviewer2Deadline(newDeadline);
        }

        String nowStr = LocalDateTime.now().format(DATE_TIME_FORMAT);
        String oldName = currentReviewer != null ? currentReviewer.getName() : "Reviewer " + slot;
        manuscript.addHistoryItem(new StatusHistoryItem(
                "hist_" + System.currentTimeMillis(),
                "Reviewer Reassigned",
                nowStr,
                editor.getName(),
                "Editor",
                "Slot " + slot + " reviewer (" + oldName + ") reassigned to " + replacement.getName() + " after 15 days of inactivity."
                        + (request.getReason() != null && !request.getReason().isBlank() ? " Reason: " + request.getReason().trim() : "")
        ));

        Manuscript saved = manuscriptRepository.save(manuscript);

        // Send email to newly assigned reviewer
        try {
            emailService.sendReviewerAssignmentEmail(
                    replacement.getEmail(),
                    replacement.getName(),
                    manuscript.getId(),
                    manuscript.getTitle(),
                    15,
                    request.getReason() != null ? request.getReason().trim() : "Manuscript peer review assignment (Reassigned)"
            );
        } catch (Exception e) {
            log.error("Failed to send reassignment email to reviewer {}: {}", replacement.getEmail(), e.getMessage());
        }

        return manuscriptMapper.toDTO(saved, false);
    }

    @Override
    @Transactional(readOnly = true)
    public CommunicationThreadDTO getCommunications(String manuscriptId) {
        User user = getAuthenticatedUser();
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ResourceNotFoundException("Manuscript not found: " + manuscriptId));

        validateCommunicationAccess(manuscript, user);

        List<ManuscriptCommunication> comms = communicationRepository.findByManuscriptOrderBySentAtAsc(manuscript);
        List<CommunicationMessageDTO> messageDTOs = comms.stream().map(c -> {
            CommunicationMessageDTO dto = new CommunicationMessageDTO(
                    c.getId(),
                    c.getSender() != null ? c.getSender().getId() : null,
                    c.getSenderName(),
                    c.getSenderRole(),
                    c.getMessage(),
                    c.getSentAt() != null ? c.getSentAt().toString() : ""
            );
            dto.setAttachmentName(c.getAttachmentName());
            dto.setAttachmentUrl(c.getAttachmentUrl());
            dto.setAttachmentSize(c.getAttachmentSize());
            return dto;
        }).collect(Collectors.toList());

        boolean isPost = manuscriptMapper.isPostReviewStage(manuscript);
        int total = messageDTOs.size();
        boolean limitReached = total >= 5;
        boolean canSend = isPost && !limitReached;

        return new CommunicationThreadDTO(manuscript.getId(), total, 5, canSend, isPost, messageDTOs);
    }

    @Override
    @Transactional
    public CommunicationThreadDTO sendMessage(String manuscriptId, SendMessageRequest request) {
        User user = getAuthenticatedUser();
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ResourceNotFoundException("Manuscript not found: " + manuscriptId));

        validateCommunicationAccess(manuscript, user);

        if (!manuscriptMapper.isPostReviewStage(manuscript)) {
            throw new BadRequestException("Editor–Author communication is only enabled after the reviewer evaluation process has ended.");
        }

        long count = communicationRepository.countByManuscript(manuscript);
        if (count >= 5) {
            throw new BadRequestException("The maximum of five communication exchanges has been reached.");
        }

        String role = determineSenderRole(user, manuscript);
        ManuscriptCommunication comm = new ManuscriptCommunication(
                "comm_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                manuscript,
                user,
                user.getName(),
                role,
                request.getMessage().trim()
        );

        if (request.getAttachmentName() != null && !request.getAttachmentName().isBlank()) {
            comm.setAttachmentName(request.getAttachmentName().trim());
            comm.setAttachmentUrl(request.getAttachmentUrl());
            comm.setAttachmentSize(request.getAttachmentSize());

            // If author uploads an attachment after making changes, record it as a revised manuscript file
            if ("Author".equalsIgnoreCase(role)) {
                int revisedVersion = (int) manuscript.getFiles().stream().filter(ManuscriptFile::isRevised).count() + 1;
                String fileId = "file_rev_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
                long size = request.getAttachmentSize() != null && request.getAttachmentSize() > 0 ? request.getAttachmentSize() : 3600000L;
                String fileKey = fileStorageService.storeFile(("Communication attachment for " + manuscriptId).getBytes(), request.getAttachmentName().trim());
                ManuscriptFile revisedFile = new ManuscriptFile(fileId, request.getAttachmentName().trim(), size, "application/pdf", fileKey, revisedVersion, true);
                manuscript.addFile(revisedFile);

                manuscript.setStatus("Revision Submitted");
                manuscript.setRevisionForwardedToReviewer(false);
                manuscript.setAuthorRevisionResponse("Changes submitted via Editor-Author Communication: " + request.getMessage().trim());

                String nowStr = LocalDateTime.now().format(DATE_TIME_FORMAT);
                StatusHistoryItem hist = new StatusHistoryItem(
                        "hist_" + System.currentTimeMillis(),
                        "Revision Submitted",
                        nowStr,
                        user.getName(),
                        "Author",
                        "Revised manuscript uploaded via Editor Communications: " + request.getAttachmentName().trim()
                );
                manuscript.addHistoryItem(hist);
                manuscriptRepository.save(manuscript);
            }
        }

        communicationRepository.save(comm);

        return getCommunications(manuscriptId);
    }

    private void validateCommunicationAccess(Manuscript manuscript, User user) {
        String roleName = user.getRole().getName();
        boolean isEditor = "ROLE_EDITOR".equalsIgnoreCase(roleName) || "ROLE_ADMIN".equalsIgnoreCase(roleName);
        boolean isAuthor = (manuscript.getAuthorUser() != null && manuscript.getAuthorUser().getId().equals(user.getId()))
                || (manuscript.getAuthorEmail() != null && manuscript.getAuthorEmail().equalsIgnoreCase(user.getEmail()));

        if (!isEditor && !isAuthor) {
            throw new BadRequestException("Access denied: Only the authorized Editor and Manuscript Author can participate in communications.");
        }
    }

    private String determineSenderRole(User user, Manuscript manuscript) {
        String roleName = user.getRole().getName();
        if ("ROLE_EDITOR".equalsIgnoreCase(roleName) || "ROLE_ADMIN".equalsIgnoreCase(roleName)) {
            return "Editor";
        }
        return "Author";
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManuscriptResponseDTO> getPublishedManuscripts(String search, String domain) {
        List<Manuscript> published = manuscriptRepository.findByStatusOrderByCreatedAtDesc("Published");
        return filterManuscripts(published, null, domain, search, false);
    }

    @Override
    @Transactional(readOnly = true)
    public ManuscriptFile getAuthorizedFile(String fileId) {
        ManuscriptFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Manuscript file not found with ID: " + fileId));

        // Authorization check: User must be Editor, assigned Reviewer, or author of the manuscript
        User currentUser = getAuthenticatedUser();
        Manuscript m = file.getManuscript();

        boolean isEditor = currentUser.getRole() == UserRole.ROLE_EDITOR;
        boolean isAuthor = m.getAuthorEmail().equalsIgnoreCase(currentUser.getEmail());
        boolean isReviewer = m.getAssignedReviewer() != null && m.getAssignedReviewer().getId().equals(currentUser.getId());

        if (!isEditor && !isAuthor && !isReviewer) {
            throw new UnauthorizedException("You are not authorized to download this manuscript file.");
        }

        return file;
    }

    private List<ManuscriptResponseDTO> filterManuscripts(List<Manuscript> list, String status, String domain, String search, boolean maskAuthor) {
        return filterManuscripts(list, status, domain, search, maskAuthor, false);
    }

    private List<ManuscriptResponseDTO> filterManuscripts(List<Manuscript> list, String status, String domain, String search, boolean maskAuthor, boolean maskReviewer) {
        return list.stream()
                .filter(m -> {
                    if (status != null && !status.isBlank() && !status.equalsIgnoreCase("all")) {
                        if (!m.getStatus().equalsIgnoreCase(status.trim())) {
                            return false;
                        }
                    }
                    if (domain != null && !domain.isBlank() && !domain.equalsIgnoreCase("all")) {
                        if (!m.getDomain().equalsIgnoreCase(domain.trim())) {
                            return false;
                        }
                    }
                    if (search != null && !search.isBlank()) {
                        String s = search.toLowerCase();
                        boolean matchId = m.getId().toLowerCase().contains(s);
                        boolean matchTitle = m.getTitle().toLowerCase().contains(s);
                        boolean matchAuthor = m.getAuthorName().toLowerCase().contains(s);
                        boolean matchDomain = m.getDomain().toLowerCase().contains(s);
                        boolean matchDoi = m.getPublicationDoi() != null && m.getPublicationDoi().toLowerCase().contains(s);
                        return matchId || matchTitle || matchAuthor || matchDomain || matchDoi;
                    }
                    return true;
                })
                .map(m -> manuscriptMapper.toDTO(m, maskAuthor, maskReviewer))
                .collect(Collectors.toList());
    }

    private Optional<Manuscript> findExistingManuscriptForAuthor(ManuscriptSubmissionRequest request, String email) {
        // 1. Explicit ID check from request if supplied
        String explicitId = request.getId();
        if (explicitId != null && !explicitId.isBlank()) {
            Optional<Manuscript> byId = manuscriptRepository.findById(explicitId);
            if (byId.isPresent()) {
                Manuscript m = byId.get();
                if (m.getAuthorEmail().equalsIgnoreCase(email)) {
                    return Optional.of(m);
                }
            }
        }

        // 2. Exact or case-insensitive title match for this author
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            Optional<Manuscript> byTitle = manuscriptRepository.findFirstByAuthorEmailIgnoreCaseAndTitleIgnoreCase(
                    email, request.getTitle().trim()
            );
            if (byTitle.isPresent()) {
                return byTitle;
            }
        }

        // 3. Normalized title match (ignoring punctuation, hyphens, and spacing differences)
        List<Manuscript> authorSubmissions = manuscriptRepository.findByAuthorEmailIgnoreCaseOrderByCreatedAtDesc(email);
        if (request.getTitle() != null && !request.getTitle().isBlank() && !authorSubmissions.isEmpty()) {
            String normReq = request.getTitle().replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            for (Manuscript m : authorSubmissions) {
                if (m.getTitle() != null) {
                    String normM = m.getTitle().replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                    if (normReq.equals(normM)) {
                        return Optional.of(m);
                    }
                }
            }
        }

        // 4. If author has a manuscript explicitly returned for corrections or revision requested
        if (!authorSubmissions.isEmpty()) {
            for (Manuscript m : authorSubmissions) {
                String s = m.getStatus();
                if ("Returned for Corrections".equalsIgnoreCase(s) || "Revision Requested".equalsIgnoreCase(s)) {
                    return Optional.of(m);
                }
            }
        }

        return Optional.empty();
    }

    private String generateAuthorPassword() {
        String digits = "23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder("Author@");
        for (int i = 0; i < 4; i++) {
            sb.append(digits.charAt(random.nextInt(digits.length())));
        }
        return sb.toString();
    }

    private Author getOrCreateAuthorEntity(User authorUser, String name, String email, String institution, String department, String cityCountry, String orcid) {
        String cleanEmail = email.trim().toLowerCase();
        return authorRepository.findByEmailIgnoreCase(cleanEmail)
                .or(() -> authorUser != null ? authorRepository.findByUserId(authorUser.getId()) : Optional.empty())
                .map(existing -> {
                    if (authorUser != null && existing.getUser() == null) existing.setUser(authorUser);
                    if (name != null && !name.isBlank()) existing.setName(name);
                    if (institution != null && !institution.isBlank()) existing.setInstitution(institution);
                    if (department != null && !department.isBlank()) existing.setDepartment(department);
                    if (cityCountry != null && !cityCountry.isBlank()) existing.setCityCountry(cityCountry);
                    if (orcid != null && !orcid.isBlank()) existing.setOrcid(orcid);
                    return authorRepository.save(existing);
                })
                .orElseGet(() -> {
                    Author a = new Author();
                    a.setId("auth_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 5));
                    a.setUser(authorUser);
                    a.setName(name != null ? name : (authorUser != null ? authorUser.getName() : "Author"));
                    a.setEmail(cleanEmail);
                    a.setInstitution(institution != null ? institution : "Academic Institution");
                    a.setDepartment(department);
                    a.setCityCountry(cityCountry != null ? cityCountry : "India");
                    a.setOrcid(orcid != null ? orcid : "0000-0000-0000-0000");
                    return authorRepository.save(a);
                });
    }

    private Reviewer getOrCreateReviewerEntity(User user) {
        if (user == null) return null;
        String cleanEmail = user.getEmail().trim().toLowerCase();
        return reviewerRepository.findByUserId(user.getId())
                .or(() -> reviewerRepository.findByEmailIgnoreCase(cleanEmail))
                .orElseGet(() -> {
                    Reviewer r = new Reviewer();
                    r.setId("rev_rec_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 5));
                    r.setUser(user);
                    r.setName(user.getName());
                    r.setEmail(cleanEmail);
                    r.setInstitution(user.getInstitution() != null ? user.getInstitution() : "Academic Institution");
                    r.setDepartment(user.getDepartment() != null ? user.getDepartment() : "Peer Review Board");
                    r.setDomain(user.getDomain() != null ? user.getDomain() : "Computer Science");
                    r.setPhone(user.getPhone());
                    return reviewerRepository.save(r);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuthorTableDTO> getAuthorsTable() {
        List<Manuscript> allManuscripts = manuscriptRepository.findAll();
        Map<String, List<Manuscript>> papersByAuthorEmail = new HashMap<>();

        for (Manuscript m : allManuscripts) {
            if (m.getAuthorEmail() != null) {
                String em = m.getAuthorEmail().trim().toLowerCase();
                papersByAuthorEmail.computeIfAbsent(em, k -> new ArrayList<>()).add(m);
            }
        }

        List<Author> authors = authorRepository.findAll();
        Set<String> processedEmails = new HashSet<>();
        List<AuthorTableDTO> result = new ArrayList<>();

        for (Author a : authors) {
            String emailKey = a.getEmail().trim().toLowerCase();
            processedEmails.add(emailKey);

            AuthorTableDTO dto = new AuthorTableDTO();
            dto.setId(a.getId());
            dto.setName(a.getName());
            dto.setEmail(a.getEmail());
            dto.setInstitution(a.getInstitution());
            dto.setDepartment(a.getDepartment());
            dto.setCityCountry(a.getCityCountry());
            dto.setOrcid(a.getOrcid());
            dto.setPhone(a.getPhone());

            List<Manuscript> authorPapers = papersByAuthorEmail.getOrDefault(emailKey, Collections.emptyList());
            dto.setTotalPapers(authorPapers.size());

            List<AuthorTableDTO.AuthorPaperItemDTO> paperDTOs = new ArrayList<>();
            for (Manuscript p : authorPapers) {
                AuthorTableDTO.AuthorPaperItemDTO pDto = new AuthorTableDTO.AuthorPaperItemDTO();
                pDto.setId(p.getId());
                pDto.setTitle(p.getTitle());
                pDto.setDomain(p.getDomain());
                pDto.setStatus(p.getStatus());
                pDto.setSubmissionDate(p.getSubmissionDate());

                if (p.getAssignedReviewer() != null) {
                    User r1 = p.getAssignedReviewer();
                    AuthorTableDTO.ReviewerBriefDTO r1Dto = new AuthorTableDTO.ReviewerBriefDTO(
                            r1.getId(), r1.getName(), r1.getEmail(), r1.getInstitution(), r1.getDomain()
                    );
                    r1Dto.setStage1Submitted(reviewRepository.findByManuscriptAndReviewerAndStage(p, r1, 1).isPresent());
                    r1Dto.setStage2Submitted(reviewRepository.findByManuscriptAndReviewerAndStage(p, r1, 2).isPresent());
                    pDto.setReviewer1(r1Dto);
                }

                if (p.getAssignedReviewer2() != null) {
                    User r2 = p.getAssignedReviewer2();
                    AuthorTableDTO.ReviewerBriefDTO r2Dto = new AuthorTableDTO.ReviewerBriefDTO(
                            r2.getId(), r2.getName(), r2.getEmail(), r2.getInstitution(), r2.getDomain()
                    );
                    r2Dto.setStage1Submitted(reviewRepository.findByManuscriptAndReviewerAndStage(p, r2, 1).isPresent());
                    r2Dto.setStage2Submitted(reviewRepository.findByManuscriptAndReviewerAndStage(p, r2, 2).isPresent());
                    pDto.setReviewer2(r2Dto);
                }

                paperDTOs.add(pDto);
            }
            dto.setPapers(paperDTOs);
            result.add(dto);
        }

        for (Map.Entry<String, List<Manuscript>> entry : papersByAuthorEmail.entrySet()) {
            if (!processedEmails.contains(entry.getKey()) && !entry.getValue().isEmpty()) {
                Manuscript first = entry.getValue().get(0);
                AuthorTableDTO dto = new AuthorTableDTO();
                dto.setId("auth_m_" + first.getId());
                dto.setName(first.getAuthorName());
                dto.setEmail(first.getAuthorEmail());
                dto.setInstitution(first.getAuthorInstitution());
                dto.setDepartment(first.getAuthorDepartment());
                dto.setCityCountry(first.getAuthorCityCountry());
                dto.setOrcid(first.getAuthorOrcid());
                dto.setTotalPapers(entry.getValue().size());

                List<AuthorTableDTO.AuthorPaperItemDTO> paperDTOs = new ArrayList<>();
                for (Manuscript p : entry.getValue()) {
                    AuthorTableDTO.AuthorPaperItemDTO pDto = new AuthorTableDTO.AuthorPaperItemDTO();
                    pDto.setId(p.getId());
                    pDto.setTitle(p.getTitle());
                    pDto.setDomain(p.getDomain());
                    pDto.setStatus(p.getStatus());
                    pDto.setSubmissionDate(p.getSubmissionDate());

                    if (p.getAssignedReviewer() != null) {
                        User r1 = p.getAssignedReviewer();
                        AuthorTableDTO.ReviewerBriefDTO r1Dto = new AuthorTableDTO.ReviewerBriefDTO(
                                r1.getId(), r1.getName(), r1.getEmail(), r1.getInstitution(), r1.getDomain()
                        );
                        r1Dto.setStage1Submitted(reviewRepository.findByManuscriptAndReviewerAndStage(p, r1, 1).isPresent());
                        r1Dto.setStage2Submitted(reviewRepository.findByManuscriptAndReviewerAndStage(p, r1, 2).isPresent());
                        pDto.setReviewer1(r1Dto);
                    }
                    if (p.getAssignedReviewer2() != null) {
                        User r2 = p.getAssignedReviewer2();
                        AuthorTableDTO.ReviewerBriefDTO r2Dto = new AuthorTableDTO.ReviewerBriefDTO(
                                r2.getId(), r2.getName(), r2.getEmail(), r2.getInstitution(), r2.getDomain()
                        );
                        r2Dto.setStage1Submitted(reviewRepository.findByManuscriptAndReviewerAndStage(p, r2, 1).isPresent());
                        r2Dto.setStage2Submitted(reviewRepository.findByManuscriptAndReviewerAndStage(p, r2, 2).isPresent());
                        pDto.setReviewer2(r2Dto);
                    }
                    paperDTOs.add(pDto);
                }
                dto.setPapers(paperDTOs);
                result.add(dto);
            }
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewerTableDTO> getReviewersTable() {
        List<User> reviewerUsers = userRepository.findByRoleOrderByNameAsc(UserRole.ROLE_REVIEWER);
        List<Manuscript> allManuscripts = manuscriptRepository.findAll();

        List<ReviewerTableDTO> result = new ArrayList<>();

        for (User ru : reviewerUsers) {
            ReviewerTableDTO dto = new ReviewerTableDTO();
            dto.setId(ru.getId());
            dto.setName(ru.getName());
            dto.setEmail(ru.getEmail());
            dto.setInstitution(ru.getInstitution());
            dto.setDepartment(ru.getDepartment());
            dto.setDomain(ru.getDomain());
            dto.setPhone(ru.getPhone());

            List<ReviewerTableDTO.ReviewerPaperAssignmentDTO> assignedList = new ArrayList<>();
            int completedCount = 0;
            int activeCount = 0;

            for (Manuscript m : allManuscripts) {
                boolean isRev1 = m.getAssignedReviewer() != null && m.getAssignedReviewer().getId().equals(ru.getId());
                boolean isRev2 = m.getAssignedReviewer2() != null && m.getAssignedReviewer2().getId().equals(ru.getId());

                if (isRev1 || isRev2) {
                    ReviewerTableDTO.ReviewerPaperAssignmentDTO assignment = new ReviewerTableDTO.ReviewerPaperAssignmentDTO();
                    assignment.setPaperId(m.getId());
                    assignment.setPaperTitle(m.getTitle());
                    assignment.setDomain(m.getDomain());
                    assignment.setStatus(m.getStatus());
                    assignment.setAssignedRole(isRev1 ? "Reviewer 1" : "Reviewer 2");
                    assignment.setAssignedDate(m.getAssignedDate());
                    assignment.setReviewDeadline(m.getReviewDeadline());

                    ReviewerTableDTO.AuthorBriefDTO authorBrief = new ReviewerTableDTO.AuthorBriefDTO(
                            m.getAuthor() != null ? m.getAuthor().getId() : "auth_" + m.getAuthorEmail(),
                            m.getAuthorName(),
                            m.getAuthorEmail(),
                            m.getAuthorInstitution(),
                            m.getAuthorCityCountry(),
                            m.getAuthorOrcid()
                    );
                    assignment.setAuthor(authorBrief);

                    User coUser = isRev1 ? m.getAssignedReviewer2() : m.getAssignedReviewer();
                    if (coUser != null) {
                        assignment.setCoReviewer(new ReviewerTableDTO.CoReviewerBriefDTO(
                                coUser.getId(), coUser.getName(), coUser.getEmail(), coUser.getInstitution()
                        ));
                    }

                    boolean s1 = reviewRepository.findByManuscriptAndReviewerAndStage(m, ru, 1).isPresent();
                    boolean s2 = reviewRepository.findByManuscriptAndReviewerAndStage(m, ru, 2).isPresent();
                    assignment.setStage1Completed(s1);
                    assignment.setStage2Completed(s2);

                    if (s1 && s2) {
                        completedCount++;
                    } else {
                        activeCount++;
                    }

                    assignedList.add(assignment);
                }
            }

            dto.setActiveAssignmentsCount(activeCount);
            dto.setCompletedReviewsCount(completedCount);
            dto.setTotalAssignedPapers(assignedList.size());
            dto.setAssignedPapers(assignedList);
            result.add(dto);
        }

        return result;
    }
}
