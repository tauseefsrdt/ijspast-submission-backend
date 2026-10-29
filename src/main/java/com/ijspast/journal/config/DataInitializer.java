package com.ijspast.journal.config;

import com.ijspast.journal.entity.Review;
import com.ijspast.journal.entity.User;
import com.ijspast.journal.entity.UserProfile;
import com.ijspast.journal.entity.UserRole;
import com.ijspast.journal.repository.AccountSetupTokenRepository;
import com.ijspast.journal.repository.ManuscriptRepository;
import com.ijspast.journal.repository.PasswordResetTokenRepository;
import com.ijspast.journal.repository.ReviewRepository;
import com.ijspast.journal.repository.UserProfileRepository;
import com.ijspast.journal.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ManuscriptRepository manuscriptRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AccountSetupTokenRepository accountSetupTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.editor.email:${EDITOR_EMAIL:}}")
    private String editorEmail;

    @Value("${app.editor.password:${EDITOR_PASSWORD:Editor@123}}")
    private String editorPassword;

    @Value("${app.editor.name:${EDITOR_NAME:Prof. (Dr.) Vijay Tiwari}}")
    private String editorName;

    @Value("${app.editor.institution:${EDITOR_INSTITUTION:Shri Ramswaroop Memorial University}}")
    private String editorInstitution;

    public DataInitializer(UserRepository userRepository,
                           UserProfileRepository userProfileRepository,
                           ManuscriptRepository manuscriptRepository,
                           ReviewRepository reviewRepository,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           AccountSetupTokenRepository accountSetupTokenRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.manuscriptRepository = manuscriptRepository;
        this.reviewRepository = reviewRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.accountSetupTokenRepository = accountSetupTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking database initialization status...");

        // Remove any legacy static/demo manuscripts and accounts (disabled to preserve active evaluation workflow)
        // cleanLegacyStaticData();

        // Initialize Primary Editor Account ONLY
        if (editorEmail != null && !editorEmail.isBlank()) {
            seedEditor();
            log.info("Database initialization completed. Only Editor initialized: {}", editorEmail);
        } else {
            log.warn("Database initialization skipped: app.editor.email / EDITOR_EMAIL is not configured.");
        }
    }

    private void cleanLegacyStaticData() {
        // 1. Delete legacy static manuscripts (e.g. IJSPAST-2026-0001, 0002, 0003)
        List<String> staticManuscriptIds = List.of("IJSPAST-2026-0001", "IJSPAST-2026-0002", "IJSPAST-2026-0003");
        for (String mId : staticManuscriptIds) {
            manuscriptRepository.findById(mId).ifPresent(m -> {
                log.info("Purging legacy static manuscript: {}", mId);
                List<Review> reviews = reviewRepository.findByManuscriptOrderBySubmittedAtDesc(m);
                if (!reviews.isEmpty()) {
                    reviewRepository.deleteAll(reviews);
                }
                manuscriptRepository.delete(m);
            });
        }
    }

    private User seedEditor() {
        User editor = userRepository.findByEmailIgnoreCase(editorEmail.trim().toLowerCase())
                .orElseGet(() -> {
                    log.info("Seeding Initial Editor account: {}", editorEmail);
                    User user = new User(
                            "editor_01",
                            editorName,
                            editorEmail.trim().toLowerCase(),
                            passwordEncoder.encode(editorPassword),
                            UserRole.ROLE_EDITOR
                    );
                    user.setInstitution(editorInstitution);
                    user.setDepartment("Office of Editor-in-Chief");
                    user.setEnabled(true);
                    user.setAccountSetupCompleted(true);
                    return userRepository.save(user);
                });

        userProfileRepository.findByUserId(editor.getId()).orElseGet(() -> {
            UserProfile profile = new UserProfile();
            profile.setId("profile_" + editor.getId());
            profile.setUser(editor);
            profile.setInstitution(editor.getInstitution() != null ? editor.getInstitution() : editorInstitution);
            profile.setDepartment(editor.getDepartment() != null ? editor.getDepartment() : "Office of Editor-in-Chief");
            profile.setCityCountry("Lucknow, India");
            profile.setDomainExpertise("Applied Sciences & Journal Management");
            return userProfileRepository.save(profile);
        });

        return editor;
    }
}
