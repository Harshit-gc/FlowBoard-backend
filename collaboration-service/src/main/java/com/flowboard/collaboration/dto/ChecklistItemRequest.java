package com.flowboard.collaboration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ChecklistItemRequest {

    @NotNull(message = "Checklist ID is required")
    private Integer checklistId;

    @NotBlank(message = "Item text is required")
    private String text;

    private Integer assigneeId;
    private LocalDate dueDate;
}