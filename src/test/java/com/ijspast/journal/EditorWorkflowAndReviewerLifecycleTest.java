package com.ijspast.journal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ijspast.journal.dto.request.*;
import com.ijspast.journal.dto.response.CommunicationThreadDTO;
import com.ijspast.journal.dto.response.ManuscriptResponseDTO;
import com.ijspast.journal.entity.*;
import com.ijspast.journal.repository.*;
import com.ijspast.journal.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class EditorWorkflowAndReviewerLifecycleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ManuscriptRepository manuscriptRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewAssignmentRepository reviewAssignmentRepository;

    @Autowired
    private ManuscriptCommunicationRepository communicationRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Value("${app.editor.email}")
    private String editorEmail;

    private User editor;
    private String editorJwt;
    private User reviewer1;
    private String reviewer1Jwt;
    private User reviewer2;
    private String reviewer2Jwt;
    private User reviewer3;
    private String reviewer3Jwt;

    @BeforeEach
    void setupUsers() {
        editor = userRepository.findByEmailIgnoreCase(editorEmail).orElseGet(() -> {
            User ed = new User("ed_test", "Editor In Chief", editorEmail, "hashedpass", UserRole.ROLE_EDITOR);
            return userRepository.save(ed);
        });
        editorJwt = jwtTokenProvider.generateToken(editor.getEmail(), editor.getRole().name(), editor.getId());

        reviewer1 = createReviewer("rev1." + System.currentTimeMillis() + "@univ.edu", "Dr. Alice Walker");
        reviewer1Jwt = jwtTokenProvider.generateToken(reviewer1.getEmail(), reviewer1.getRole().name(), reviewer1.getId());

        reviewer2 = createReviewer("rev2." + System.currentTimeMillis() + "@univ.edu", "Dr. Bob Martinez");
        reviewer2Jwt = jwtTokenProvider.generateToken(reviewer2.getEmail(), reviewer2.getRole().name(), reviewer2.getId());

        reviewer3 = createReviewer("rev3." + System.currentTimeMillis() + "@univ.edu", "Dr. Charlie Chen");
        reviewer3Jwt = jwtTokenProvider.generateToken(reviewer3.getEmail(), reviewer3.getRole().name(), reviewer3.getId());
    }

    private User createReviewer(String email, String name) {
        User u = new User("rev_" + System.currentTimeMillis() + "_" + email.hashCode(), name, email, "hashedpass", UserRole.ROLE_REVIEWER);
        u.setInstitution("Test University");
        u.setDomain("Computer Science");
        u.setEnabled(true);
        u.setAccountSetupCompleted(true);
        return userRepository.save(u);
    }

    private String submitNewManuscript(String authorEmail, String authorName) throws Exception {
        AuthorInfoRequest authorInfo = new AuthorInfoRequest(
                authorName, authorEmail, "Global Tech Institute", "Dept. of AI", "New York, USA", "0000-0001-1111-2222"
        );
        ManuscriptSubmissionRequest submissionReq = new ManuscriptSubmissionRequest();
        submissionReq.setAuthor(authorInfo);
        submissionReq.setTitle("Deep Learning for Automated Peer Review Systems: " + System.currentTimeMillis());
        submissionReq.setDomain("Computer Science & Engineering");
        submissionReq.setAbstractText("An extensive study on applying deep neural networks to scientific review validation.");
        submissionReq.setKeywords("Deep Learning, Neural Networks, Peer Review");
        submissionReq.setDeclaration(true);
        submissionReq.setFile(new FileMetadataRequest("paper.pdf", 2500000L, "application/pdf"));

        String res = mockMvc.perform(post("/api/v1/manuscripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submissionReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(res).path("data").path("manuscriptId").asText();
    }

    @Test
    @DisplayName("Requirement 1: Reject button hidden and rejected during active reviewer evaluation")
    void testRequirement1_HideRejectButtonAfterReviewerAssignment() throws Exception {
        String manuscriptId = submitNewManuscript("author1." + System.currentTimeMillis() + "@test.edu", "Dr. John Author");

        // Before assignment: canReject is true
        mockMvc.perform(get("/api/v1/editor/manuscripts/" + manuscriptId)
                        .header("Authorization", "Bearer " + editorJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canReject").value(true));

        // Assign dual reviewers
        AssignReviewerRequest assignReq = new AssignReviewerRequest(reviewer1.getId(), reviewer1.getName(), 15);
        assignReq.setReviewer2Id(reviewer2.getId());
        assignReq.setReviewer2Name(reviewer2.getName());

        mockMvc.perform(post("/api/v1/editor/manuscripts/" + manuscriptId + "/assign-reviewer")
                        .header("Authorization", "Bearer " + editorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canReject").value(false));

        // Active evaluation in progress: attempting to REJECT via API must be blocked with 400
        EditorialDecisionRequest rejectDecision = new EditorialDecisionRequest();
        rejectDecision.setDecision("REJECT");
        rejectDecision.setRejectionReason("Premature rejection attempted while reviewers are evaluating.");

        mockMvc.perform(post("/api/v1/editor/manuscripts/" + manuscriptId + "/decision")
                        .header("Authorization", "Bearer " + editorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectDecision)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("active reviewer evaluation")));
    }

    @Test
    @DisplayName("Requirement 2: Disable Accept button until both reviewers submit feedback (0/2, 1/2, 2/2 Ready)")
    void testRequirement2_DisableAcceptButtonUntilBothReviewersSubmitFeedback() throws Exception {
        String manuscriptId = submitNewManuscript("author2." + System.currentTimeMillis() + "@test.edu", "Dr. Jane Author");

        // Assign dual reviewers
        AssignReviewerRequest assignReq = new AssignReviewerRequest(reviewer1.getId(), reviewer1.getName(), 15);
        assignReq.setReviewer2Id(reviewer2.getId());
        assignReq.setReviewer2Name(reviewer2.getName());

        mockMvc.perform(post("/api/v1/editor/manuscripts/" + manuscriptId + "/assign-reviewer")
                        .header("Authorization", "Bearer " + editorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedbackProgress").value("0/2 Ready"))
                .andExpect(jsonPath("$.data.canAccept").value(false));

        // Editor attempts to ACCEPT at 0/2 Ready -> MUST FAIL with 400
        EditorialDecisionRequest earlyAccept = new EditorialDecisionRequest();
        earlyAccept.setDecision("ACCEPT");
        earlyAccept.setComments("Premature acceptance with 0/2 ready.");

        mockMvc.perform(post("/api/v1/editor/manuscripts/" + manuscriptId + "/decision")
                        .header("Authorization", "Bearer " + editorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(earlyAccept)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("0/2 Ready")));

        // Reviewer 1 submits feedback -> 1/2 Ready
        SubmitReviewRequest r1Review = new SubmitReviewRequest();
        r1Review.setRecommendation("Accept");
        r1Review.setTechnicalQuality(5);
        r1Review.setOriginality(5);
        r1Review.setClarity(4);
        r1Review.setStage(1);
        r1Review.setCommentsForAuthor("Solid contribution.");
        r1Review.setCommentsForEditor("Highly recommended.");

        mockMvc.perform(post("/api/v1/reviewer/assignments/" + manuscriptId + "/reviews")
                        .header("Authorization", "Bearer " + reviewer1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r1Review)))
                .andExpect(status().isCreated());

        // Check progress is 1/2 Ready and canAccept is false
        mockMvc.perform(get("/api/v1/editor/manuscripts/" + manuscriptId)
                        .header("Authorization", "Bearer " + editorJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedbackProgress").value("1/2 Ready"))
                .andExpect(jsonPath("$.data.canAccept").value(false));

        // Editor attempts to ACCEPT at 1/2 Ready -> MUST FAIL with 400
        mockMvc.perform(post("/api/v1/editor/manuscripts/" + manuscriptId + "/decision")
                        .header("Authorization", "Bearer " + editorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(earlyAccept)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("1/2 Ready")));

        // Reviewer 2 submits feedback -> 2/2 Ready
        SubmitReviewRequest r2Review = new SubmitReviewRequest();
        r2Review.setRecommendation("Accept");
        r2Review.setTechnicalQuality(4);
        r2Review.setOriginality(4);
        r2Review.setClarity(5);
        r2Review.setStage(1);
        r2Review.setCommentsForAuthor("Good methodology.");
        r2Review.setCommentsForEditor("Acceptable.");

        mockMvc.perform(post("/api/v1/reviewer/assignments/" + manuscriptId + "/reviews")
                        .header("Authorization", "Bearer " + reviewer2Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r2Review)))
                .andExpect(status().isCreated());

        // Check progress is 2/2 Ready and canAccept is true
        mockMvc.perform(get("/api/v1/editor/manuscripts/" + manuscriptId)
                        .header("Authorization", "Bearer " + editorJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedbackProgress").value("2/2 Ready"))
                .andExpect(jsonPath("$.data.canAccept").value(true));

        // Now Editor accepts -> SUCCEEDS with 200
        EditorialDecisionRequest validAccept = new EditorialDecisionRequest();
        validAccept.setDecision("ACCEPT");
        validAccept.setComments("Both reviewers approved. Manuscript accepted.");

        mockMvc.perform(post("/api/v1/editor/manuscripts/" + manuscriptId + "/decision")
                        .header("Authorization", "Bearer " + editorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAccept)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("Accepted"));
    }

    @Test
    @DisplayName("Requirement 3: Reassign reviewer after 15 calendar days without feedback")
    void testRequirement3_AllowReviewerReassignmentAfter15DaysWithoutFeedback() throws Exception {
        String manuscriptId = submitNewManuscript("author3." + System.currentTimeMillis() + "@test.edu", "Dr. Mark Author");

        // Assign dual reviewers
        AssignReviewerRequest assignReq = new AssignReviewerRequest(reviewer1.getId(), reviewer1.getName(), 15);
        assignReq.setReviewer2Id(reviewer2.getId());
        assignReq.setReviewer2Name(reviewer2.getName());

        mockMvc.perform(post("/api/v1/editor/manuscripts/" + manuscriptId + "/assign-reviewer")
                        .header("Authorization", "Bearer " + editorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewer1Status").value("Pending"))
                .andExpect(jsonPath("$.data.reviewer1CanReassign").value(false));

        // Attempting to reassign Reviewer 1 before 15 days deadline -> MUST FAIL with 400
        ReassignReviewerRequest earlyReassign = new ReassignReviewerRequest(1, reviewer3.getId(), "Trying early reassignment");
        mockMvc.perform(post("/api/v1/editor/manuscripts/" + manuscriptId + "/reassign-reviewer")
                        .header("Authorization", "Bearer " + editorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(earlyReassign)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("only permitted after 15 calendar days")));

        // Simulate 15 days passing for Reviewer 1 (set assigned date to 16 days ago and deadline to 1 day ago)
        Manuscript m = manuscriptRepository.findById(manuscriptId).orElseThrow();
        m.setAssignedReviewer1Date(LocalDate.now().minusDays(16));
        m.setAssignedReviewer1Deadline(LocalDate.now().minusDays(1));
        manuscriptRepository.save(m);

        // Reviewer 1 should now be Overdue and reviewer1CanReassign true
        mockMvc.perform(get("/api/v1/editor/manuscripts/" + manuscriptId)
                        .header("Authorization", "Bearer " + editorJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewer1Status").value("Overdue"))
                .andExpect(jsonPath("$.data.reviewer1CanReassign").value(true))
                .andExpect(jsonPath("$.data.reviewer2Status").value("Pending"))
                .andExpect(jsonPath("$.data.reviewer2CanReassign").value(false));

        // Reassign Reviewer 1 to Reviewer 3
        ReassignReviewerRequest validReassign = new ReassignReviewerRequest(1, reviewer3.getId(), "Reviewer 1 unresponsive after 15 days");
        mockMvc.perform(post("/api/v1/editor/manuscripts/" + manuscriptId + "/reassign-reviewer")
                        .header("Authorization", "Bearer " + editorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReassign)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignedReviewerName").value(reviewer3.getName()))
                .andExpect(jsonPath("$.data.reviewer1Status").value("Pending"))
                .andExpect(jsonPath("$.data.reviewer1CanReassign").value(false))
                // Reviewer 2 was NOT reset and remains assigned
                .andExpect(jsonPath("$.data.assignedReviewer2Name").value(reviewer2.getName()));

        // Verify Reviewer 3 receives fresh 15-day deadline
        Manuscript updated = manuscriptRepository.findById(manuscriptId).orElseThrow();
        assertThat(updated.getAssignedReviewer().getId()).isEqualTo(reviewer3.getId());
        assertThat(updated.getAssignedReviewer1Deadline()).isEqualTo(LocalDate.now().plusDays(15));
    }

    @Test
    @DisplayName("Requirement 4: Add Editor Comments when merging feedback, saved and masked from Author")
    void testRequirement4_EditorCommentsFieldWhenMergingFeedback() throws Exception {
        String manuscriptId = submitNewManuscript("author4." + System.currentTimeMillis() + "@test.edu", "Dr. Paul Author");

        // Assign dual reviewers
        AssignReviewerRequest assignReq = new AssignReviewerRequest(reviewer1.getId(), reviewer1.getName(), 15);
        assignReq.setReviewer2Id(reviewer2.getId());
        assignReq.setReviewer2Name(reviewer2.getName());

        mockMvc.perform(post("/api/v1/editor/manuscripts/" + manuscriptId + "/assign-reviewer")
                        .header("Authorization", "Bearer " + editorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk());

        // Submit both reviews
        SubmitReviewRequest r1Review = new SubmitReviewRequest();
        r1Review.setRecommendation("Major Revision");
        r1Review.setTechnicalQuality(4);
        r1Review.setOriginality(4);
        r1Review.setClarity(4);
        r1Review.setCommentsForAuthor("Rev 1 remarks: Section 2 is unclear.");
        r1Review.setCommentsForEditor("Needs rewrite.");
        mockMvc.perform(post("/api/v1/reviewer/assignments/" + manuscriptId + "/reviews")
                        .header("Authorization", "Bearer " + reviewer1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r1Review)))
                .andExpect(status().isCreated());

        SubmitReviewRequest r2Review = new SubmitReviewRequest();
        r2Review.setRecommendation("Minor Revision");
        r2Review.setTechnicalQuality(4);
        r2Review.setOriginality(4);
        r2Review.setClarity(4);
        r2Review.setCommentsForAuthor("Rev 2 remarks: Check figure 3 labels.");
        r2Review.setCommentsForEditor("Minor issues.");
        mockMvc.perform(post("/api/v1/reviewer/assignments/" + manuscriptId + "/reviews")
                        .header("Authorization", "Bearer " + reviewer2Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r2Review)))
                .andExpect(status().isCreated());

        // Editor requests revision with Editor Comments field
        EditorialDecisionRequest revDecision = new EditorialDecisionRequest();
        revDecision.setDecision("REQUEST_REVISION");
        revDecision.setComments("Consolidated feedback: Address comments from both reviewers.");
        revDecision.setEditorComments("Editor-in-Chief note: Please pay special attention to the mathematical proof in Section 4.");

        mockMvc.perform(post("/api/v1/editor/manuscripts/" + manuscriptId + "/decision")
                        .header("Authorization", "Bearer " + editorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revDecision)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("Revision Requested"))
                .andExpect(jsonPath("$.data.editorComments").value("Editor-in-Chief note: Please pay special attention to the mathematical proof in Section 4."));

        // Verify Author views manuscript: receives editorComments, but reviewer names/emails remain double-blind masked
        User authorUser = userRepository.findByEmailIgnoreCase(manuscriptRepository.findById(manuscriptId).get().getAuthorEmail()).orElseThrow();
        String authorJwt = jwtTokenProvider.generateToken(authorUser.getEmail(), authorUser.getRole().name(), authorUser.getId());

        mockMvc.perform(get("/api/v1/author/manuscripts/" + manuscriptId)
                        .header("Authorization", "Bearer " + authorJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.editorComments").value("Editor-in-Chief note: Please pay special attention to the mathematical proof in Section 4."))
                .andExpect(jsonPath("$.data.assignedReviewerName").doesNotExist())
                .andExpect(jsonPath("$.data.assignedReviewer2Name").doesNotExist());
    }

    @Test
    @DisplayName("Requirement 5: Reviewer roles end after two feedback submissions")
    void testRequirement5_EndReviewerRoleAfterTwoFeedbackSubmissions() throws Exception {
        String manuscriptId = submitNewManuscript("author5." + System.currentTimeMillis() + "@test.edu", "Dr. Sarah Author");

        // Assign dual reviewers
        AssignReviewerRequest assignReq = new AssignReviewerRequest(reviewer1.getId(), reviewer1.getName(), 15);
        assignReq.setReviewer2Id(reviewer2.getId());
        assignReq.setReviewer2Name(reviewer2.getName());

        mockMvc.perform(post("/api/v1/editor/manuscripts/" + manuscriptId + "/assign-reviewer")
                        .header("Authorization", "Bearer " + editorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk());

        // First submission by Reviewer 1 -> count is 1/2
        SubmitReviewRequest r1Review = new SubmitReviewRequest();
        r1Review.setRecommendation("Major Revision");
        r1Review.setTechnicalQuality(4);
        r1Review.setOriginality(4);
        r1Review.setClarity(4);
        r1Review.setCommentsForAuthor("Initial evaluation remarks.");
        r1Review.setCommentsForEditor("Needs substantial work.");
        mockMvc.perform(post("/api/v1/reviewer/assignments/" + manuscriptId + "/reviews")
                        .header("Authorization", "Bearer " + reviewer1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r1Review)))
                .andExpect(status().isCreated());

        // Reviewer 1 attempts immediate second submission -> MUST BE BLOCKED (form locked until editor forwards revision)
        mockMvc.perform(post("/api/v1/reviewer/assignments/" + manuscriptId + "/reviews")
                        .header("Authorization", "Bearer " + reviewer1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r1Review)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Second feedback submission is locked")));

        // Reviewer 2 submits round 1 feedback
        SubmitReviewRequest r2Review = new SubmitReviewRequest();
        r2Review.setRecommendation("Major Revision");
        r2Review.setTechnicalQuality(4);
        r2Review.setOriginality(4);
        r2Review.setClarity(4);
        r2Review.setCommentsForAuthor("Reviewer 2 remarks.");
        r2Review.setCommentsForEditor("Promising.");
        mockMvc.perform(post("/api/v1/reviewer/assignments/" + manuscriptId + "/reviews")
                        .header("Authorization", "Bearer " + reviewer2Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r2Review)))
                .andExpect(status().isCreated());

        // Editor requests revision
        EditorialDecisionRequest revDecision = new EditorialDecisionRequest();
        revDecision.setDecision("REQUEST_REVISION");
        revDecision.setComments("Please revise based on feedback.");
        mockMvc.perform(post("/api/v1/editor/manuscripts/" + manuscriptId + "/decision")
                        .header("Authorization", "Bearer " + editorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revDecision)))
                .andExpect(status().isOk());

        // Author uploads revision
        User authorUser = userRepository.findByEmailIgnoreCase(manuscriptRepository.findById(manuscriptId).get().getAuthorEmail()).orElseThrow();
        String authorJwt = jwtTokenProvider.generateToken(authorUser.getEmail(), authorUser.getRole().name(), authorUser.getId());
        RevisionSubmissionRequest revSub = new RevisionSubmissionRequest(
                "All reviewer points addressed.",
                new FileMetadataRequest("paper_revised.pdf", 2600000L, "application/pdf")
        );
        mockMvc.perform(post("/api/v1/author/manuscripts/" + manuscriptId + "/revision")
                        .header("Authorization", "Bearer " + authorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revSub)))
                .andExpect(status().isOk());

        // Editor explicitly forwards revised manuscript to reviewers for Round 2
        ForwardRevisionRequest fwdReq = new ForwardRevisionRequest(15, "Please re-evaluate author revisions.");
        mockMvc.perform(post("/api/v1/editor/manuscripts/" + manuscriptId + "/forward-revision")
                        .header("Authorization", "Bearer " + editorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fwdReq)))
                .andExpect(status().isOk());

        // Second submission by Reviewer 1 -> count is 2/2
        SubmitReviewRequest r1Round2 = new SubmitReviewRequest();
        r1Round2.setRecommendation("Accept");
        r1Round2.setStage(2);
        r1Round2.setTechnicalQuality(5);
        r1Round2.setOriginality(5);
        r1Round2.setClarity(5);
        r1Round2.setCommentsForAuthor("Revisions satisfactory.");
        r1Round2.setCommentsForEditor("Paper is ready for publication.");
        mockMvc.perform(post("/api/v1/reviewer/assignments/" + manuscriptId + "/reviews")
                        .header("Authorization", "Bearer " + reviewer1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r1Round2)))
                .andExpect(status().isCreated());

        // Check Reviewer 1 role ended in DTO
        mockMvc.perform(get("/api/v1/editor/manuscripts/" + manuscriptId)
                        .header("Authorization", "Bearer " + editorJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewer1SubmissionCount").value(2))
                .andExpect(jsonPath("$.data.reviewer1RoleEnded").value(true));

        // Attempt third submission by Reviewer 1 -> MUST BE PERMANENTLY BLOCKED WITH 400
        mockMvc.perform(post("/api/v1/reviewer/assignments/" + manuscriptId + "/reviews")
                        .header("Authorization", "Bearer " + reviewer1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r1Round2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("maximum permitted feedback submissions (2/2)")));
    }

    @Test
    @DisplayName("Requirement 6: Five Editor-Author communication exchanges ceiling enforced after reviewer evaluation")
    void testRequirement6_FiveEditorAuthorCommunicationsCeiling() throws Exception {
        String manuscriptId = submitNewManuscript("author6." + System.currentTimeMillis() + "@test.edu", "Dr. Robert Author");
        User authorUser = userRepository.findByEmailIgnoreCase(manuscriptRepository.findById(manuscriptId).get().getAuthorEmail()).orElseThrow();
        String authorJwt = jwtTokenProvider.generateToken(authorUser.getEmail(), authorUser.getRole().name(), authorUser.getId());

        // 1. Before post-review: communications disabled
        mockMvc.perform(get("/api/v1/editor/manuscripts/" + manuscriptId + "/communications")
                        .header("Authorization", "Bearer " + editorJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canSendMessage").value(false))
                .andExpect(jsonPath("$.data.exchangesUsed").value(0))
                .andExpect(jsonPath("$.data.maxExchanges").value(5));

        SendMessageRequest msg1 = new SendMessageRequest("Message 1: Editor checking in.");
        mockMvc.perform(post("/api/v1/editor/manuscripts/" + manuscriptId + "/communications")
                        .header("Authorization", "Bearer " + editorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msg1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("only enabled after the reviewer evaluation process has ended")));

        // Transition manuscript to post-review state (e.g. Accepted)
        Manuscript m = manuscriptRepository.findById(manuscriptId).orElseThrow();
        m.setStatus("Accepted");
        manuscriptRepository.save(m);

        // 2. Exchange 1: Editor sends message 1 -> 1/5 used
        mockMvc.perform(post("/api/v1/editor/manuscripts/" + manuscriptId + "/communications")
                        .header("Authorization", "Bearer " + editorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("Welcome to the final formatting stage."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exchangesUsed").value(1))
                .andExpect(jsonPath("$.data.canSendMessage").value(true));

        // 3. Exchange 2: Author sends message 2 -> 2/5 used
        mockMvc.perform(post("/api/v1/author/manuscripts/" + manuscriptId + "/communications")
                        .header("Authorization", "Bearer " + authorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("Thank you editor, high-res figures are prepared."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exchangesUsed").value(2));

        // 4. Exchange 3: Editor sends message 3 -> 3/5 used
        mockMvc.perform(post("/api/v1/editor/manuscripts/" + manuscriptId + "/communications")
                        .header("Authorization", "Bearer " + editorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("Please check the copyright transfer form."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exchangesUsed").value(3));

        // 5. Exchange 4: Author sends message 4 -> 4/5 used
        mockMvc.perform(post("/api/v1/author/manuscripts/" + manuscriptId + "/communications")
                        .header("Authorization", "Bearer " + authorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("Copyright transfer signed and returned."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exchangesUsed").value(4));

        // 6. Exchange 5: Editor sends message 5 -> 5/5 used (Limit reached)
        mockMvc.perform(post("/api/v1/editor/manuscripts/" + manuscriptId + "/communications")
                        .header("Authorization", "Bearer " + editorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("Acknowledged. All set for production."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exchangesUsed").value(5))
                .andExpect(jsonPath("$.data.canSendMessage").value(false));

        // 7. Attempt 6th message: Author attempts to send -> MUST BE BLOCKED WITH 400
        mockMvc.perform(post("/api/v1/author/manuscripts/" + manuscriptId + "/communications")
                        .header("Authorization", "Bearer " + authorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("Extra 6th message should be rejected."))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The maximum of five communication exchanges has been reached."));

        // Reviewer accounts must NEVER be allowed access to editor-author communications
        mockMvc.perform(get("/api/v1/editor/manuscripts/" + manuscriptId + "/communications")
                        .header("Authorization", "Bearer " + reviewer1Jwt))
                .andExpect(status().isForbidden());
    }
}
