package com.flowboard.collaboration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LabelRequest {

    @NotBlank(message = "Label name is required")
    private String name;

    @NotNull(message = "Board ID is required")
    private Integer boardId;

    @NotBlank(message = "Color is required")
    @Pattern(regexp = "^#([A-Fa-f0-9]{6})$",
            message = "Color must be a valid hex e.g. #FF5630")
    private String color;
}