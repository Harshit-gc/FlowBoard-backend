package com.flowboard.notification.service;

import com.flowboard.notification.dto.BulkNotificationRequest;
import com.flowboard.notification.dto.NotificationRequest;
import com.flowboard.notification.dto.NotificationResponse;
import com.flowboard.notification.dto.UnreadCountResponse;

import java.util.List;

public interface NotificationService {

    NotificationResponse send(NotificationRequest request);

    List<NotificationResponse> sendBulk(BulkNotificationRequest request);

    NotificationResponse markAsRead(int notificationId);

    void markAllRead(int recipientId);

    void deleteRead(int recipientId);

    List<NotificationResponse> getByRecipient(int recipientId);

    UnreadCountResponse getUnreadCount(int recipientId);

    void deleteNotification(int notificationId);

    void sendEmail(String toEmail, String subject, String body);

    List<NotificationResponse> getAll();

    NotificationResponse getById(int notificationId);
}