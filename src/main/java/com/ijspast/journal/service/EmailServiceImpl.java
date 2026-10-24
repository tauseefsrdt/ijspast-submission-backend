package com.ijspast.journal.service;

import com.ijspast.journal.dto.response.DispatchedEmailDTO;
import com.ijspast.journal.entity.EmailNotificationLog;
import com.ijspast.journal.repository.EmailNotificationLogRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final EmailNotificationLogRepository emailLogRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@srdt.co.in}")
    private String smtpUsername;

    @Value("${app.mail.from:${spring.mail.username:no-reply@srdt.co.in}}")
    private String mailFrom;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    public EmailServiceImpl(EmailNotificationLogRepository emailLogRepository,
                            @Autowired(required = false) JavaMailSender mailSender) {
        this.emailLogRepository = emailLogRepository;
        this.mailSender = mailSender;
    }

    private void trySendMimeMessage(String to, String subject, String content) {
        if (mailSender == null || smtpUsername == null || smtpUsername.isBlank()) {
            log.info("SMTP credentials not configured. Email logged to Outbox for testing: To={}, Subject={}", to, subject);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            String sender = (mailFrom != null && !mailFrom.isBlank()) ? mailFrom : smtpUsername;
            helper.setFrom(sender);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, false);
            mailSender.send(message);
            log.info("Email sent successfully via SMTP to {}", to);
        } catch (Exception e) {
            log.warn("Failed to transmit email over SMTP to {}: {}. Notice logged in outbox.", to, e.getMessage());
        }
    }

    private DispatchedEmailDTO recordAndDispatch(String to, String toName, String subject, String type,
                                                 String manuscriptId, String content) {
        String id = "mail_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String sentAt = LocalDateTime.now().format(DATE_FORMATTER);

        EmailNotificationLog logEntry = new EmailNotificationLog(
                id, to, toName, subject, type, manuscriptId, content, sentAt, "Delivered"
        );
        emailLogRepository.save(logEntry);

        // Attempt SMTP dispatch
        trySendMimeMessage(to, subject, content);

        return new DispatchedEmailDTO(id, to, toName, subject, type, manuscriptId, content, sentAt, "Delivered");
    }

    @Override
    @Transactional
    public DispatchedEmailDTO sendAuthorCredentialsEmail(String authorEmail, String authorName, String password,
                                                         String manuscriptId, String manuscriptTitle) {
        String subject = "[IJSPAST] Manuscript Received & Author Account Credentials [" + manuscriptId + "]";
        String content = "Dear " + authorName + ",\n\n"
                + "Thank you for submitting your manuscript to the International Journal of Scientific Progress in Applied Science and Technology (IJSPAST).\n\n"
                + "Manuscript Details:\n"
                + "- Reference ID: " + manuscriptId + "\n"
                + "- Title: \"" + manuscriptTitle + "\"\n"
                + "- Status: Submitted (Awaiting Editorial Assessment)\n\n"
                + "A secure author account has been generated for you to track peer-review progress and upload revisions:\n"
                + "- Portal URL: " + frontendUrl + "/login\n"
                + "- Username / Email: " + authorEmail + "\n"
                + "- Temporary Password: " + password + "\n\n"
                + "Please retain these credentials. You may log in at any time to monitor your submission's lifecycle.\n\n"
                + "Sincerely,\n"
                + "Editorial Secretariat\n"
                + "IJSPAST - Shri Ramswaroop Memorial University";

        return recordAndDispatch(authorEmail, authorName, subject, "AUTHOR_CREDENTIALS", manuscriptId, content);
    }

    @Override
    @Transactional
    public DispatchedEmailDTO sendReviewerInvitationEmail(String reviewerEmail, String reviewerName, String password,
                                                          String manuscriptId, String manuscriptTitle, int deadlineDays) {
        String subject = "[IJSPAST] Peer Review Invitation & Portal Credentials [" + manuscriptId + "]";
        String content = "Dear " + reviewerName + ",\n\n"
                + "You have been appointed as an expert peer reviewer for manuscript " + manuscriptId + ":\n"
                + "Title: \"" + manuscriptTitle + "\"\n"
                + "Review Deadline: " + deadlineDays + " days\n\n"
                + "Login credentials to access the double-blind review scorecard:\n"
                + "- Portal URL: " + frontendUrl + "/login\n"
                + "- Username / Email: " + reviewerEmail + "\n"
                + "- Password: " + password + "\n\n"
                + "Double-Blind Protocol: Author identity has been masked from the manuscript file. All feedback must be submitted through the portal.\n\n"
                + "Editorial Office, IJSPAST";

        return recordAndDispatch(reviewerEmail, reviewerName, subject, "REVIEWER_INVITATION", manuscriptId, content);
    }

    @Override
    @Transactional
    public DispatchedEmailDTO sendReviewCompletedEmail(String editorEmail, String reviewerName,
                                                       String manuscriptId, String manuscriptTitle, String recommendation) {
        String targetEmail = (editorEmail != null && !editorEmail.isBlank()) ? editorEmail : "editor@ijspast.demo";
        String subject = "[IJSPAST] Review Completed by " + reviewerName + " for [" + manuscriptId + "]";
        String content = "Dear Editor-in-Chief,\n\n"
                + "Peer reviewer " + reviewerName + " has completed their evaluation for manuscript " + manuscriptId + ":\n"
                + "Title: \"" + manuscriptTitle + "\"\n"
                + "Recommendation: " + recommendation + "\n\n"
                + "You can now examine the 11-point scorecard, confidential editor notes, and forward anonymous comments to the author.\n\n"
                + "Portal URL: " + frontendUrl + "/editor/manuscript/" + manuscriptId + "\n\n"
                + "IJSPAST Automated Peer Review System";

        return recordAndDispatch(targetEmail, "Prof. (Dr.) Vijay Tiwari (Editor-in-Chief)", subject, "REVIEW_COMPLETED", manuscriptId, content);
    }

    @Override
    @Transactional
    public DispatchedEmailDTO sendRevisionRequestEmail(String authorEmail, String authorName,
                                                       String manuscriptId, String manuscriptTitle, String comments) {
        String subject = "[IJSPAST] Revision Requested for Manuscript [" + manuscriptId + "]";
        String content = "Dear " + authorName + ",\n\n"
                + "Following peer review of your submission " + manuscriptId + " (\"" + manuscriptTitle + "\"), the Editorial Board has determined that revisions are required before publication can be considered.\n\n"
                + "Reviewer Comments (Relayed Anonymously):\n"
                + "--------------------------------------------------\n"
                + comments + "\n"
                + "--------------------------------------------------\n\n"
                + "Please log in to the Author Center to upload your revised manuscript document and point-by-point response:\n"
                + "Portal URL: " + frontendUrl + "/login\n\n"
                + "Sincerely,\n"
                + "Prof. (Dr.) Vijay Tiwari\n"
                + "Editor-in-Chief, IJSPAST";

        return recordAndDispatch(authorEmail, authorName, subject, "REVISION_REQUEST", manuscriptId, content);
    }

    @Override
    @Transactional
    public DispatchedEmailDTO sendRevisionSubmittedEmail(String recipientEmail, String recipientName, String role,
                                                         String manuscriptId, String manuscriptTitle, String response) {
        String subject = "[IJSPAST] Revised Manuscript Submitted for [" + manuscriptId + "] (Round 2)";
        String content = "Dear " + recipientName + ",\n\n"
                + "The corresponding author has submitted a revised manuscript and response for " + manuscriptId + ":\n"
                + "Title: \"" + manuscriptTitle + "\"\n\n"
                + "Author Response Summary:\n"
                + "\"" + response + "\"\n\n"
                + "Please access the portal to inspect the revised document and perform re-evaluation:\n"
                + "Portal URL: " + frontendUrl + "/login\n\n"
                + "IJSPAST Editorial Secretariat";

        return recordAndDispatch(recipientEmail, recipientName, subject, "REVISION_SUBMITTED", manuscriptId, content);
    }

    @Override
    @Transactional
    public DispatchedEmailDTO sendPublicationEmail(String authorEmail, String authorName,
                                                   String manuscriptId, String manuscriptTitle,
                                                   String doi, String volume, String issue) {
        String subject = "[IJSPAST] Official Publication Online: [" + manuscriptId + "]";
        String content = "Dear " + authorName + ",\n\n"
                + "We are pleased to announce that your article \"" + manuscriptTitle + "\" is now officially published online in the International Journal of Scientific Progress in Applied Science and Technology (IJSPAST).\n\n"
                + "Publication Details:\n"
                + "- Citation: IJSPAST, " + volume + ", " + issue + "\n"
                + "- Permanent DOI: https://doi.org/" + doi + "\n"
                + "- Public Repository: " + frontendUrl + "/published\n\n"
                + "Congratulations on your publication!\n\n"
                + "Editorial Board\n"
                + "Shri Ramswaroop Memorial University";

        return recordAndDispatch(authorEmail, authorName, subject, "PUBLICATION_CONFIRMATION", manuscriptId, content);
    }

    @Override
    @Transactional
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String subject = "[IJSPAST] Password Reset Request";
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        String content = "Dear Colleague,\n\n"
                + "A password reset request was received for your IJSPAST account.\n\n"
                + "Click the link below to establish a new password (valid for 30 minutes):\n"
                + resetLink + "\n\n"
                + "If you did not request this, please disregard this email. Your password will remain unchanged.\n\n"
                + "IJSPAST Security Office";

        recordAndDispatch(toEmail, toEmail, subject, "PASSWORD_RESET", null, content);
    }

    @Override
    @Transactional
    public void sendAccountActivationEmail(String toEmail, String activationToken) {
        String subject = "[IJSPAST] Account Activation";
        String activateLink = frontendUrl + "/activate-account?token=" + activationToken;
        String content = "Dear Colleague,\n\n"
                + "An account has been created for you on the IJSPAST portal.\n\n"
                + "Click the link below to activate your account and establish your password:\n"
                + activateLink + "\n\n"
                + "Editorial Office, IJSPAST";

        recordAndDispatch(toEmail, toEmail, subject, "ACCOUNT_ACTIVATION", null, content);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DispatchedEmailDTO> getOutboxEmails(String type, String search) {
        List<EmailNotificationLog> logs;
        if (type != null && !type.isBlank()) {
            logs = emailLogRepository.findByEmailTypeOrderByCreatedAtDesc(type);
        } else {
            logs = emailLogRepository.findAllByOrderByCreatedAtDesc();
        }

        return logs.stream()
                .filter(log -> {
                    if (search == null || search.isBlank()) return true;
                    String s = search.toLowerCase();
                    return log.getRecipientEmail().toLowerCase().contains(s)
                            || log.getRecipientName().toLowerCase().contains(s)
                            || log.getSubject().toLowerCase().contains(s)
                            || (log.getManuscriptId() != null && log.getManuscriptId().toLowerCase().contains(s));
                })
                .map(l -> new DispatchedEmailDTO(
                        l.getId(), l.getRecipientEmail(), l.getRecipientName(),
                        l.getSubject(), l.getEmailType(), l.getManuscriptId(),
                        l.getContent(), l.getSentAt(), l.getStatus()
                ))
                .collect(Collectors.toList());
    }
}
