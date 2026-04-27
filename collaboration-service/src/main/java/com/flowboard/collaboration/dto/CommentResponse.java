package com.flowboard.collaboration.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class CommentResponse {
    private Integer commentId;
    private Integer cardId;
    private Integer authorId;
    private String content;
    private Integer parentCommentId;
    private boolean isDeleted;
    private int replyCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}