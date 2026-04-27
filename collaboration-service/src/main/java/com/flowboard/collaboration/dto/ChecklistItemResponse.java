package com.flowboard.collaboration.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data @Builder
public class ChecklistItemResponse {
    private Integer itemId;
    private Integer checklistId;
    private String text;
    private boolean isCompleted;
    private Integer assigneeId;
    private LocalDate dueDate;
}