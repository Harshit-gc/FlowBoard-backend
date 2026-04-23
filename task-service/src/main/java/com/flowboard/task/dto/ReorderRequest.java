package com.flowboard.task.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class ReorderRequest {

    @NotNull(message = "Ordered IDs are required")
    private List<Integer> orderedIds;
}