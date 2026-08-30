package com.capstone.aicapstone;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void notifyStudent(String studentEmail, String title, String message, String link) {
        if (studentEmail == null || studentEmail.trim().isEmpty()) return;
        String now = LocalDateTime.now().format(FORMATTER);
        Notification notification = new Notification(studentEmail, "STUDENT", title, message, now, link);
        notificationRepository.save(notification);
    }

    public void notifyCompany(String companyEmail, String title, String message, String link) {
        if (companyEmail == null || companyEmail.trim().isEmpty()) return;
        String now = LocalDateTime.now().format(FORMATTER);
        Notification notification = new Notification(companyEmail, "COMPANY", title, message, now, link);
        notificationRepository.save(notification);
    }

    public List<Notification> getNotificationsForUser(String email) {
        if (email == null) return Collections.emptyList();
        return notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(email);
    }

    public long getUnreadCount(String email) {
        if (email == null) return 0;
        return notificationRepository.countByRecipientEmailAndIsReadFalse(email);
    }

    public void markAllAsRead(String email) {
        if (email == null) return;
        List<Notification> unread = notificationRepository.findByRecipientEmailAndIsReadFalse(email);
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }
}
