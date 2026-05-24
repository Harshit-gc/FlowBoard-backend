package com.flowboard.auth.dto;

import com.flowboard.auth.entity.User;
import lombok.Data;

@Data
public class AdminUpdateUserRequest {
    private String fullName;
    private String username;
    private String email;
    private String password;
    private User.Role role;
}