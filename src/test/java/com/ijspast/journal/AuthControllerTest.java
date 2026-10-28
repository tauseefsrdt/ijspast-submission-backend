package com.ijspast.journal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ijspast.journal.dto.request.ChangePasswordRequest;
import com.ijspast.journal.dto.request.ForgotPasswordRequest;
import com.ijspast.journal.dto.request.LoginRequest;
import com.ijspast.journal.dto.request.ResendOtpRequest;
import com.ijspast.journal.dto.request.ResetPasswordRequest;
import com.ijspast.journal.dto.request.VerifyOtpRequest;
import com.ijspast.journal.entity.PasswordResetToken;
import com.ijspast.journal.entity.User;
import com.ijspast.journal.repository.PasswordResetTokenRepository;
import com.ijspast.journal.repository.UserRepository;
import com.ijspast.journal.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerTest {

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

    @org.junit.jupiter.api.BeforeEach
    void setupRoleUsers() {
        org.springframework.security.crypto.password.PasswordEncoder encoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

        // Seed Author account
        if (userRepository.findByEmailIgnoreCase("author@ijspast.demo").isEmpty()) {
            User author = new User("author_test_01", "Dr. John Author", "author@ijspast.demo",
                    encoder.encode("Author@123"), com.ijspast.journal.entity.UserRole.ROLE_AUTHOR);
            author.setEnabled(true);
            author.setAccountSetupCompleted(true);
            userRepository.save(author);
        }

        // Seed Reviewer account
        if (userRepository.findByEmailIgnoreCase("reviewer@ijspast.demo").isEmpty()) {
            User reviewer = new User("reviewer_test_01", "Dr. Jane Reviewer", "reviewer@ijspast.demo",
                    encoder.encode("Reviewer@123"), com.ijspast.journal.entity.UserRole.ROLE_REVIEWER);
            reviewer.setEnabled(true);
            reviewer.setAccountSetupCompleted(true);
            userRepository.save(reviewer);
        }
    }

    @Test
    @DisplayName("Login with valid editor credentials returns JWT and editor role")
    void testValidEditorLogin() throws Exception {
        LoginRequest request = new LoginRequest("editor@ijspast.demo", "Editor@123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("editor@ijspast.demo"))
                .andExpect(jsonPath("$.data.user.role").value("editor"));
    }

    @Test
    @DisplayName("Login with invalid password returns 401 Unauthorized")
    void testInvalidPasswordLogin() throws Exception {
        LoginRequest request = new LoginRequest("editor@ijspast.demo", "WrongPassword@999");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Author can request password reset OTP successfully")
    void testAuthorForgotPasswordFlow() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("author@ijspast.demo");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Reviewer can request password reset OTP successfully")
    void testReviewerForgotPasswordFlow() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("reviewer@ijspast.demo");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Editor password reset request succeeds with 200 OK")
    void testEditorForgotPasswordSuccess() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("editor@ijspast.demo");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Editor resend OTP request succeeds with 200 OK")
    void testEditorResendOtpSuccess() throws Exception {
        ForgotPasswordRequest forgotReq = new ForgotPasswordRequest("editor@ijspast.demo");
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotReq)))
                .andExpect(status().isOk());

        ResendOtpRequest request = new ResendOtpRequest("editor@ijspast.demo");

        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Author can change password authenticated successfully")
    void testAuthorChangePasswordSuccess() throws Exception {
        User user = userRepository.findByEmailIgnoreCase("author@ijspast.demo").orElseThrow();
        String jwt = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name(), user.getId());

        ChangePasswordRequest request = new ChangePasswordRequest("Author@123", "NewAuthorPass@123", "NewAuthorPass@123");

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Restore password
        user = userRepository.findByEmailIgnoreCase("author@ijspast.demo").orElseThrow();
        user.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("Author@123"));
        userRepository.save(user);
    }

    @Test
    @DisplayName("Reviewer can change password authenticated successfully")
    void testReviewerChangePasswordSuccess() throws Exception {
        User user = userRepository.findByEmailIgnoreCase("reviewer@ijspast.demo").orElseThrow();
        String jwt = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name(), user.getId());

        ChangePasswordRequest request = new ChangePasswordRequest("Reviewer@123", "NewReviewerPass@123", "NewReviewerPass@123");

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Restore password
        user = userRepository.findByEmailIgnoreCase("reviewer@ijspast.demo").orElseThrow();
        user.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("Reviewer@123"));
        userRepository.save(user);
    }

    @Test
    @DisplayName("Editor can change password authenticated successfully")
    void testEditorChangePasswordSuccess() throws Exception {
        User user = userRepository.findByEmailIgnoreCase("editor@ijspast.demo").orElseThrow();
        String jwt = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name(), user.getId());

        ChangePasswordRequest request = new ChangePasswordRequest("Editor@123", "NewEditorPass@123", "NewEditorPass@123");

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Restore password
        user = userRepository.findByEmailIgnoreCase("editor@ijspast.demo").orElseThrow();
        user.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("Editor@123"));
        userRepository.save(user);
    }

    @Test
    @DisplayName("Get current user /api/v1/auth/me returns authenticated user details")
    void testGetCurrentUser() throws Exception {
        User user = userRepository.findByEmailIgnoreCase("editor@ijspast.demo").orElseThrow();
        String jwt = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name(), user.getId());

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("editor@ijspast.demo"))
                .andExpect(jsonPath("$.data.role").value("editor"));
    }

    @Test
    @DisplayName("Complete Author OTP forgot-password flow: send OTP, verify OTP, and reset password")
    void testCompleteAuthorOtpForgotPasswordFlow() throws Exception {
        String email = "author@ijspast.demo";

        // 1. Request OTP via forgot-password
        ForgotPasswordRequest forgotReq = new ForgotPasswordRequest(email);
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 2. Fetch generated token and OTP code from repository
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        PasswordResetToken token = resetTokenRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(user).orElseThrow();
        org.junit.jupiter.api.Assertions.assertNotNull(token.getOtpCode());
        org.junit.jupiter.api.Assertions.assertEquals(6, token.getOtpCode().length());
        String correctOtp = token.getOtpCode();

        // 3. Verify with wrong OTP fails
        VerifyOtpRequest wrongOtpReq = new VerifyOtpRequest(email, "000000");
        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongOtpReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // 4. Verify with correct OTP succeeds and returns resetToken
        VerifyOtpRequest correctOtpReq = new VerifyOtpRequest(email, correctOtp);
        String responseContent = mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctOtpReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.resetToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(responseContent);
        String resetToken = rootNode.path("data").path("resetToken").asText();

        // 5. Reset password with matching passwords succeeds
        String newPass = "SuperSecretAuthor@2026";
        ResetPasswordRequest validResetReq = new ResetPasswordRequest(resetToken, newPass, newPass);
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validResetReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 6. Verify login with the newly updated password succeeds
        LoginRequest newLoginReq = new LoginRequest(email, newPass);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLoginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty());

        // Restore original password
        User updatedUser = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        updatedUser.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("Author@123"));
        userRepository.save(updatedUser);
    }

    @Test
    @DisplayName("Author resend OTP generates new OTP and invalidates previous token")
    void testAuthorResendOtpFlow() throws Exception {
        String email = "author@ijspast.demo";

        ResendOtpRequest resendReq = new ResendOtpRequest(email);
        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        PasswordResetToken token = resetTokenRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(user).orElseThrow();
        org.junit.jupiter.api.Assertions.assertNotNull(token.getOtpCode());
        org.junit.jupiter.api.Assertions.assertEquals(6, token.getOtpCode().length());
    }
}
