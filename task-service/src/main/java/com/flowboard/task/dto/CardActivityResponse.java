package com.flowboard.task.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class CardActivityResponse {
    private Integer activityId;
    private Integer cardId;
    private Integer actorId;
    private String action;
    private String oldValue;
    private String newValue;
    private LocalDateTime createdAt;
}