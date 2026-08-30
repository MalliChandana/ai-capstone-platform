package com.capstone.aicapstone;

import jakarta.persistence.*;

@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String recipientEmail;
    private String recipientRole; // "STUDENT" or "COMPANY"
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    private String createdAt;
    private boolean isRead;
    private String link;

    public Notification() {
    }

    public Notification(String recipientEmail, String recipientRole, String title, String message, String createdAt, String link) {
        this.recipientEmail = recipientEmail;
        this.recipientRole = recipientRole;
        this.title = title;
        this.message = message;
        this.createdAt = createdAt;
        this.link = link;
        this.isRead = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getRecipientRole() {
        return recipientRole;
    }

    public void setRecipientRole(String recipientRole) {
        this.recipientRole = recipientRole;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
