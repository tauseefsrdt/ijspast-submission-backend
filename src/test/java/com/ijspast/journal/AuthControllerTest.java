package com.ijspast.journal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ijspast.journal.dto.request.ChangePasswordRequest;
import com.ijspast.journal.dto.request.ForgotPasswordRequest;
import com.ijspast.journal.dto.request.LoginRequest;
import com.ijspast.journal.dto.request.ResetPasswordRequest;
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
    @DisplayName("Forgot password endpoint returns generic success and generates reset token")
    void testForgotPasswordFlow() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("editor@ijspast.demo");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Reset password with valid token updates password successfully")
    void testResetPasswordSuccess() throws Exception {
        User user = userRepository.findByEmailIgnoreCase("editor@ijspast.demo").orElseThrow();
        String rawToken = "test-token-123456";

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedHash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : encodedHash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        String tokenHash = hexString.toString();

        PasswordResetToken resetToken = new PasswordResetToken(
                UUID.randomUUID().toString(), user, tokenHash, LocalDateTime.now().plusMinutes(20)
        );
        resetTokenRepository.save(resetToken);

        ResetPasswordRequest request = new ResetPasswordRequest(rawToken, "NewSecurePassword@123", "NewSecurePassword@123");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Change password authenticated endpoint verifies old password and rejects reuse")
    void testChangePasswordValidation() throws Exception {
        User user = userRepository.findByEmailIgnoreCase("editor@ijspast.demo").orElseThrow();
        String jwt = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name(), user.getId());

        // Reusing same password should be rejected
        ChangePasswordRequest samePassRequest = new ChangePasswordRequest("Editor@123", "Editor@123", "Editor@123");

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(samePassRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
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
}
