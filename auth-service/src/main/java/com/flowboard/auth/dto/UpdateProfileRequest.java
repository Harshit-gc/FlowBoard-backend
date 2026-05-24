package com.flowboard.auth.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private String username;
    private String avatarUrl;
    private String bio;
    private String securityQuestion;
    private String securityAnswer;
}