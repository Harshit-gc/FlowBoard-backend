package com.flowboard.auth.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    private Integer recipientId;
    private Integer actorId;
    private String type;
    private String title;
    private String message;
    private int relatedId;
    private String relatedType;
    private String deepLinkUrl;
}