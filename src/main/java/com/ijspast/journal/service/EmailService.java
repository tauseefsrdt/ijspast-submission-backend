package com.ijspast.journal.service;

import com.ijspast.journal.dto.response.DispatchedEmailDTO;

import java.util.List;

public interface EmailService {

    DispatchedEmailDTO sendAuthorCredentialsEmail(String authorEmail, String authorName, String password,
                                                  String manuscriptId, String manuscriptTitle);

    DispatchedEmailDTO sendReviewerAccountCreationEmail(String reviewerEmail, String reviewerName, String password);

    DispatchedEmailDTO sendReviewerInvitationEmail(String reviewerEmail, String reviewerName, String password,
                                                    String manuscriptId, String manuscriptTitle, int deadlineDays);

    DispatchedEmailDTO sendReviewerAssignmentEmail(String reviewerEmail, String reviewerName,
                                                  String manuscriptId, String manuscriptTitle, int deadlineDays, String notes);

    DispatchedEmailDTO sendRevisionForwardedToReviewerEmail(String reviewerEmail, String reviewerName,
                                                           String manuscriptId, String manuscriptTitle, int deadlineDays, String editorRemarks);

    DispatchedEmailDTO sendReviewCompletedEmail(String editorEmail, String reviewerName,
                                                String manuscriptId, String manuscriptTitle, String recommendation);

    DispatchedEmailDTO sendRevisionRequestEmail(String authorEmail, String authorName,
                                                String manuscriptId, String manuscriptTitle, String comments);

    DispatchedEmailDTO sendRevisionSubmittedEmail(String recipientEmail, String recipientName, String role,
                                                  String manuscriptId, String manuscriptTitle, String response);

    DispatchedEmailDTO sendPublicationEmail(String authorEmail, String authorName,
                                            String manuscriptId, String manuscriptTitle,
                                            String doi, String volume, String issue);

    void sendPasswordResetEmail(String toEmail, String resetToken);

    void sendPasswordResetOtpEmail(String toEmail, String otpCode);

    void sendAccountActivationEmail(String toEmail, String activationToken);

    List<DispatchedEmailDTO> getOutboxEmails(String type, String search);
}
