package com.flowboard.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskListRequest {

    @NotBlank(message = "List name is required")
    private String name;

    @NotNull(message = "Board ID is required")
    private Integer boardId;

    private String color;
}