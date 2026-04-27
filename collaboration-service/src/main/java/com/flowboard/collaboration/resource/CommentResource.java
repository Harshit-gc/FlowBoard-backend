package com.flowboard.collaboration.resource;

import com.flowboard.collaboration.config.JwtConfig;
import com.flowboard.collaboration.dto.*;
import com.flowboard.collaboration.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Comment & Attachment Management",
        description = "Threaded comments and file attachments on cards")
@SecurityRequirement(name = "bearerAuth")
public class CommentResource {

    private final CommentService commentService;
    private final JwtConfig jwtConfig;

    // ── Comment Endpoints ─────────────────────────────────────────────────────

    @PostMapping("/comments")
    @Operation(summary = "Add a comment or reply to a card")
    public ResponseEntity<CommentResponse> addComment(
            @Valid @RequestBody CommentRequest request,
            @RequestHeader("Authorization") String bearer) {
        return ResponseEntity.status(201).body(
                commentService.addComment(
                        request, getUserId(bearer)));
    }

    @GetMapping("/comments/{commentId}")
    @Operation(summary = "Get comment by ID")
    public ResponseEntity<CommentResponse> getById(
            @PathVariable Integer commentId) {
        return ResponseEntity.ok(
                commentService.getCommentById(commentId));
    }

    @GetMapping("/cards/{cardId}/comments")
    @Operation(summary = "Get all top-level comments for a card")
    public ResponseEntity<List<CommentResponse>> getByCard(
            @PathVariable Integer cardId) {
        return ResponseEntity.ok(
                commentService.getCommentsByCard(cardId));
    }

    @GetMapping("/comments/{commentId}/replies")
    @Operation(summary = "Get all replies to a comment")
    public ResponseEntity<List<CommentResponse>> getReplies(
            @PathVariable Integer commentId) {
        return ResponseEntity.ok(
                commentService.getReplies(commentId));
    }

    @PutMapping("/comments/{commentId}")
    @Operation(summary = "Update own comment content")
    public ResponseEntity<CommentResponse> update(
            @PathVariable Integer commentId,
            @RequestBody Map<String, String> body,
            @RequestHeader("Authorization") String bearer) {
        return ResponseEntity.ok(commentService.updateComment(
                commentId,
                body.get("content"),
                getUserId(bearer)));
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "Soft delete own comment")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable Integer commentId,
            @RequestHeader("Authorization") String bearer) {
        commentService.deleteComment(commentId, getUserId(bearer));
        return ResponseEntity.ok(
                Map.of("message", "Comment deleted"));
    }

    @GetMapping("/cards/{cardId}/comments/count")
    @Operation(summary = "Get comment count for a card")
    public ResponseEntity<Map<String, Long>> getCount(
            @PathVariable Integer cardId) {
        return ResponseEntity.ok(Map.of("count",
                commentService.getCommentCount(cardId)));
    }

    // ── Attachment Endpoints ──────────────────────────────────────────────────

    @PostMapping("/attachments")
    @Operation(summary = "Add a file attachment to a card")
    public ResponseEntity<AttachmentResponse> addAttachment(
            @Valid @RequestBody AttachmentRequest request,
            @RequestHeader("Authorization") String bearer) {
        return ResponseEntity.status(201).body(
                commentService.addAttachment(
                        request, getUserId(bearer)));
    }

    @GetMapping("/attachments/{attachmentId}")
    @Operation(summary = "Get attachment by ID")
    public ResponseEntity<AttachmentResponse> getAttachment(
            @PathVariable Integer attachmentId) {
        return ResponseEntity.ok(
                commentService.getAttachmentById(attachmentId));
    }

    @GetMapping("/cards/{cardId}/attachments")
    @Operation(summary = "Get all attachments for a card")
    public ResponseEntity<List<AttachmentResponse>> getAttachments(
            @PathVariable Integer cardId) {
        return ResponseEntity.ok(
                commentService.getAttachmentsByCard(cardId));
    }

    @DeleteMapping("/attachments/{attachmentId}")
    @Operation(summary = "Delete own attachment")
    public ResponseEntity<Map<String, String>> deleteAttachment(
            @PathVariable Integer attachmentId,
            @RequestHeader("Authorization") String bearer) {
        commentService.deleteAttachment(
                attachmentId, getUserId(bearer));
        return ResponseEntity.ok(
                Map.of("message", "Attachment deleted"));
    }

    @GetMapping("/cards/{cardId}/attachments/count")
    @Operation(summary = "Get attachment count for a card")
    public ResponseEntity<Map<String, Long>> getAttachmentCount(
            @PathVariable Integer cardId) {
        return ResponseEntity.ok(Map.of("count",
                commentService.getAttachmentCount(cardId)));
    }

    private Integer getUserId(String bearer) {
        return jwtConfig.getUserIdFromToken(bearer.substring(7));
    }
}