package com.flowboard.task.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder
public class CardResponse {
    private Integer cardId;
    private Integer listId;
    private Integer boardId;
    private String title;
    private String description;
    private Integer position;
    private String priority;
    private String status;
    private LocalDate dueDate;
    private LocalDate startDate;
    private Integer assigneeId;
    private Integer createdById;
    private boolean isArchived;
    private boolean isOverdue;
    private String coverColor;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}