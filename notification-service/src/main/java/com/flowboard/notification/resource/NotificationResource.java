package com.flowboard.notification.resource;

import com.flowboard.notification.config.JwtConfig;
import com.flowboard.notification.dto.BulkNotificationRequest;
import com.flowboard.notification.dto.NotificationRequest;
import com.flowboard.notification.dto.NotificationResponse;
import com.flowboard.notification.dto.UnreadCountResponse;
import com.flowboard.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "Notification management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class NotificationResource {

    private final NotificationService notificationService;
    private final JwtConfig jwtConfig;

    // POST /api/v1/notifications
    @PostMapping
    @Operation(summary = "Send a single notification")
    public ResponseEntity<NotificationResponse> send(
            @RequestHeader("Authorization") String bearer,
            @Valid @RequestBody NotificationRequest request) {
        if (request.getRecipientId().equals(request.getActorId())) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.send(request));
    }

    // POST /api/v1/notifications/bulk
    @PostMapping("/bulk")
    @Operation(summary = "Send bulk notifications to multiple recipients")
    public ResponseEntity<List<NotificationResponse>> sendBulk(
            @RequestHeader("Authorization") String bearer,
            @Valid @RequestBody BulkNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.sendBulk(request));
    }

    // GET /api/v1/notifications/{notificationId}
    @GetMapping("/{notificationId}")
    @Operation(summary = "Get a notification by ID")
    public ResponseEntity<NotificationResponse> getById(
            @RequestHeader("Authorization") String bearer,
            @PathVariable int notificationId) {
        return ResponseEntity.ok(notificationService.getById(notificationId));
    }

    // GET /api/v1/notifications/recipient/{userId}
    @GetMapping("/recipient/{userId}")
    @Operation(summary = "Get all notifications for a recipient")
    public ResponseEntity<List<NotificationResponse>> getByRecipient(
            @RequestHeader("Authorization") String bearer,
            @PathVariable int userId) {
        return ResponseEntity.ok(notificationService.getByRecipient(userId));
    }

    // GET /api/v1/notifications/unread-count/{userId}
    @GetMapping("/unread-count/{userId}")
    @Operation(summary = "Get unread notification count for badge display")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @RequestHeader("Authorization") String bearer,
            @PathVariable int userId) {
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    // PUT /api/v1/notifications/{notificationId}/read
    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Mark a single notification as read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @RequestHeader("Authorization") String bearer,
            @PathVariable int notificationId) {
        return ResponseEntity.ok(notificationService.markAsRead(notificationId));
    }

    // PUT /api/v1/notifications/recipient/{userId}/read-all
    @PutMapping("/recipient/{userId}/read-all")
    @Operation(summary = "Mark all notifications as read for a recipient")
    public ResponseEntity<String> markAllRead(
            @RequestHeader("Authorization") String bearer,
            @PathVariable int userId) {
        notificationService.markAllRead(userId);
        return ResponseEntity.ok("All notifications marked as read for userId: " + userId);
    }

    // DELETE /api/v1/notifications/{notificationId}
    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Delete a single notification")
    public ResponseEntity<String> deleteNotification(
            @RequestHeader("Authorization") String bearer,
            @PathVariable int notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok("Notification deleted successfully");
    }

    // DELETE /api/v1/notifications/read/{userId}
    @DeleteMapping("/read/{userId}")
    @Operation(summary = "Delete all read notifications for a recipient")
    public ResponseEntity<String> deleteRead(
            @RequestHeader("Authorization") String bearer,
            @PathVariable int userId) {
        notificationService.deleteRead(userId);
        return ResponseEntity.ok("All read notifications deleted for userId: " + userId);
    }

    // GET /api/v1/notifications
    @GetMapping
    @Operation(summary = "Admin: Get all notifications across the platform")
    public ResponseEntity<List<NotificationResponse>> getAll(
            @RequestHeader("Authorization") String bearer) {
        return ResponseEntity.ok(notificationService.getAll());
    }

    private Integer getUserId(String bearer) {
        return jwtConfig.getUserIdFromToken(bearer.substring(7));
    }
}