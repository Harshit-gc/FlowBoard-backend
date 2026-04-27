package com.flowboard.collaboration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChecklistRequest {

    @NotNull(message = "Card ID is required")
    private Integer cardId;

    @NotBlank(message = "Checklist title is required")
    private String title;
}