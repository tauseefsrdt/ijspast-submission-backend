package com.ijspast.journal.service;

import com.ijspast.journal.dto.request.*;
import com.ijspast.journal.dto.response.AuthResponseDTO;
import com.ijspast.journal.dto.response.UserResponseDTO;

public interface AuthService {

    AuthResponseDTO login(LoginRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resendOtp(ResendOtpRequest request);

    com.ijspast.journal.dto.response.VerifyOtpResponseDTO verifyOtp(VerifyOtpRequest request);

    void resetPassword(ResetPasswordRequest request);

    void activateAccount(AccountActivationRequest request);

    void changePassword(ChangePasswordRequest request);

    UserResponseDTO getCurrentUser();
}
