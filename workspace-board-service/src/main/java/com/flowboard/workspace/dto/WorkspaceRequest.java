package com.flowboard.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class WorkspaceRequest {

    @NotBlank(message = "Workspace name is required")
    private String name;

    private String description;
    private String logoUrl;

    @Pattern(regexp = "PUBLIC|PRIVATE",
            message = "Visibility must be PUBLIC or PRIVATE")
    private String visibility = "PRIVATE";
}