package com.flowboard.collaboration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentRequest {

    @NotNull(message = "Card ID is required")
    private Integer cardId;

    @NotBlank(message = "Content is required")
    private String content;

    // null = top-level comment, non-null = reply
    private Integer parentCommentId;
}