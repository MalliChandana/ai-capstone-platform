package com.capstone.aicapstone;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail);
    List<Notification> findByRecipientEmailAndIsReadFalse(String recipientEmail);
    long countByRecipientEmailAndIsReadFalse(String recipientEmail);
}
