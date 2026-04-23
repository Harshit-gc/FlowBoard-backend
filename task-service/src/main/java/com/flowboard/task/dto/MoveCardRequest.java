package com.flowboard.task.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MoveCardRequest {

    @NotNull(message = "Target list ID is required")
    private Integer targetListId;

    @NotNull(message = "Target board ID is required")
    private Integer targetBoardId;

    private Integer position;
}