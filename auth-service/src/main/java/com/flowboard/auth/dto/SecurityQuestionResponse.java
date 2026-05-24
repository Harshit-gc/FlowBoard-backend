package com.flowboard.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SecurityQuestionResponse {
    private Integer userId;
    private String securityQuestion;
}