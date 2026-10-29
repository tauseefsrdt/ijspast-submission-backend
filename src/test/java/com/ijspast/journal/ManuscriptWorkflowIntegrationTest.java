package com.ijspast.journal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ijspast.journal.dto.request.*;
import com.ijspast.journal.entity.Manuscript;
import com.ijspast.journal.entity.User;
import com.ijspast.journal.entity.UserRole;
import com.ijspast.journal.repository.EmailNotificationLogRepository;
import com.ijspast.journal.repository.ManuscriptFileRepository;
import com.ijspast.journal.repository.ManuscriptRepository;
import com.ijspast.journal.repository.UserRepository;
import com.ijspast.journal.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ManuscriptWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ManuscriptRepository manuscriptRepository;

    @Autowired
    private ManuscriptFileRepository manuscriptFileRepository;

    @Autowired
    private EmailNotificationLogRepository emailNotificationLogRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Value("${app.editor.email}")
    private String editorEmail;

    @Test
    @DisplayName("Complete Journal Lifecycle: Author Submission -> Editor Reviewer Assignment -> Reviewer Scorecard -> Editorial Decision -> Publication")
    void testCompleteJournalLifecycle() throws Exception {
        // -------------------------------------------------------------
        // Step 1: New Author submits a manuscript publicly (/api/v1/manuscripts)
        // -------------------------------------------------------------
        String newAuthorEmail = "newauthor." + System.currentTimeMillis() + "@university.edu";

        AuthorInfoRequest authorInfo = new AuthorInfoRequest(
                "Dr. Jennifer Smith",
                newAuthorEmail,
                "Oxford Research Institute",
                "Dept. of Nanotechnology",
                "Oxford, UK",
                "0000-0002-9876-5432"
        );

        ManuscriptSubmissionRequest submissionReq = new ManuscriptSubmissionRequest();
        submissionReq.setAuthor(authorInfo);
        submissionReq.setTitle("Novel High-Mobility 2D Graphene Nanostructures for Terahertz Transceivers");
        submissionReq.setDomain("Electrical, Electronics & Communications Engineering");
        submissionReq.setAbstractText("This paper introduces scalable fabrication protocols for ballistic graphene field effect channels operating efficiently in the terahertz spectrum.");
        submissionReq.setKeywords("Graphene, Terahertz, 2D Materials, Transceivers");
        submissionReq.setCoauthors("Prof. Alan Turing");
        submissionReq.setDeclaration(true);
        submissionReq.setFile(new FileMetadataRequest("Graphene_THz_Paper.pdf", 4200000L, "application/pdf"));

        String submissionResponseJson = mockMvc.perform(post("/api/v1/manuscripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submissionReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.manuscriptId").isNotEmpty())
                .andExpect(jsonPath("$.data.generatedCredentials.email").value(newAuthorEmail))
                .andExpect(jsonPath("$.data.generatedCredentials.password").isNotEmpty())
                .andExpect(jsonPath("$.data.manuscript.status").value("Submitted"))
                .andReturn().getResponse().getContentAsString();

        String manuscriptId = objectMapper.readTree(submissionResponseJson).path("data").path("manuscriptId").asText();
        assertThat(manuscriptId).startsWith("IJSPAST-");

        // Verify Author User was created in database
        User createdAuthor = userRepository.findByEmailIgnoreCase(newAuthorEmail).orElse(null);
        assertThat(createdAuthor).isNotNull();
        assertThat(createdAuthor.getName()).isEqualTo("Dr. Jennifer Smith");

        // -------------------------------------------------------------
        // Step 2: Same Author submits another manuscript (associates with existing user)
        // -------------------------------------------------------------
        ManuscriptSubmissionRequest secondSubmissionReq = new ManuscriptSubmissionRequest();
        secondSubmissionReq.setAuthor(authorInfo);
        secondSubmissionReq.setTitle("Second Paper on Carbon Allotropes");
        secondSubmissionReq.setDomain("Applied Science, Physics & Sensor Calibration");
        secondSubmissionReq.setAbstractText("Additional findings regarding carbon allotrope mechanical strength.");
        secondSubmissionReq.setKeywords("Carbon, Allotropes, Mechanics");
        secondSubmissionReq.setDeclaration(true);

        mockMvc.perform(post("/api/v1/manuscripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondSubmissionReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.generatedCredentials.email").value(newAuthorEmail));

        // User count should not duplicate
        long authorCountWithEmail = userRepository.findByEmailIgnoreCase(newAuthorEmail).stream().count();
        assertThat(authorCountWithEmail).isEqualTo(1);

        // -------------------------------------------------------------
        // Step 3: Editor logs in and assigns Reviewer to the first manuscript
        // -------------------------------------------------------------
        User editor = userRepository.findByEmailIgnoreCase(editorEmail).orElseThrow();
        String editorJwt = jwtTokenProvider.generateToken(editor.getEmail(), editor.getRole().name(), editor.getId());

        User reviewer = userRepository.findByEmailIgnoreCase("workflow.reviewer@university.edu")
                .orElseGet(() -> {
                    User r = new User(
                            "rev_workflow_01",
                            "Dr. Workflow Reviewer",
                            "workflow.reviewer@university.edu",
                            "Reviewer@123",
                            UserRole.ROLE_REVIEWER
                    );
                    r.setInstitution("Test Institute");
                    r.setEnabled(true);
                    r.setAccountSetupCompleted(true);
                    return userRepository.save(r);
                });

        AssignReviewerRequest assignReq = new AssignReviewerRequest(reviewer.getId(), reviewer.getName(), 14);

        mockMvc.perform(post("/api/v1/editor/manuscripts/" + manuscriptId + "/assign-reviewer")
                        .header("Authorization", "Bearer " + editorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("Under Review"))
                .andExpect(jsonPath("$.data.assignedReviewerName").value(reviewer.getName()));

        // -------------------------------------------------------------
        // Step 4: Reviewer logs in and views assignment (Author details strictly masked for double-blind review)
        // -------------------------------------------------------------
        String reviewerJwt = jwtTokenProvider.generateToken(reviewer.getEmail(), reviewer.getRole().name(), reviewer.getId());

        mockMvc.perform(get("/api/v1/reviewer/assignments/" + manuscriptId)
                        .header("Authorization", "Bearer " + reviewerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.author.name").value("[Masked for Double-Blind Review]"))
                .andExpect(jsonPath("$.data.author.email").doesNotExist())
                .andExpect(jsonPath("$.data.coauthors").doesNotExist());

        // -------------------------------------------------------------
        // Step 5: Reviewer submits complete scorecard with rubric scores
        // -------------------------------------------------------------
        SubmitReviewRequest reviewReq = new SubmitReviewRequest();
        reviewReq.setRecommendation("Minor Revision");
        reviewReq.setTechnicalQuality(4);
        reviewReq.setOriginality(5);
        reviewReq.setClarity(4);
        reviewReq.setStage(2);
        reviewReq.setStageName("Stage 2: Final Evaluation");
        reviewReq.setRubricScores(java.util.List.of(4, 5, 4, 4, 5, 4, 5, 4, 4, 5, 5));
        reviewReq.setOverallScore(4.5);
        reviewReq.setCommentsForEditor("Strong paper with solid derivations. Minor clarifications required in Section 3.");
        reviewReq.setCommentsForAuthor("Please elaborate on the carrier mobility calibration benchmarks in Table 2.");
        reviewReq.setConfidentialComments("High publication potential.");

        mockMvc.perform(post("/api/v1/reviewer/assignments/" + manuscriptId + "/reviews")
                        .header("Authorization", "Bearer " + reviewerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.recommendation").value("Minor Revision"))
                .andExpect(jsonPath("$.data.technicalQuality").value(4))
                .andExpect(jsonPath("$.data.overallScore").value(4.5))
                .andExpect(jsonPath("$.data.rubricScores", hasSize(11)));

        // Manuscript status should now be 'Review Completed'
        Manuscript updatedPostReview = manuscriptRepository.findById(manuscriptId).orElseThrow();
        assertThat(updatedPostReview.getStatus()).isEqualTo("Review Completed");

        // -------------------------------------------------------------
        // Step 6: Editor requests revision from Author
        // -------------------------------------------------------------
        EditorialDecisionRequest revisionDecision = new EditorialDecisionRequest();
        revisionDecision.setDecision("REQUEST_REVISION");
        revisionDecision.setComments("Please address reviewer comments regarding Table 2 benchmarks.");

        mockMvc.perform(post("/api/v1/editor/manuscripts/" + manuscriptId + "/decision")
                        .header("Authorization", "Bearer " + editorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revisionDecision)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("Revision Requested"));

        // -------------------------------------------------------------
        // Step 7: Author uploads revision (sent to Editor first)
        // -------------------------------------------------------------
        String authorJwt = jwtTokenProvider.generateToken(createdAuthor.getEmail(), createdAuthor.getRole().name(), createdAuthor.getId());

        RevisionSubmissionRequest revisionSub = new RevisionSubmissionRequest(
                "Table 2 has been updated with carrier mobility statistics.",
                new FileMetadataRequest("Revised_Graphene_Paper_v2.pdf", 4300000L, "application/pdf")
        );

        mockMvc.perform(post("/api/v1/author/manuscripts/" + manuscriptId + "/revision")
                        .header("Authorization", "Bearer " + authorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revisionSub)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("Revision Submitted"))
                .andExpect(jsonPath("$.data.revisionForwardedToReviewer").value(false));

        // -------------------------------------------------------------
        // Step 7b: Editor reviews resubmission and explicitly forwards to Reviewer
        // -------------------------------------------------------------
        ForwardRevisionRequest forwardReq = new ForwardRevisionRequest(7, "Please re-evaluate Table 2 data.");
        mockMvc.perform(post("/api/v1/editor/manuscripts/" + manuscriptId + "/forward-revision")
                        .header("Authorization", "Bearer " + editorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forwardReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("Under Review"))
                .andExpect(jsonPath("$.data.revisionForwardedToReviewer").value(true));

        // -------------------------------------------------------------
        // Step 8: Editor accepts and publishes the manuscript
        // -------------------------------------------------------------
        EditorialDecisionRequest acceptDecision = new EditorialDecisionRequest();
        acceptDecision.setDecision("ACCEPT");
        acceptDecision.setComments("Revisions are satisfactory.");

        mockMvc.perform(post("/api/v1/editor/manuscripts/" + manuscriptId + "/decision")
                        .header("Authorization", "Bearer " + editorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acceptDecision)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("Accepted"));

        EditorialDecisionRequest publishDecision = new EditorialDecisionRequest();
        publishDecision.setDecision("PUBLISH");
        publishDecision.setDoi("10.5555/ijspast.2026.9999");
        publishDecision.setVolume("Vol. 14");
        publishDecision.setIssue("Issue 4");

        mockMvc.perform(post("/api/v1/editor/manuscripts/" + manuscriptId + "/decision")
                        .header("Authorization", "Bearer " + editorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(publishDecision)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("Published"))
                .andExpect(jsonPath("$.data.publicationDetails.doi").value("10.5555/ijspast.2026.9999"));

        // -------------------------------------------------------------
        // Step 9: Published paper is publicly visible in open access archive
        // -------------------------------------------------------------
        mockMvc.perform(get("/api/v1/published?search=Graphene"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[?(@.id == '" + manuscriptId + "')].status").value("Published"));
    }

    @Test
    @DisplayName("Workflow Requirement: First Submission creates account + credentials email; Corrections & Resubmission updates existing submission without generating credentials or emailing")
    void testFirstSubmissionAndResubmissionWorkflow() throws Exception {
        String testEmail = "resub.author." + System.currentTimeMillis() + "@srmu.ac.in";
        String initialTitle = "Original Title on Quantum Dot Superconductors";

        AuthorInfoRequest authorInfo = new AuthorInfoRequest(
                "Dr. Amit Verma",
                testEmail,
                "Shri Ramswaroop Memorial University",
                "Dept. of Physics",
                "Lucknow, India",
                "0000-0001-2345-6789"
        );

        ManuscriptSubmissionRequest firstSubmissionReq = new ManuscriptSubmissionRequest();
        firstSubmissionReq.setAuthor(authorInfo);
        firstSubmissionReq.setTitle(initialTitle);
        firstSubmissionReq.setDomain("Applied Science, Physics & Sensor Calibration");
        firstSubmissionReq.setAbstractText("Initial abstract describing preliminary quantum dot synthesis.");
        firstSubmissionReq.setKeywords("Quantum Dots, Superconductivity, Nanomaterials");
        firstSubmissionReq.setCoauthors("Dr. Rahul Mishra");
        firstSubmissionReq.setDeclaration(true);
        firstSubmissionReq.setFile(new FileMetadataRequest("Quantum_Draft_v1.pdf", 3100000L, "application/pdf"));

        long initialEmailCount = emailNotificationLogRepository.count();

        // 1. FIRST SUBMISSION: Creates user account, generates credentials, sends email
        String firstResp = mockMvc.perform(post("/api/v1/manuscripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstSubmissionReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.manuscriptId").isNotEmpty())
                .andExpect(jsonPath("$.data.generatedCredentials.email").value(testEmail))
                .andExpect(jsonPath("$.data.generatedCredentials.password").isNotEmpty())
                .andExpect(jsonPath("$.data.resubmission").value(false))
                .andReturn().getResponse().getContentAsString();

        String manuscriptId = objectMapper.readTree(firstResp).path("data").path("manuscriptId").asText();

        // Verify author user was created with a hashed password
        User userAfterFirst = userRepository.findByEmailIgnoreCase(testEmail).orElseThrow();
        String originalPasswordHash = userAfterFirst.getPasswordHash();
        assertThat(originalPasswordHash).isNotEmpty();

        // Verify email log was created for credentials
        long emailCountAfterFirst = emailNotificationLogRepository.count();
        assertThat(emailCountAfterFirst).isEqualTo(initialEmailCount + 1);

        // Verify manuscript created
        long countBeforeResubmission = manuscriptRepository.count();
        Manuscript firstManuscript = manuscriptRepository.findById(manuscriptId).orElseThrow();
        assertThat(firstManuscript.getAbstractText()).isEqualTo("Initial abstract describing preliminary quantum dot synthesis.");

        // 2. CORRECTION AND RESUBMISSION: Same author resubmits with corrections to abstract, keywords, coauthors, and file
        ManuscriptSubmissionRequest correctionReq = new ManuscriptSubmissionRequest();
        correctionReq.setAuthor(authorInfo);
        correctionReq.setTitle(initialTitle); // Same paper title
        correctionReq.setDomain("Applied Science, Physics & Sensor Calibration");
        correctionReq.setAbstractText("CORRECTED ABSTRACT: Comprehensive derivations for quantum dot synthesis and experimental calibration.");
        correctionReq.setKeywords("Quantum Dots, Superconductivity, Nanomaterials, Calibration");
        correctionReq.setCoauthors("Dr. Rahul Mishra, Prof. S. Sen");
        correctionReq.setDeclaration(true);
        correctionReq.setFile(new FileMetadataRequest("Quantum_Corrected_v2.pdf", 3400000L, "application/pdf"));

        mockMvc.perform(post("/api/v1/manuscripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctionReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.manuscriptId").value(manuscriptId))
                .andExpect(jsonPath("$.data.generatedCredentials.email").value(testEmail))
                .andExpect(jsonPath("$.data.generatedCredentials.password").doesNotExist()) // No new password generated
                .andExpect(jsonPath("$.data.dispatchedEmail").doesNotExist()) // No account creation email sent
                .andExpect(jsonPath("$.data.resubmission").value(true));

        // Total manuscripts count must NOT have increased!
        long countAfterResubmission = manuscriptRepository.count();
        assertThat(countAfterResubmission).isEqualTo(countBeforeResubmission);

        // Existing manuscript record must have updated fields
        Manuscript updatedManuscript = manuscriptRepository.findById(manuscriptId).orElseThrow();
        assertThat(updatedManuscript.getAbstractText()).startsWith("CORRECTED ABSTRACT:");
        assertThat(updatedManuscript.getKeywords()).contains("Calibration");
        assertThat(updatedManuscript.getCoauthors()).contains("Prof. S. Sen");
        assertThat(manuscriptFileRepository.findByManuscriptOrderByVersionAsc(updatedManuscript)).hasSizeGreaterThanOrEqualTo(2);

        // Author login credentials must remain UNCHANGED
        User userAfterResubmission = userRepository.findByEmailIgnoreCase(testEmail).orElseThrow();
        assertThat(userAfterResubmission.getPasswordHash()).isEqualTo(originalPasswordHash);

        // No new account-creation email sent during resubmission
        long emailCountAfterResubmission = emailNotificationLogRepository.count();
        assertThat(emailCountAfterResubmission).isEqualTo(emailCountAfterFirst);
    }

    @Test
    @DisplayName("Workflow Requirement: When Reviewer 1 and Reviewer 2 are both assigned, Editor must wait until both submit before sending consolidated feedback")
    void testDualReviewerConsolidationRequirement() throws Exception {
        String authorEmail = "dual.review.author." + System.currentTimeMillis() + "@srmu.ac.in";
        ManuscriptSubmissionRequest submissionReq = new ManuscriptSubmissionRequest();
        submissionReq.setAuthor(new AuthorInfoRequest("Dr. Priya Sharma", authorEmail, "SRMU", "CSE", "Lucknow", "0000-0002-1234-5678"));
        submissionReq.setTitle("Dual Reviewer Testing on Neural Architecture Search");
        submissionReq.setDomain("Artificial Intelligence and Machine Learning (Computing & AI)");
        submissionReq.setAbstractText("Paper on neural architecture search efficiency.");
        submissionReq.setKeywords("AI, NAS, Deep Learning");
        submissionReq.setDeclaration(true);
        submissionReq.setFile(new FileMetadataRequest("NAS_Paper.pdf", 2500000L, "application/pdf"));

        String subResp = mockMvc.perform(post("/api/v1/manuscripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submissionReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String manuscriptId = objectMapper.readTree(subResp).path("data").path("manuscriptId").asText();

        // Register Reviewer 1 and Reviewer 2
        User r1 = userRepository.save(new User("rev_dual_1_" + System.currentTimeMillis(), "Dr. Reviewer One", "rev1." + System.currentTimeMillis() + "@test.edu", "Rev@123", UserRole.ROLE_REVIEWER));
        User r2 = userRepository.save(new User("rev_dual_2_" + System.currentTimeMillis(), "Dr. Reviewer Two", "rev2." + System.currentTimeMillis() + "@test.edu", "Rev@123", UserRole.ROLE_REVIEWER));

        User editor = userRepository.findByEmailIgnoreCase(editorEmail).orElseThrow();
        String editorJwt = jwtTokenProvider.generateToken(editor.getEmail(), editor.getRole().name(), editor.getId());

        // Assign dual reviewers
        AssignReviewerRequest assignReq = new AssignReviewerRequest(r1.getId(), r1.getName(), 14);
        assignReq.setReviewer2Id(r2.getId());
        assignReq.setReviewer2Name(r2.getName());

        mockMvc.perform(post("/api/v1/editor/manuscripts/" + manuscriptId + "/assign-reviewer")
                        .header("Authorization", "Bearer " + editorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk());

        // 1. Only Reviewer 1 submits review
        String r1Jwt = jwtTokenProvider.generateToken(r1.getEmail(), r1.getRole().name(), r1.getId());
        SubmitReviewRequest r1Review = new SubmitReviewRequest();
        r1Review.setRecommendation("Major Revision");
        r1Review.setTechnicalQuality(3);
        r1Review.setOriginality(4);
        r1Review.setClarity(3);
        r1Review.setStage(1);
        r1Review.setCommentsForAuthor("Reviewer 1 comments: Expand search space benchmark.");
        r1Review.setCommentsForEditor("Promising but needs more baseline comparisons.");

        mockMvc.perform(post("/api/v1/reviewer/assignments/" + manuscriptId + "/reviews")
                        .header("Authorization", "Bearer " + r1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r1Review)))
                .andExpect(status().isCreated());

        // 2. Editor attempts to send revision feedback to author before Reviewer 2 submits -> MUST BE BLOCKED (400 Bad Request)
        EditorialDecisionRequest earlyDecision = new EditorialDecisionRequest();
        earlyDecision.setDecision("REQUEST_REVISION");
        earlyDecision.setComments("Premature revision request.");

        mockMvc.perform(post("/api/v1/editor/manuscripts/" + manuscriptId + "/decision")
                        .header("Authorization", "Bearer " + editorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(earlyDecision)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("Both Reviewer 1 and Reviewer 2 must submit their evaluations before consolidated feedback can be sent")));

        // 3. Now Reviewer 2 also submits feedback
        String r2Jwt = jwtTokenProvider.generateToken(r2.getEmail(), r2.getRole().name(), r2.getId());
        SubmitReviewRequest r2Review = new SubmitReviewRequest();
        r2Review.setRecommendation("Minor Revision");
        r2Review.setTechnicalQuality(4);
        r2Review.setOriginality(4);
        r2Review.setClarity(4);
        r2Review.setStage(1);
        r2Review.setCommentsForAuthor("Reviewer 2 comments: Clarify GPU training cost in Table 1.");
        r2Review.setCommentsForEditor("Acceptable after minor GPU efficiency clarifications.");

        mockMvc.perform(post("/api/v1/reviewer/assignments/" + manuscriptId + "/reviews")
                        .header("Authorization", "Bearer " + r2Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r2Review)))
                .andExpect(status().isCreated());

        // 4. Editor now merges and sends consolidated feedback -> MUST SUCCEED (200 OK)
        EditorialDecisionRequest consolidatedDecision = new EditorialDecisionRequest();
        consolidatedDecision.setDecision("REQUEST_REVISION");
        consolidatedDecision.setComments("=== CONSOLIDATED REVIEW REPORT ===\nReviewer 1: Expand search space.\nReviewer 2: Clarify GPU cost.");

        mockMvc.perform(post("/api/v1/editor/manuscripts/" + manuscriptId + "/decision")
                        .header("Authorization", "Bearer " + editorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(consolidatedDecision)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("Revision Requested"))
                .andExpect(jsonPath("$.data.revisionRequestComments", org.hamcrest.Matchers.containsString("CONSOLIDATED REVIEW REPORT")));
    }
}
