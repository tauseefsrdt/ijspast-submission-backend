package com.ijspast.journal.repository;

import com.ijspast.journal.entity.EmailNotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailNotificationLogRepository extends JpaRepository<EmailNotificationLog, String> {

    List<EmailNotificationLog> findAllByOrderByCreatedAtDesc();

    List<EmailNotificationLog> findByRecipientEmailIgnoreCaseOrderByCreatedAtDesc(String recipientEmail);

    List<EmailNotificationLog> findByEmailTypeOrderByCreatedAtDesc(String emailType);

    List<EmailNotificationLog> findByManuscriptIdOrderByCreatedAtDesc(String manuscriptId);
}
