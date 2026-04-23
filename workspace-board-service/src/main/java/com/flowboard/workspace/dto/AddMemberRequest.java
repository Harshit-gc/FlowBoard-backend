package com.flowboard.workspace.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AddMemberRequest {

    @NotNull(message = "User ID is required")
    private Integer userId;

    @Pattern(regexp = "ADMIN|MEMBER|OBSERVER",
            message = "Role must be ADMIN, MEMBER or OBSERVER")
    private String role = "MEMBER";
}