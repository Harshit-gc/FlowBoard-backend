package com.flowboard.collaboration.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent implements Serializable {
    private Integer recipientId;
    private Integer actorId;
    private String type;
    private String title;
    private String message;
    private Integer relatedId;
    private String relatedType;
    private String deepLinkUrl;
}