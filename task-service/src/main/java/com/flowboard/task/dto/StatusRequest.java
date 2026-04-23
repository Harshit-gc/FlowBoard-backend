package com.flowboard.task.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class StatusRequest {

    @Pattern(regexp = "TO_DO|IN_PROGRESS|IN_REVIEW|DONE",
            message = "Status must be TO_DO, IN_PROGRESS, IN_REVIEW or DONE")
    private String status;
}