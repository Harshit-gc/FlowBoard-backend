package com.flowboard.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank
    private String email;

    @NotBlank
    private String username;

    @NotBlank
    private String securityAnswer;

    @NotBlank
    private String newPassword;
}