package com.ijspast.journal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ijspast.journal.dto.request.CreateReviewerRequest;
import com.ijspast.journal.dto.request.ResetPasswordRequest;
import com.ijspast.journal.entity.PasswordResetToken;
import com.ijspast.journal.entity.User;
import com.ijspast.journal.entity.UserRole;
import com.ijspast.journal.repository.PasswordResetTokenRepository;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SecurityAndRoleAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository resetTokenRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Value("${app.editor.email}")
    private String editorEmail;

    @Test
    @DisplayName("Unauthenticated request to protected endpoint returns 401")
    void testUnauthenticatedAccessFails() throws Exception {
        mockMvc.perform(get("/api/v1/editor/manuscripts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Author role attempting to access Editor endpoint returns 403 Forbidden")
    void testRoleAccessControl() throws Exception {
        User author = userRepository.findByEmailIgnoreCase("test.author@example.com")
                .orElseGet(() -> {
                    User a = new User("test_author_temp", "Dr. Temp Author", "test.author@example.com", "pass", UserRole.ROLE_AUTHOR);
                    a.setEnabled(true);
                    return userRepository.save(a);
                });
        String authorJwt = jwtTokenProvider.generateToken(author.getEmail(), author.getRole().name(), author.getId());

        mockMvc.perform(get("/api/v1/editor/manuscripts")
                        .header("Authorization", "Bearer " + authorJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Editor creates and invites a new Reviewer")
    void testEditorCreatesReviewer() throws Exception {
        User editor = userRepository.findByEmailIgnoreCase(editorEmail).orElseThrow();
        String editorJwt = jwtTokenProvider.generateToken(editor.getEmail(), editor.getRole().name(), editor.getId());

        String reviewerEmail = "prof.sharma." + System.currentTimeMillis() + "@iitk.ac.in";
        CreateReviewerRequest createReq = new CreateReviewerRequest(
                "Prof. Arvind Sharma",
                reviewerEmail,
                "+91 9988776655",
                "IIT Kanpur",
                "Information Technology and Cloud Computing"
        );

        mockMvc.perform(post("/api/v1/editor/reviewers")
                        .header("Authorization", "Bearer " + editorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reviewer.email").value(reviewerEmail))
                .andExpect(jsonPath("$.data.credentials.password").isNotEmpty());

        // Verify reviewer is in DB with ROLE_REVIEWER
        User createdReviewer = userRepository.findByEmailIgnoreCase(reviewerEmail).orElse(null);
        assertThat(createdReviewer).isNotNull();
        assertThat(createdReviewer.getRole()).isEqualTo(UserRole.ROLE_REVIEWER);
    }

    @Test
    @DisplayName("Expired reset token is rejected with 400 Bad Request")
    void testExpiredResetTokenRejected() throws Exception {
        User user = userRepository.findByEmailIgnoreCase(editorEmail).orElseThrow();
        String rawToken = "expired-token-xyz";

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedHash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : encodedHash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        // Expired in past
        PasswordResetToken expiredToken = new PasswordResetToken(
                UUID.randomUUID().toString(), user, hexString.toString(), LocalDateTime.now().minusMinutes(5)
        );
        resetTokenRepository.save(expiredToken);

        ResetPasswordRequest request = new ResetPasswordRequest(rawToken, "NewPass@1234", "NewPass@1234");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
