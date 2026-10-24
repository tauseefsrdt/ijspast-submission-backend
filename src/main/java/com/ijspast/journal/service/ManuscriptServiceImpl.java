package com.ijspast.journal.service;

import com.ijspast.journal.dto.request.AssignReviewerRequest;
import com.ijspast.journal.dto.request.EditorialDecisionRequest;
import com.ijspast.journal.dto.request.ManuscriptSubmissionRequest;
import com.ijspast.journal.dto.request.RevisionSubmissionRequest;
import com.ijspast.journal.dto.response.DispatchedEmailDTO;
import com.ijspast.journal.dto.response.ManuscriptResponseDTO;
import com.ijspast.journal.dto.response.SubmissionResponseDTO;
import com.ijspast.journal.entity.*;
import com.ijspast.journal.exception.BadRequestException;
import com.ijspast.journal.exception.ResourceNotFoundException;
import com.ijspast.journal.exception.UnauthorizedException;
import com.ijspast.journal.mapper.ManuscriptMapper;
import com.ijspast.journal.repository.ManuscriptFileRepository;
import com.ijspast.journal.repository.ManuscriptRepository;
import com.ijspast.journal.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ManuscriptServiceImpl implements ManuscriptService {

    private static final Logger log = LoggerFactory.getLogger(ManuscriptServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ManuscriptRepository manuscriptRepository;
    private final UserRepository userRepository;
    private final ManuscriptFileRepository fileRepository;
    private final ManuscriptMapper manuscriptMapper;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public ManuscriptServiceImpl(ManuscriptRepository manuscriptRepository,
                                 UserRepository userRepository,
                                 ManuscriptFileRepository fileRepository,
                                 ManuscriptMapper manuscriptMapper,
                                 FileStorageService fileStorageService,
                                 EmailService emailService,
                                 PasswordEncoder passwordEncoder) {
        this.manuscriptRepository = manuscriptRepository;
        this.userRepository = userRepository;
        this.fileRepository = fileRepository;
        this.manuscriptMapper = manuscriptMapper;
        this.fileStorageService = fileStorageService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
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

        return filterManuscripts(list, status, null, search, false);
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

        return manuscriptMapper.toDTO(manuscript, false);
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
        manuscript.setAuthorRevisionResponse(request.getResponse().trim());

        String nowStr = LocalDateTime.now().format(DATE_TIME_FORMAT);
        StatusHistoryItem hist = new StatusHistoryItem(
                "hist_" + System.currentTimeMillis(),
                "Revision Submitted",
                nowStr,
                author.getName(),
                "Author",
                "Revised manuscript uploaded: " + fileName + ". Author remarks: " + request.getResponse().trim()
        );
        manuscript.addHistoryItem(hist);

        Manuscript saved = manuscriptRepository.save(manuscript);

        // Notify Editor
        emailService.sendRevisionSubmittedEmail(
                "editor@ijspast.demo",
                "Prof. (Dr.) Vijay Tiwari (Editor-in-Chief)",
                "Editor",
                saved.getId(),
                saved.getTitle(),
                request.getResponse().trim()
        );

        // Notify Reviewer if assigned
        if (saved.getAssignedReviewer() != null) {
            emailService.sendRevisionSubmittedEmail(
                    saved.getAssignedReviewer().getEmail(),
                    saved.getAssignedReviewerName(),
                    "Reviewer",
                    saved.getId(),
                    saved.getTitle(),
                    request.getResponse().trim()
            );
        }

        return manuscriptMapper.toDTO(saved, false);
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

    @Override
    @Transactional
    public ManuscriptResponseDTO assignReviewer(String manuscriptId, AssignReviewerRequest request) {
        User editor = getAuthenticatedUser();
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ResourceNotFoundException("Manuscript not found: " + manuscriptId));

        User reviewer = userRepository.findById(request.getReviewerId())
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found: " + request.getReviewerId()));

        if (reviewer.getRole() != UserRole.ROLE_REVIEWER) {
            throw new BadRequestException("Selected user is not registered as a Reviewer.");
        }

        int deadlineDays = request.getDeadlineDays() > 0 ? request.getDeadlineDays() : 14;
        String deadlineStr = LocalDate.now().plusDays(deadlineDays).toString();
        String dateStr = LocalDate.now().toString();

        manuscript.setStatus("Under Review");
        manuscript.setAssignedReviewer(reviewer);
        manuscript.setAssignedReviewerName(reviewer.getName());
        manuscript.setAssignedDate(dateStr);
        manuscript.setReviewDeadline(deadlineStr);

        String nowStr = LocalDateTime.now().format(DATE_TIME_FORMAT);
        StatusHistoryItem historyItem = new StatusHistoryItem(
                "hist_" + System.currentTimeMillis(),
                "Under Review",
                nowStr,
                editor.getName(),
                "Editor",
                "Assigned to " + reviewer.getName() + " for peer review evaluation. Due by " + deadlineStr + "."
        );
        manuscript.addHistoryItem(historyItem);

        Manuscript saved = manuscriptRepository.save(manuscript);

        // Send peer review invitation email
        emailService.sendReviewerInvitationEmail(
                reviewer.getEmail(),
                reviewer.getName(),
                "Reviewer@123",
                saved.getId(),
                saved.getTitle(),
                deadlineDays
        );

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
                if (request.getComments() == null || request.getComments().isBlank()) {
                    throw new BadRequestException("Revision comments for the author are required.");
                }
                manuscript.setStatus("Revision Requested");
                manuscript.setRevisionRequestComments(request.getComments().trim());
                manuscript.addHistoryItem(new StatusHistoryItem(
                        "hist_" + System.currentTimeMillis(),
                        "Revision Requested",
                        nowStr,
                        editor.getName(),
                        "Editor",
                        "Editorial revision requested: " + request.getComments().trim()
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
        return manuscriptMapper.toDTO(saved, false);
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
                .map(m -> manuscriptMapper.toDTO(m, maskAuthor))
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
}
