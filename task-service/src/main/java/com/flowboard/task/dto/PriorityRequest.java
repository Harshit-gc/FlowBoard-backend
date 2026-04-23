package com.flowboard.task.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PriorityRequest {

    @Pattern(regexp = "LOW|MEDIUM|HIGH|CRITICAL",
            message = "Priority must be LOW, MEDIUM, HIGH or CRITICAL")
    private String priority;
}