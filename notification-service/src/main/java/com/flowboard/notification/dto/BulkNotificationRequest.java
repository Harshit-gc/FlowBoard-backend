package com.flowboard.notification.dto;

import com.flowboard.notification.entity.Notification.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BulkNotificationRequest {

    @NotEmpty(message = "At least one recipient ID is required")
    private List<Integer> recipientIds;

    @NotNull(message = "Actor ID is required")
    private Integer actorId;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    private int relatedId;

    private String relatedType;

    private String deepLinkUrl;
}