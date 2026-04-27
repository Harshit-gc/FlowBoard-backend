package com.flowboard.collaboration.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class ChecklistResponse {
    private Integer checklistId;
    private Integer cardId;
    private String title;
    private Integer position;
    private int totalItems;
    private int completedItems;
    private int progressPercent;
    private List<ChecklistItemResponse> items;
    private LocalDateTime createdAt;
}