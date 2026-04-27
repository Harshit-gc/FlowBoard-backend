package com.flowboard.collaboration.service;

import com.flowboard.collaboration.dto.*;
import java.util.List;

public interface CommentService {

    // Comment operations
    CommentResponse addComment(CommentRequest request,
                               Integer authorId);
    CommentResponse getCommentById(Integer commentId);
    List<CommentResponse> getCommentsByCard(Integer cardId);
    List<CommentResponse> getReplies(Integer commentId);
    CommentResponse updateComment(Integer commentId,
                                  String content,
                                  Integer requesterId);
    void deleteComment(Integer commentId, Integer requesterId);
    long getCommentCount(Integer cardId);

    // Attachment operations
    AttachmentResponse addAttachment(AttachmentRequest request,
                                     Integer uploaderId);
    List<AttachmentResponse> getAttachmentsByCard(Integer cardId);
    AttachmentResponse getAttachmentById(Integer attachmentId);
    void deleteAttachment(Integer attachmentId,
                          Integer requesterId);
    long getAttachmentCount(Integer cardId);
}