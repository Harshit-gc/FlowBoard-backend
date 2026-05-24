package com.flowboard.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CardRequest {

    @NotBlank(message = "Card title is required")
    private String title;

    private String description;

    @NotNull(message = "List ID is required")
    private Integer listId;

    @NotNull(message = "Board ID is required")
    private Integer boardId;

    @Pattern(regexp = "LOW|MEDIUM|HIGH|CRITICAL",
            message = "Priority must be LOW, MEDIUM, HIGH or CRITICAL")
    private String priority;

    @Pattern(regexp = "TO_DO|IN_PROGRESS|IN_REVIEW|DONE",
            message = "Status must be TO_DO, IN_PROGRESS, IN_REVIEW or DONE")
    private String status;

    private LocalDate dueDate;
    private LocalDate startDate;
    private Integer assigneeId;
    private String coverColor;
}