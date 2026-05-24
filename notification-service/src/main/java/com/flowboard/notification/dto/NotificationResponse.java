package com.flowboard.notification.dto;

import com.flowboard.notification.entity.Notification.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {

    private int notificationId;
    private int recipientId;
    private int actorId;
    private NotificationType type;
    private String title;
    private String message;
    private int relatedId;
    private String relatedType;
    private Boolean isRead;
    private String deepLinkUrl;
    private LocalDateTime createdAt;
}