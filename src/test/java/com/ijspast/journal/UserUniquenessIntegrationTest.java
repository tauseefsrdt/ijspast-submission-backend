package com.ijspast.journal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ijspast.journal.dto.request.AuthorInfoRequest;
import com.ijspast.journal.dto.request.AuthorRegistrationRequest;
import com.ijspast.journal.dto.request.CreateEditorRequest;
import com.ijspast.journal.dto.request.FileMetadataRequest;
import com.ijspast.journal.dto.request.ManuscriptSubmissionRequest;
import com.ijspast.journal.dto.request.UpdateProfileRequest;
import com.ijspast.journal.dto.response.ApiResponse;
import com.ijspast.journal.entity.User;
import com.ijspast.journal.entity.UserRole;
import com.ijspast.journal.repository.UserRepository;
import com.ijspast.journal.security.JwtTokenProvider;
import com.ijspast.journal.service.DataIntegrityAuditService;
import com.ijspast.journal.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class UserUniquenessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private DataIntegrityAuditService auditService;

    @Value("${app.editor.email}")
    private String editorEmail;

    private ManuscriptSubmissionRequest createSubmissionRequest(String name, String email, String phone, String title) {
        ManuscriptSubmissionRequest req = new ManuscriptSubmissionRequest();
        req.setTitle(title);
        req.setDomain("Computing & AI");
        req.setAbstractText("This is a comprehensive evaluation of quantum neural network architectures for predictive modeling, requiring a length between one hundred fifty and two hundred fifty words to satisfy journal requirements. The paper discusses convergence, efficiency, and benchmark performance across various synthetic and realistic datasets.");
        req.setKeywords("Machine Learning, Neural Networks, AI, Deep Learning, Statistics");
        req.setCoauthors("Dr. CoAuthor One, Dr. CoAuthor Two");

        AuthorInfoRequest author = new AuthorInfoRequest(
                name,
                email,
                "University of Technology",
                "Computer Science",
                "Lucknow, India",
                "0000-0002-1825-0097",
                phone
        );
        req.setAuthor(author);

        FileMetadataRequest file = new FileMetadataRequest("paper.pdf", 2500000L, "application/pdf");
        req.setFile(file);
        return req;
    }

    @Test
    @DisplayName("Req 1 & 9: Audit service should detect clean DB with zero duplicates and safely resolve any conflicts")
    void testAuditService() {
        DataIntegrityAuditService.AuditSummary summary = auditService.auditAndSafelyResolveExistingRecords();
        assertNotNull(summary);
        assertTrue(summary.getTotalUsersAudited() >= 1);
        assertEquals(0, summary.getDuplicateEmailsFound());
        assertEquals(0, summary.getDuplicatePhonesFound());
        assertEquals(0, summary.getCrossRoleDuplicatesFound());
    }

    @Test
    @DisplayName("Req 2 & 5: Case-insensitive email duplicate check & whitespace trimming on Author registration")
    void testCaseInsensitiveEmailDuplicateAuthor() throws Exception {
        String baseEmail = "author.uniqueness." + System.currentTimeMillis() + "@univ.edu";

        // Register first author via submission
        ManuscriptSubmissionRequest sub1 = createSubmissionRequest(
                "Author One",
                baseEmail,
                "+91 91234 56701",
                "Quantum Machine Learning Paper 1"
        );

        mockMvc.perform(post("/api/v1/manuscripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sub1)))
                .andExpect(status().isCreated());

        // Attempt to register a SECOND author with uppercase + whitespace variations of the same email
        String duplicateEmailVariation = "  " + baseEmail.toUpperCase() + "  ";
        AuthorRegistrationRequest dupAuthor = new AuthorRegistrationRequest(
                "Author Two Duplicate",
                duplicateEmailVariation,
                "+91 91234 56702",
                "Secret@123",
                "Another University",
                "Dept of Physics",
                "Delhi, India",
                "0000-0003-1234-5678"
        );

        // Standalone registration should reject duplicate email with 409 Conflict
        mockMvc.perform(post("/api/v1/auth/register-author")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupAuthor)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsStringIgnoringCase("already exists")));
    }

    @Test
    @DisplayName("Req 3 & 6: Phone normalization duplicate check across different formatting styles")
    void testPhoneNormalizationDuplicateAuthor() throws Exception {
        String email1 = "author1.phone." + System.currentTimeMillis() + "@univ.edu";
        String email2 = "author2.phone." + System.currentTimeMillis() + "@univ.edu";

        // Author 1 registers with international formatted phone: +91 98765 43210
        ManuscriptSubmissionRequest sub1 = createSubmissionRequest(
                "Author Phone One",
                email1,
                "+91 98765 43210",
                "Paper On Sensor Calibration"
        );

        mockMvc.perform(post("/api/v1/manuscripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sub1)))
                .andExpect(status().isCreated());

        // Author 2 attempts to register with different formatting of same number: "09876543210" (leading trunk 0)
        ManuscriptSubmissionRequest sub2 = createSubmissionRequest(
                "Author Phone Two",
                email2,
                "09876543210",
                "Paper On Different Topic"
        );

        mockMvc.perform(post("/api/v1/manuscripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sub2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsStringIgnoringCase("phone number")));

        // Author 2 attempts with hyphenated 10 digits without prefix: "98765-43210"
        ManuscriptSubmissionRequest sub3 = createSubmissionRequest(
                "Author Phone Three",
                email2,
                "98765-43210",
                "Paper On Yet Another Topic"
        );

        mockMvc.perform(post("/api/v1/manuscripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sub3)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsStringIgnoringCase("phone number")));
    }

    @Test
    @DisplayName("Req 4: Cross-role email validation: Author cannot use Editor email")
    void testCrossRoleEmailValidationAuthorUsesEditorEmail() throws Exception {
        // Attempt to submit a manuscript using the seeded Editor's email address
        ManuscriptSubmissionRequest sub = createSubmissionRequest(
                "Malicious Author",
                "  " + editorEmail.toUpperCase() + "  ", // with case variations and whitespace
                "+91 93333 44444",
                "Attempted Hijack Paper"
        );

        mockMvc.perform(post("/api/v1/manuscripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sub)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsStringIgnoringCase("editor account already exists with this email address")))
                .andExpect(jsonPath("$.message", containsStringIgnoringCase("Authors and editors cannot share the same email address")));
    }

    @Test
    @DisplayName("Req 4: Cross-role email validation: Editor cannot be created with Author's email")
    void testCrossRoleEmailValidationEditorUsesAuthorEmail() throws Exception {
        String authorEmail = "author.for.cross." + System.currentTimeMillis() + "@test.edu";

        // Register author
        ManuscriptSubmissionRequest sub = createSubmissionRequest(
                "Dr. Cross Author",
                authorEmail,
                "+91 94444 55555",
                "Cross Role Baseline Paper"
        );

        mockMvc.perform(post("/api/v1/manuscripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sub)))
                .andExpect(status().isCreated());

        // Prepare editor token
        User editorUser = userRepository.findByEmailIgnoreCase(editorEmail).orElseThrow();
        String editorToken = jwtTokenProvider.generateToken(editorUser.getEmail(), editorUser.getRole().name(), editorUser.getId());

        // Attempt to create a new editor using author's email
        CreateEditorRequest editorReq = new CreateEditorRequest(
                "New Editor Conflict",
                " " + authorEmail.toUpperCase() + " ",
                "+91 97777 88888",
                "EditorPassword@123",
                "SRMU",
                "Editorial"
        );

        mockMvc.perform(post("/api/v1/editor/editors")
                        .header("Authorization", "Bearer " + editorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editorReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsStringIgnoringCase("author account already exists with this email address")))
                .andExpect(jsonPath("$.message", containsStringIgnoringCase("Authors and editors cannot share the same email address")));
    }

    @Test
    @DisplayName("Req 4: Cross-role phone validation: Author cannot use Editor's phone number")
    void testCrossRolePhoneValidationAuthorUsesEditorPhone() throws Exception {
        // Set phone on Editor
        User editorUser = userRepository.findByEmailIgnoreCase(editorEmail).orElseThrow();
        editorUser.setPhone("+919888877777");
        userRepository.save(editorUser);

        // Attempt to submit manuscript with Editor's phone formatted differently: "09888877777"
        String authorEmail = "author.phone.cross." + System.currentTimeMillis() + "@univ.edu";
        ManuscriptSubmissionRequest sub = createSubmissionRequest(
                "Author Phone Clashing",
                authorEmail,
                "09888877777", // leading zero format of +91 98888 77777
                "Sensor Calibration Paper"
        );

        mockMvc.perform(post("/api/v1/manuscripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sub)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsStringIgnoringCase("editor account already exists with this phone number")))
                .andExpect(jsonPath("$.message", containsStringIgnoringCase("Authors and editors cannot share the same phone number")));
    }

    @Test
    @DisplayName("Req 4: Cross-role phone validation: Editor cannot be created with Author's phone number")
    void testCrossRolePhoneValidationEditorUsesAuthorPhone() throws Exception {
        String authorEmail = "author.phone.author." + System.currentTimeMillis() + "@univ.edu";
        String authorPhone = "+91 92222 33333";

        // Register author with phone
        ManuscriptSubmissionRequest sub = createSubmissionRequest(
                "Dr. Phone Author",
                authorEmail,
                authorPhone,
                "Robotics Navigation Paper"
        );

        mockMvc.perform(post("/api/v1/manuscripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sub)))
                .andExpect(status().isCreated());

        // Prepare editor token
        User editorUser = userRepository.findByEmailIgnoreCase(editorEmail).orElseThrow();
        String editorToken = jwtTokenProvider.generateToken(editorUser.getEmail(), editorUser.getRole().name(), editorUser.getId());

        // Attempt to create editor with same phone formatted as "9222233333"
        CreateEditorRequest editorReq = new CreateEditorRequest(
                "Dr. New Clashing Editor",
                "new.editor." + System.currentTimeMillis() + "@journal.edu",
                "9222233333",
                "Pass@12345",
                "SRMU",
                "Editorial"
        );

        mockMvc.perform(post("/api/v1/editor/editors")
                        .header("Authorization", "Bearer " + editorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editorReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsStringIgnoringCase("author account already exists with this phone number")))
                .andExpect(jsonPath("$.message", containsStringIgnoringCase("Authors and editors cannot share the same phone number")));
    }

    @Test
    @DisplayName("Req 7: Profile update validation rejects duplicate email and phone")
    void testProfileUpdateUniqueness() throws Exception {
        // Author A
        String emailA = "authorA." + System.currentTimeMillis() + "@test.edu";
        String phoneA = "+91 96666 11111";
        ManuscriptSubmissionRequest subA = createSubmissionRequest("Author A", emailA, phoneA, "Paper A");
        mockMvc.perform(post("/api/v1/manuscripts").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(subA))).andExpect(status().isCreated());

        // Author B
        String emailB = "authorB." + System.currentTimeMillis() + "@test.edu";
        String phoneB = "+91 96666 22222";
        ManuscriptSubmissionRequest subB = createSubmissionRequest("Author B", emailB, phoneB, "Paper B");
        mockMvc.perform(post("/api/v1/manuscripts").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(subB))).andExpect(status().isCreated());

        User userB = userRepository.findByEmailIgnoreCase(emailB).orElseThrow();
        String tokenB = jwtTokenProvider.generateToken(userB.getEmail(), userB.getRole().name(), userB.getId());

        // 1. Author B tries to update their email to Author A's email -> Rejected
        UpdateProfileRequest emailConflictUpdate = new UpdateProfileRequest("Author B Updated", emailA.toUpperCase(), phoneB, "Univ", "Dept");
        mockMvc.perform(put("/api/v1/auth/profile")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emailConflictUpdate)))
                .andExpect(status().isConflict());

        // 2. Author B tries to update their phone to Author A's phone -> Rejected
        UpdateProfileRequest phoneConflictUpdate = new UpdateProfileRequest("Author B Updated", emailB, "09666611111", "Univ", "Dept");
        mockMvc.perform(put("/api/v1/auth/profile")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(phoneConflictUpdate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsStringIgnoringCase("phone number")));

        // 3. Author B updates to a new unique phone -> Allowed
        UpdateProfileRequest validUpdate = new UpdateProfileRequest("Author B Renamed", emailB, "+91 96666 33333", "SRMU", "CSE");
        mockMvc.perform(put("/api/v1/auth/profile")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Author B Renamed")))
                .andExpect(jsonPath("$.data.phone", is("+919666633333")));
    }
}

