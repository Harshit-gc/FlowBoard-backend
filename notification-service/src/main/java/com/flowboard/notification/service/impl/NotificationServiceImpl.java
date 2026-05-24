package com.flowboard.notification.service.impl;

import com.flowboard.notification.dto.BulkNotificationRequest;
import com.flowboard.notification.dto.NotificationRequest;
import com.flowboard.notification.dto.NotificationResponse;
import com.flowboard.notification.dto.UnreadCountResponse;
import com.flowboard.notification.entity.Notification;
import com.flowboard.notification.exception.AppException;
import com.flowboard.notification.repository.NotificationRepository;
import com.flowboard.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:noreply@flowboard.com}")
    private String mailFrom;

    @Override
    public NotificationResponse send(NotificationRequest request) {
        // Self-notification check kept for HTTP endpoint only
        // Consumer skips self-notifications before calling this
        Notification notification = Notification.builder()
                .recipientId(request.getRecipientId())
                .actorId(request.getActorId())
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .relatedId(request.getRelatedId())
                .relatedType(request.getRelatedType())
                .deepLinkUrl(request.getDeepLinkUrl())
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification saved: type={} recipient={}",
                request.getType(), request.getRecipientId());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public List<NotificationResponse> sendBulk(BulkNotificationRequest request) {
        if (request.getRecipientIds() == null || request.getRecipientIds().isEmpty()) {
            throw new AppException("Recipient list cannot be empty", HttpStatus.BAD_REQUEST);
        }

        List<Notification> notifications = new ArrayList<>();

        for (Integer recipientId : request.getRecipientIds()) {
            if (recipientId.equals(request.getActorId())) {
                continue;
            }
            Notification notification = Notification.builder()
                    .recipientId(recipientId)
                    .actorId(request.getActorId())
                    .type(request.getType())
                    .title(request.getTitle())
                    .message(request.getMessage())
                    .relatedId(request.getRelatedId())
                    .relatedType(request.getRelatedType())
                    .deepLinkUrl(request.getDeepLinkUrl())
                    .isRead(false)
                    .build();
            notifications.add(notification);
        }

        List<Notification> saved = notificationRepository.saveAll(notifications);
        return saved.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(int notificationId) {
        Notification notification = findById(notificationId);
        if (notification.isRead()) {
            return toResponse(notification);
        }
        notificationRepository.markAsReadById(notificationId);
        notification.setRead(true);
        return toResponse(notification);
    }

    @Override
    @Transactional
    public void markAllRead(int recipientId) {
        notificationRepository.markAllAsReadByRecipientId(recipientId);
    }

    @Override
    @Transactional
    public void deleteRead(int recipientId) {
        notificationRepository.deleteByRecipientIdAndIsReadTrue(recipientId);
    }

    @Override
    public List<NotificationResponse> getByRecipient(int recipientId) {
        return notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(recipientId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UnreadCountResponse getUnreadCount(int recipientId) {
        int count = notificationRepository.countByRecipientIdAndIsRead(recipientId, false);
        return new UnreadCountResponse(recipientId, count);
    }

    @Override
    @Transactional
    public void deleteNotification(int notificationId) {
        Notification notification = findById(notificationId);
        notificationRepository.delete(notification);
    }

    @Override
    public void sendEmail(String toEmail, String subject, String body) {
        if (!mailEnabled) {
            log.info("Mail disabled. Skipping email to {} | Subject: {}", toEmail, subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {} | Subject: {}", toEmail, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    public List<NotificationResponse> getAll() {
        return notificationRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public NotificationResponse getById(int notificationId) {
        return toResponse(findById(notificationId));
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private Notification findById(int notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new AppException(
                        "Notification not found with id: " + notificationId,
                        HttpStatus.NOT_FOUND
                ));
    }

    private boolean isCriticalEvent(Notification.NotificationType type) {
        return type == Notification.NotificationType.ASSIGNMENT ||
                type == Notification.NotificationType.DUE_DATE;
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .recipientId(n.getRecipientId())
                .actorId(n.getActorId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .relatedId(n.getRelatedId())
                .relatedType(n.getRelatedType())
                .isRead(n.isRead())
                .deepLinkUrl(n.getDeepLinkUrl())
                .createdAt(n.getCreatedAt())
                .build();
    }
}