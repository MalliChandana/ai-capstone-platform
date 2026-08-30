package com.capstone.aicapstone;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String timestamp;

    private String actorEmail;

    private String actorRole;

    private String action;

    @Column(columnDefinition = "TEXT")
    private String details;

    private String ipAddress;

    public AuditLog(String timestamp, String actorEmail, String actorRole, String action, String details, String ipAddress) {
        this.timestamp = timestamp;
        this.actorEmail = actorEmail;
        this.actorRole = actorRole;
        this.action = action;
        this.details = details;
        this.ipAddress = ipAddress;
    }
}
