package com.flowboard.task.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class TaskListResponse {
    private Integer listId;
    private Integer boardId;
    private String name;
    private Integer position;
    private String color;
    private boolean isArchived;
    private long cardCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}