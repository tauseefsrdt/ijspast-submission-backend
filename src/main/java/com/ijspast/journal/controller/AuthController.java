package com.ijspast.journal.controller;

import com.ijspast.journal.dto.request.*;
import com.ijspast.journal.dto.response.ApiResponse;
import com.ijspast.journal.dto.response.AuthResponseDTO;
import com.ijspast.journal.dto.response.UserResponseDTO;
import com.ijspast.journal.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponseDTO response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Authentication successful", response));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                "If the email is registered in our portal, a 6-digit OTP code has been dispatched.", null
        ));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.success(
                "A fresh 6-digit verification code has been dispatched to your email address.", null
        ));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<com.ijspast.journal.dto.response.VerifyOtpResponseDTO>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        com.ijspast.journal.dto.response.VerifyOtpResponseDTO response = authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP verified successfully.", response));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully. You may now log in.", null));
    }

    @PostMapping("/activate-account")
    public ResponseEntity<ApiResponse<Void>> activateAccount(@Valid @RequestBody AccountActivationRequest request) {
        authService.activateAccount(request);
        return ResponseEntity.ok(ApiResponse.success("Account activated successfully. You may now log in.", null));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully.", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getCurrentUser() {
        UserResponseDTO user = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}
