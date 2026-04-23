package com.flowboard.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateMemberRoleRequest {

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "ADMIN|MEMBER|OBSERVER",
            message = "Role must be ADMIN, MEMBER or OBSERVER")
    private String role;
}