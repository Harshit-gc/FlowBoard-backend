package com.flowboard.notification.service;

import com.flowboard.notification.dto.*;
import com.flowboard.notification.entity.Notification;
import com.flowboard.notification.entity.Notification.NotificationType;
import com.flowboard.notification.exception.AppException;
import com.flowboard.notification.repository.NotificationRepository;
import com.flowboard.notification.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl Unit Tests")
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private JavaMailSender mailSender;

    @InjectMocks private NotificationServiceImpl notificationService;

    // ── Shared fixtures ───────────────────────────────────────────────────────

    private Notification unreadNotification;
    private Notification readNotification;

    @BeforeEach
    void setUp() {
        // default: mail disabled
        ReflectionTestUtils.setField(notificationService, "mailEnabled", false);
        ReflectionTestUtils.setField(notificationService, "mailFrom", "noreply@flowboard.com");

        unreadNotification = Notification.builder()
                .notificationId(1)
                .recipientId(10)
                .actorId(20)
                .type(NotificationType.ASSIGNMENT)
                .title("You were assigned")
                .message("You have been assigned to: Fix bug")
                .relatedId(5)
                .relatedType("CARD")
                .isRead(false)
                .deepLinkUrl("/board/1")
                .createdAt(LocalDateTime.now())
                .build();

        readNotification = Notification.builder()
                .notificationId(2)
                .recipientId(10)
                .actorId(20)
                .type(NotificationType.COMMENT)
                .title("New comment")
                .message("Someone commented on your card")
                .relatedId(5)
                .relatedType("CARD")
                .isRead(true)
                .deepLinkUrl("/board/1")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // send()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("send()")
    class SendTests {

        @Test
        @DisplayName("should save notification and return response")
        void send_success() {
            NotificationRequest req = NotificationRequest.builder()
                    .recipientId(10)
                    .actorId(20)
                    .type(NotificationType.ASSIGNMENT)
                    .title("You were assigned")
                    .message("You have been assigned to: Fix bug")
                    .relatedId(5)
                    .relatedType("CARD")
                    .deepLinkUrl("/board/1")
                    .build();

            when(notificationRepository.save(any(Notification.class)))
                    .thenReturn(unreadNotification);

            NotificationResponse resp = notificationService.send(req);

            assertThat(resp.getNotificationId()).isEqualTo(1);
            assertThat(resp.getRecipientId()).isEqualTo(10);
            assertThat(resp.getActorId()).isEqualTo(20);
            assertThat(resp.getType()).isEqualTo(NotificationType.ASSIGNMENT);
            assertThat(resp.getTitle()).isEqualTo("You were assigned");
            assertThat(resp.getIsRead()).isFalse();

            ArgumentCaptor<Notification> captor =
                    ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            assertThat(captor.getValue().isRead()).isFalse();
            assertThat(captor.getValue().getRecipientId()).isEqualTo(10);
        }

        @Test
        @DisplayName("should save COMMENT type notification correctly")
        void send_commentType_success() {
            NotificationRequest req = NotificationRequest.builder()
                    .recipientId(10)
                    .actorId(20)
                    .type(NotificationType.COMMENT)
                    .title("New comment")
                    .message("Someone commented")
                    .relatedId(5)
                    .relatedType("CARD")
                    .build();

            when(notificationRepository.save(any())).thenReturn(readNotification);

            NotificationResponse resp = notificationService.send(req);

            assertThat(resp.getType()).isEqualTo(NotificationType.COMMENT);
        }

        @Test
        @DisplayName("should save DUE_DATE type notification correctly")
        void send_dueDateType_success() {
            Notification dueDateNotif = Notification.builder()
                    .notificationId(3).recipientId(10).actorId(20)
                    .type(NotificationType.DUE_DATE)
                    .title("Due date set").message("Due date set on your card")
                    .isRead(false).build();

            NotificationRequest req = NotificationRequest.builder()
                    .recipientId(10).actorId(20)
                    .type(NotificationType.DUE_DATE)
                    .title("Due date set").message("Due date set on your card")
                    .build();

            when(notificationRepository.save(any())).thenReturn(dueDateNotif);

            NotificationResponse resp = notificationService.send(req);

            assertThat(resp.getType()).isEqualTo(NotificationType.DUE_DATE);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // sendBulk()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("sendBulk()")
    class SendBulkTests {

        @Test
        @DisplayName("should send notifications to all recipients except actor")
        void sendBulk_skipsActor() {
            BulkNotificationRequest req = new BulkNotificationRequest();
            req.setRecipientIds(List.of(10, 20, 30)); // 20 is actor
            req.setActorId(20);
            req.setType(NotificationType.MENTION);
            req.setTitle("Board update");
            req.setMessage("Board was updated");

            Notification n1 = Notification.builder().notificationId(1)
                    .recipientId(10).actorId(20).type(NotificationType.MENTION)
                    .title("Board update").message("Board was updated")
                    .isRead(false).build();
            Notification n2 = Notification.builder().notificationId(2)
                    .recipientId(30).actorId(20).type(NotificationType.MENTION)
                    .title("Board update").message("Board was updated")
                    .isRead(false).build();

            when(notificationRepository.saveAll(anyList()))
                    .thenReturn(List.of(n1, n2));

            List<NotificationResponse> result = notificationService.sendBulk(req);

            // Only 2 saved — actor (20) skipped
            assertThat(result).hasSize(2);
            assertThat(result).extracting(NotificationResponse::getRecipientId)
                    .containsExactlyInAnyOrder(10, 30);

            ArgumentCaptor<List<Notification>> captor =
                    ArgumentCaptor.forClass(List.class);
            verify(notificationRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(2);
            assertThat(captor.getValue())
                    .noneMatch(n -> n.getRecipientId() == 20);
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when recipient list is empty")
        void sendBulk_emptyRecipients_throwsBadRequest() {
            BulkNotificationRequest req = new BulkNotificationRequest();
            req.setRecipientIds(List.of());
            req.setActorId(20);
            req.setType(NotificationType.MENTION);
            req.setTitle("Update");
            req.setMessage("Something happened");

            AppException ex = catchThrowableOfType(
                    () -> notificationService.sendBulk(req), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getMessage()).isEqualTo("Recipient list cannot be empty");
            verify(notificationRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when recipient list is null")
        void sendBulk_nullRecipients_throwsBadRequest() {
            BulkNotificationRequest req = new BulkNotificationRequest();
            req.setRecipientIds(null);
            req.setActorId(20);
            req.setType(NotificationType.MENTION);
            req.setTitle("Update");
            req.setMessage("Something happened");

            AppException ex = catchThrowableOfType(
                    () -> notificationService.sendBulk(req), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("should return empty list when all recipients are the actor")
        void sendBulk_allRecipientsAreActor_returnsEmpty() {
            BulkNotificationRequest req = new BulkNotificationRequest();
            req.setRecipientIds(List.of(20, 20)); // all are actor
            req.setActorId(20);
            req.setType(NotificationType.MENTION);
            req.setTitle("Update");
            req.setMessage("Something");

            when(notificationRepository.saveAll(anyList())).thenReturn(List.of());

            List<NotificationResponse> result = notificationService.sendBulk(req);

            assertThat(result).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // markAsRead()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("markAsRead()")
    class MarkAsReadTests {

        @Test
        @DisplayName("should mark unread notification as read")
        void markAsRead_success() {
            when(notificationRepository.findById(1))
                    .thenReturn(Optional.of(unreadNotification));

            NotificationResponse resp = notificationService.markAsRead(1);

            assertThat(resp.getIsRead()).isTrue();
            verify(notificationRepository).markAsReadById(1);
        }

        @Test
        @DisplayName("should return existing response without re-marking when already read")
        void markAsRead_alreadyRead_noOp() {
            when(notificationRepository.findById(2))
                    .thenReturn(Optional.of(readNotification));

            NotificationResponse resp = notificationService.markAsRead(2);

            assertThat(resp.getIsRead()).isTrue();
            // Should NOT call markAsReadById again
            verify(notificationRepository, never()).markAsReadById(2);
        }

        @Test
        @DisplayName("should throw NOT_FOUND for unknown notification")
        void markAsRead_notFound() {
            when(notificationRepository.findById(99)).thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> notificationService.markAsRead(99), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(ex.getMessage()).isEqualTo("Notification not found with id: 99");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // markAllRead()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("markAllRead()")
    class MarkAllReadTests {

        @Test
        @DisplayName("should call repository to mark all notifications as read")
        void markAllRead_success() {
            notificationService.markAllRead(10);

            verify(notificationRepository).markAllAsReadByRecipientId(10);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteRead()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteRead()")
    class DeleteReadTests {

        @Test
        @DisplayName("should delete all read notifications for a recipient")
        void deleteRead_success() {
            notificationService.deleteRead(10);

            verify(notificationRepository)
                    .deleteByRecipientIdAndIsReadTrue(10);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getByRecipient()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getByRecipient()")
    class GetByRecipientTests {

        @Test
        @DisplayName("should return notifications ordered by createdAt desc")
        void getByRecipient_success() {
            when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(10))
                    .thenReturn(List.of(unreadNotification, readNotification));

            List<NotificationResponse> result =
                    notificationService.getByRecipient(10);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getNotificationId()).isEqualTo(1);
            assertThat(result.get(1).getNotificationId()).isEqualTo(2);
        }

        @Test
        @DisplayName("should return empty list when recipient has no notifications")
        void getByRecipient_empty() {
            when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(99))
                    .thenReturn(List.of());

            List<NotificationResponse> result =
                    notificationService.getByRecipient(99);

            assertThat(result).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getUnreadCount()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUnreadCount()")
    class GetUnreadCountTests {

        @Test
        @DisplayName("should return correct unread count for recipient")
        void getUnreadCount_success() {
            when(notificationRepository.countByRecipientIdAndIsRead(10, false))
                    .thenReturn(3);

            UnreadCountResponse resp = notificationService.getUnreadCount(10);

            assertThat(resp.getRecipientId()).isEqualTo(10);
            assertThat(resp.getUnreadCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("should return 0 when all notifications are read")
        void getUnreadCount_zero() {
            when(notificationRepository.countByRecipientIdAndIsRead(10, false))
                    .thenReturn(0);

            UnreadCountResponse resp = notificationService.getUnreadCount(10);

            assertThat(resp.getUnreadCount()).isEqualTo(0);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteNotification()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteNotification()")
    class DeleteNotificationTests {

        @Test
        @DisplayName("should delete notification when found")
        void deleteNotification_success() {
            when(notificationRepository.findById(1))
                    .thenReturn(Optional.of(unreadNotification));

            notificationService.deleteNotification(1);

            verify(notificationRepository).delete(unreadNotification);
        }

        @Test
        @DisplayName("should throw NOT_FOUND when notification does not exist")
        void deleteNotification_notFound() {
            when(notificationRepository.findById(99)).thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> notificationService.deleteNotification(99),
                    AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            verify(notificationRepository, never()).delete(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getAll() / getById()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAll() and getById()")
    class GetAllAndByIdTests {

        @Test
        @DisplayName("getAll() should return all notifications")
        void getAll_success() {
            when(notificationRepository.findAll())
                    .thenReturn(List.of(unreadNotification, readNotification));

            List<NotificationResponse> result = notificationService.getAll();

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("getById() should return notification when found")
        void getById_found() {
            when(notificationRepository.findById(1))
                    .thenReturn(Optional.of(unreadNotification));

            NotificationResponse resp = notificationService.getById(1);

            assertThat(resp.getNotificationId()).isEqualTo(1);
            assertThat(resp.getTitle()).isEqualTo("You were assigned");
            assertThat(resp.getDeepLinkUrl()).isEqualTo("/board/1");
        }

        @Test
        @DisplayName("getById() should throw NOT_FOUND for unknown id")
        void getById_notFound() {
            when(notificationRepository.findById(99)).thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> notificationService.getById(99), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(ex.getMessage())
                    .isEqualTo("Notification not found with id: 99");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // sendEmail()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("sendEmail()")
    class SendEmailTests {

        @Test
        @DisplayName("should skip sending when mail is disabled")
        void sendEmail_mailDisabled_skips() {
            ReflectionTestUtils.setField(notificationService, "mailEnabled", false);

            notificationService.sendEmail(
                    "user@example.com", "Welcome", "Hello!");

            verify(mailSender, never()).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("should send email when mail is enabled")
        void sendEmail_mailEnabled_sends() {
            ReflectionTestUtils.setField(notificationService, "mailEnabled", true);

            notificationService.sendEmail(
                    "user@example.com", "Welcome", "Hello!");

            ArgumentCaptor<SimpleMailMessage> captor =
                    ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());

            SimpleMailMessage sent = captor.getValue();
            assertThat(sent.getTo()).containsExactly("user@example.com");
            assertThat(sent.getSubject()).isEqualTo("Welcome");
            assertThat(sent.getText()).isEqualTo("Hello!");
            assertThat(sent.getFrom()).isEqualTo("noreply@flowboard.com");
        }

        @Test
        @DisplayName("should not throw when mail send fails")
        void sendEmail_mailFails_noException() {
            ReflectionTestUtils.setField(notificationService, "mailEnabled", true);

            doThrow(new RuntimeException("SMTP error"))
                    .when(mailSender).send(any(SimpleMailMessage.class));

            assertThatNoException().isThrownBy(() ->
                    notificationService.sendEmail(
                            "user@example.com", "Subject", "Body"));
        }
    }
}