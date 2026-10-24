package com.ijspast.journal.service;

import com.ijspast.journal.dto.request.*;
import com.ijspast.journal.dto.response.AuthResponseDTO;
import com.ijspast.journal.dto.response.UserResponseDTO;
import com.ijspast.journal.entity.AccountSetupToken;
import com.ijspast.journal.entity.PasswordResetToken;
import com.ijspast.journal.entity.User;
import com.ijspast.journal.exception.BadRequestException;
import com.ijspast.journal.exception.InvalidTokenException;
import com.ijspast.journal.exception.ResourceNotFoundException;
import com.ijspast.journal.exception.UnauthorizedException;
import com.ijspast.journal.mapper.UserMapper;
import com.ijspast.journal.repository.AccountSetupTokenRepository;
import com.ijspast.journal.repository.PasswordResetTokenRepository;
import com.ijspast.journal.repository.UserRepository;
import com.ijspast.journal.security.JwtTokenProvider;
import com.ijspast.journal.security.RateLimiterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final AccountSetupTokenRepository setupTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final UserMapper userMapper;
    private final RateLimiterService rateLimiterService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordResetTokenRepository resetTokenRepository,
                           AccountSetupTokenRepository setupTokenRepository,
                           AuthenticationManager authenticationManager,
                           JwtTokenProvider tokenProvider,
                           PasswordEncoder passwordEncoder,
                           EmailService emailService,
                           UserMapper userMapper,
                           RateLimiterService rateLimiterService) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.setupTokenRepository = setupTokenRepository;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.userMapper = userMapper;
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    @Transactional
    public AuthResponseDTO login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (!rateLimiterService.isAllowed("login:" + email)) {
            throw new BadRequestException("Too many login attempts. Please wait a moment before trying again.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        String jwt = tokenProvider.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        UserResponseDTO userResponse = userMapper.toUserResponseDTO(user);

        return new AuthResponseDTO(jwt, userResponse);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (!rateLimiterService.isAllowed("forgot-password:" + email)) {
            throw new BadRequestException("Too many password reset requests. Please wait before retrying.");
        }

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String rawToken = generateSecureToken();
            String tokenHash = hashToken(rawToken);

            LocalDateTime expiry = LocalDateTime.now().plusMinutes(30);
            PasswordResetToken resetToken = new PasswordResetToken(UUID.randomUUID().toString(), user, tokenHash, expiry);
            resetTokenRepository.save(resetToken);

            emailService.sendPasswordResetEmail(user.getEmail(), rawToken);
        } else {
            log.info("Password reset requested for non-existent email: {}", email);
        }
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match.");
        }

        String tokenHash = hashToken(request.getToken().trim());
        PasswordResetToken token = resetTokenRepository.findByTokenHashAndUsedFalse(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid or already used password reset token."));

        if (token.isExpired()) {
            throw new InvalidTokenException("Password reset token has expired. Please request a new link.");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        token.setUsed(true);
        resetTokenRepository.save(token);
    }

    @Override
    @Transactional
    public void activateAccount(AccountActivationRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Password and confirm password do not match.");
        }

        String tokenHash = hashToken(request.getToken().trim());
        AccountSetupToken token = setupTokenRepository.findByTokenHashAndUsedFalse(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid or already used account activation token."));

        if (token.isExpired()) {
            throw new InvalidTokenException("Activation token has expired. Please contact the editorial office.");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setAccountSetupCompleted(true);
        user.setEnabled(true);
        userRepository.save(user);

        token.setUsed(true);
        setupTokenRepository.save(token);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Authentication is required to change password.");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found in database."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect.");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password cannot be identical to the current password.");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException("No authenticated user in session.");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return userMapper.toUserResponseDTO(user);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
