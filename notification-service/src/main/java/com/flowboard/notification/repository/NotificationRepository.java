package com.flowboard.notification.repository;

import com.flowboard.notification.entity.Notification;
import com.flowboard.notification.entity.Notification.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(int recipientId);

    List<Notification> findByRecipientIdAndIsReadOrderByCreatedAtDesc(int recipientId, boolean isRead);

    int countByRecipientIdAndIsRead(int recipientId, boolean isRead);

    List<Notification> findByTypeOrderByCreatedAtDesc(NotificationType type);

    List<Notification> findByRelatedIdAndRelatedType(int relatedId, String relatedType);

    void deleteByNotificationId(int notificationId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.recipientId = :recipientId AND n.isRead = true")
    void deleteByRecipientIdAndIsReadTrue(@Param("recipientId") int recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipientId = :recipientId AND n.isRead = false")
    void markAllAsReadByRecipientId(@Param("recipientId") int recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.notificationId = :notificationId")
    void markAsReadById(@Param("notificationId") int notificationId);
}