package com.flowboard.collaboration.service.impl;

import com.flowboard.collaboration.dto.*;
import com.flowboard.collaboration.entity.Attachment;
import com.flowboard.collaboration.entity.Comment;
import com.flowboard.collaboration.exception.AppException;
import com.flowboard.collaboration.repository.AttachmentRepository;
import com.flowboard.collaboration.repository.CommentRepository;
import com.flowboard.collaboration.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final AttachmentRepository attachmentRepository;

    // ── Comment Operations ────────────────────────────────────────────────────

    @Override
    public CommentResponse addComment(CommentRequest request,
                                      Integer authorId) {
        // Validate parent comment exists if replying
        if (request.getParentCommentId() != null) {
            commentRepository.findById(
                            request.getParentCommentId())
                    .orElseThrow(() -> new AppException(
                            "Parent comment not found",
                            HttpStatus.NOT_FOUND));
        }

        Comment comment = Comment.builder()
                .cardId(request.getCardId())
                .authorId(authorId)
                .content(request.getContent())
                .parentCommentId(request.getParentCommentId())
                .isDeleted(false)
                .build();

        return toResponse(commentRepository.save(comment));
    }

    @Override
    public CommentResponse getCommentById(Integer commentId) {
        return toResponse(findComment(commentId));
    }

    @Override
    public List<CommentResponse> getCommentsByCard(Integer cardId) {
        // Return only top-level comments
        return commentRepository
                .findByCardIdAndParentCommentIdIsNull(cardId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentResponse> getReplies(Integer commentId) {
        findComment(commentId); // Validate exists
        return commentRepository
                .findByParentCommentId(commentId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CommentResponse updateComment(Integer commentId,
                                         String content,
                                         Integer requesterId) {
        Comment comment = findComment(commentId);

        // Only author can edit
        if (!comment.getAuthorId().equals(requesterId)) {
            throw new AppException(
                    "Only the author can edit this comment",
                    HttpStatus.FORBIDDEN);
        }
        if (comment.isDeleted()) {
            throw new AppException(
                    "Cannot edit a deleted comment",
                    HttpStatus.BAD_REQUEST);
        }

        comment.setContent(content);
        return toResponse(commentRepository.save(comment));
    }

    @Override
    public void deleteComment(Integer commentId,
                              Integer requesterId) {
        Comment comment = findComment(commentId);

        // Only author can delete
        if (!comment.getAuthorId().equals(requesterId)) {
            throw new AppException(
                    "Only the author can delete this comment",
                    HttpStatus.FORBIDDEN);
        }

        // Soft delete — preserve history
        comment.setDeleted(true);
        comment.setContent("[deleted]");
        commentRepository.save(comment);
    }

    @Override
    public long getCommentCount(Integer cardId) {
        return commentRepository
                .countByCardIdAndIsDeleted(cardId, false);
    }

    // ── Attachment Operations ─────────────────────────────────────────────────

    @Override
    public AttachmentResponse addAttachment(
            AttachmentRequest request,
            Integer uploaderId) {
        Attachment attachment = Attachment.builder()
                .cardId(request.getCardId())
                .uploaderId(uploaderId)
                .fileName(request.getFileName())
                .fileUrl(request.getFileUrl())
                .fileType(request.getFileType())
                .sizeKb(request.getSizeKb())
                .build();
        return toAttachmentResponse(
                attachmentRepository.save(attachment));
    }

    @Override
    public List<AttachmentResponse> getAttachmentsByCard(
            Integer cardId) {
        return attachmentRepository.findByCardId(cardId)
                .stream()
                .map(this::toAttachmentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AttachmentResponse getAttachmentById(
            Integer attachmentId) {
        return toAttachmentResponse(findAttachment(attachmentId));
    }

    @Override
    public void deleteAttachment(Integer attachmentId,
                                 Integer requesterId) {
        Attachment attachment = findAttachment(attachmentId);

        // Only uploader can delete
        if (!attachment.getUploaderId().equals(requesterId)) {
            throw new AppException(
                    "Only the uploader can delete this attachment",
                    HttpStatus.FORBIDDEN);
        }
        attachmentRepository.delete(attachment);
    }

    @Override
    public long getAttachmentCount(Integer cardId) {
        return attachmentRepository.countByCardId(cardId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Comment findComment(Integer commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(
                        "Comment not found", HttpStatus.NOT_FOUND));
    }

    private Attachment findAttachment(Integer attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AppException(
                        "Attachment not found",
                        HttpStatus.NOT_FOUND));
    }

    private CommentResponse toResponse(Comment c) {
        int replyCount = commentRepository
                .findByParentCommentId(c.getCommentId()).size();
        return CommentResponse.builder()
                .commentId(c.getCommentId())
                .cardId(c.getCardId())
                .authorId(c.getAuthorId())
                .content(c.isDeleted() ? "[deleted]" : c.getContent())
                .parentCommentId(c.getParentCommentId())
                .isDeleted(c.isDeleted())
                .replyCount(replyCount)
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private AttachmentResponse toAttachmentResponse(Attachment a) {
        return AttachmentResponse.builder()
                .attachmentId(a.getAttachmentId())
                .cardId(a.getCardId())
                .uploaderId(a.getUploaderId())
                .fileName(a.getFileName())
                .fileUrl(a.getFileUrl())
                .fileType(a.getFileType())
                .sizeKb(a.getSizeKb())
                .uploadedAt(a.getUploadedAt())
                .build();
    }
}