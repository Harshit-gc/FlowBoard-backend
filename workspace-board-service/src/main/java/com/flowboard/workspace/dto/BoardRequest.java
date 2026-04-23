package com.flowboard.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class BoardRequest {

    @NotBlank(message = "Board name is required")
    private String name;

    private String description;
    private String background;

    @Pattern(regexp = "PUBLIC|PRIVATE",
            message = "Visibility must be PUBLIC or PRIVATE")
    private String visibility = "PRIVATE";

    @NotNull(message = "Workspace ID is required")
    private Integer workspaceId;
}