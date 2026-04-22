package com.flowboard.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank @Email(message = "Valid email required")
    private String email;

    @NotBlank @Size(min = 3, max = 30)
    private String username;

    @NotBlank @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
}