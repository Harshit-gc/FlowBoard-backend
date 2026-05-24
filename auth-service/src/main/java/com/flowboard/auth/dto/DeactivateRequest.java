package com.flowboard.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeactivateRequest {
    @NotBlank(message = "Password is required")
    private String password;
}