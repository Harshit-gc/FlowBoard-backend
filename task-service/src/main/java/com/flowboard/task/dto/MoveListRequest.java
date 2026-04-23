package com.flowboard.task.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MoveListRequest {

    @NotNull(message = "Target board ID is required")
    private Integer targetBoardId;
}