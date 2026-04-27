package com.flowboard.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int notificationId;

    @Column(nullable = false)
    private int recipientId;

    @Column(nullable = false)
    private int actorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String message;

    private int relatedId;

    private String relatedType;

    @Column(nullable = false)
    private boolean isRead;

    private String deepLinkUrl;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        isRead = false;
    }

    public enum NotificationType {
        ASSIGNMENT,
        MENTION,
        DUE_DATE,
        COMMENT,
        MOVE
    }
}