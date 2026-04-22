package com.flowboard.auth.service;

import com.flowboard.auth.dto.*;
import com.flowboard.auth.entity.User;
import java.util.List;

public interface AuthService {

    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    boolean validateToken(String token);
    String refreshToken(String token);
    User getUserByEmail(String email);
    User getUserById(Integer userId);
    User updateProfile(Integer userId, UpdateProfileRequest request);
    void changePassword(Integer userId, ChangePasswordRequest request);
    void deactivateAccount(Integer userId);
    List<User> searchUsers(String name);
    List<User> getAllUsers();
    void deleteUser(Integer userId);
}