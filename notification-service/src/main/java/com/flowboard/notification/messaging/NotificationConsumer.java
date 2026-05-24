package com.flowboard.notification.messaging;

import com.flowboard.notification.dto.NotificationRequest;
import com.flowboard.notification.entity.Notification;
import com.flowboard.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = "${app.rabbitmq.queue}")
    public void consume(NotificationEvent event) {
        log.info("Consumed event: type={} recipient={}",
                event.getType(), event.getRecipientId());
        try {
            // Skip self-notifications silently
            if (event.getRecipientId().equals(event.getActorId())) {
                log.info("Skipping self-notification for userId={}",
                        event.getRecipientId());
                return;
            }

            NotificationRequest request = NotificationRequest.builder()
                    .recipientId(event.getRecipientId())
                    .actorId(event.getActorId())
                    .type(Notification.NotificationType
                            .valueOf(event.getType()))
                    .title(event.getTitle())
                    .message(event.getMessage())
                    .relatedId(event.getRelatedId())
                    .relatedType(event.getRelatedType())
                    .deepLinkUrl(event.getDeepLinkUrl())
                    .build();

            notificationService.send(request);
        } catch (Exception e) {
            log.error("Failed to process notification event: {}",
                    e.getMessage());
        }
    }
}