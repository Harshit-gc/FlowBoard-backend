package com.flowboard.collaboration.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class ChecklistProgressResponse {
    private Integer checklistId;
    private String title;
    private int totalItems;
    private int completedItems;
    private int progressPercent;
}