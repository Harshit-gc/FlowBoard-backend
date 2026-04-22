package com.flowboard.auth.dto;

import lombok.Data;

@Data
public class ValidateTokenRequest {
    private String token;
}