package com.flowboard.notification.messaging;

import com.flowboard.notification.dto.NotificationRequest;
import com.flowboard.notification.entity.Notification.NotificationType;
import com.flowboard.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationConsumer Unit Tests")
class NotificationConsumerTest {

    @Mock private NotificationService notificationService;

    @InjectMocks private NotificationConsumer consumer;

    // ── Shared fixture builder ────────────────────────────────────────────────

    private NotificationEvent buildEvent(Integer recipientId, Integer actorId,
                                         String type) {
        return NotificationEvent.builder()
                .recipientId(recipientId)
                .actorId(actorId)
                .type(type)
                .title("Test title")
                .message("Test message")
                .relatedId(10)
                .relatedType("CARD")
                .deepLinkUrl("/board/1")
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // consume() — happy path
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("consume() — happy path")
    class ConsumeHappyPathTests {

        @Test
        @DisplayName("should call notificationService.send() for a valid event")
        void consume_validEvent_callsService() {
            NotificationEvent event = buildEvent(10, 20, "ASSIGNMENT");

            consumer.consume(event);

            ArgumentCaptor<NotificationRequest> captor =
                    ArgumentCaptor.forClass(NotificationRequest.class);
            verify(notificationService).send(captor.capture());

            NotificationRequest req = captor.getValue();
            assertThat(req.getRecipientId()).isEqualTo(10);
            assertThat(req.getActorId()).isEqualTo(20);
            assertThat(req.getType()).isEqualTo(NotificationType.ASSIGNMENT);
            assertThat(req.getTitle()).isEqualTo("Test title");
            assertThat(req.getMessage()).isEqualTo("Test message");
            assertThat(req.getRelatedId()).isEqualTo(10);
            assertThat(req.getRelatedType()).isEqualTo("CARD");
            assertThat(req.getDeepLinkUrl()).isEqualTo("/board/1");
        }

        @Test
        @DisplayName("should process COMMENT type event correctly")
        void consume_commentEvent_callsService() {
            NotificationEvent event = buildEvent(10, 20, "COMMENT");

            consumer.consume(event);

            ArgumentCaptor<NotificationRequest> captor =
                    ArgumentCaptor.forClass(NotificationRequest.class);
            verify(notificationService).send(captor.capture());
            assertThat(captor.getValue().getType())
                    .isEqualTo(NotificationType.COMMENT);
        }

        @Test
        @DisplayName("should process DUE_DATE type event correctly")
        void consume_dueDateEvent_callsService() {
            NotificationEvent event = buildEvent(10, 20, "DUE_DATE");

            consumer.consume(event);

            ArgumentCaptor<NotificationRequest> captor =
                    ArgumentCaptor.forClass(NotificationRequest.class);
            verify(notificationService).send(captor.capture());
            assertThat(captor.getValue().getType())
                    .isEqualTo(NotificationType.DUE_DATE);
        }

        @Test
        @DisplayName("should process MENTION type event correctly")
        void consume_mentionEvent_callsService() {
            NotificationEvent event = buildEvent(10, 20, "MENTION");

            consumer.consume(event);

            verify(notificationService).send(any());
        }

        @Test
        @DisplayName("should process MOVE type event correctly")
        void consume_moveEvent_callsService() {
            NotificationEvent event = buildEvent(10, 20, "MOVE");

            consumer.consume(event);

            verify(notificationService).send(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // consume() — self-notification skip
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("consume() — self-notification skip")
    class SelfNotificationTests {

        @Test
        @DisplayName("should skip when recipientId equals actorId")
        void consume_selfNotification_skipped() {
            NotificationEvent event = buildEvent(10, 10, "ASSIGNMENT");

            consumer.consume(event);

            verify(notificationService, never()).send(any());
        }

        @Test
        @DisplayName("should process normally when recipientId differs from actorId")
        void consume_differentActor_processed() {
            NotificationEvent event = buildEvent(10, 99, "ASSIGNMENT");

            consumer.consume(event);

            verify(notificationService).send(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // consume() — error resilience
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("consume() — error resilience")
    class ErrorResilienceTests {

        @Test
        @DisplayName("should not throw when notificationService.send() fails")
        void consume_serviceFails_noException() {
            NotificationEvent event = buildEvent(10, 20, "ASSIGNMENT");

            doThrow(new RuntimeException("DB error"))
                    .when(notificationService).send(any());

            assertThatNoException().isThrownBy(() -> consumer.consume(event));
        }

        @Test
        @DisplayName("should not throw when event has invalid type string")
        void consume_invalidType_noException() {
            NotificationEvent event = buildEvent(10, 20, "INVALID_TYPE");

            // IllegalArgumentException from valueOf() should be caught internally
            assertThatNoException().isThrownBy(() -> consumer.consume(event));

            verify(notificationService, never()).send(any());
        }
    }
}