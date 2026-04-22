package com.flowboard.auth.service.impl;

import com.flowboard.auth.config.JwtConfig;
import com.flowboard.auth.dto.*;
import com.flowboard.auth.entity.User;
import com.flowboard.auth.exception.AppException;
import com.flowboard.auth.repository.UserRepository;
import com.flowboard.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException("Email already registered", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException("Username already taken", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.MEMBER)
                .provider(User.Provider.LOCAL)
                .isActive(true)
                .build();

        userRepository.save(user);
        String token = jwtConfig.generateToken(user.getEmail(), user.getRole().name(), user.getUserId());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getUserId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        if (!user.isActive()) {
            throw new AppException("Account is deactivated", HttpStatus.FORBIDDEN);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        String token = jwtConfig.generateToken(user.getEmail(), user.getRole().name(), user.getUserId());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getUserId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public boolean validateToken(String token) {
        return jwtConfig.isTokenValid(token);
    }

    @Override
    public String refreshToken(String token) {
        if (!jwtConfig.isTokenValid(token)) {
            throw new AppException("Invalid or expired token", HttpStatus.UNAUTHORIZED);
        }
        String email = jwtConfig.getEmailFromToken(token);
        User user = getUserByEmail(email);
        return jwtConfig.generateToken(user.getEmail(), user.getRole().name(), user.getUserId());
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
    }

    @Override
    public User getUserById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
    }

    @Override
    public User updateProfile(Integer userId, UpdateProfileRequest request) {
        User user = getUserById(userId);

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getUsername() != null) {
            if (!request.getUsername().equals(user.getUsername()) &&
                    userRepository.existsByUsername(request.getUsername())) {
                throw new AppException("Username already taken", HttpStatus.CONFLICT);
            }
            user.setUsername(request.getUsername());
        }
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getBio() != null) user.setBio(request.getBio());

        return userRepository.save(user);
    }

    @Override
    public void changePassword(Integer userId, ChangePasswordRequest request) {
        User user = getUserById(userId);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException("Current password is incorrect", HttpStatus.BAD_REQUEST);
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public void deactivateAccount(Integer userId) {
        User user = getUserById(userId);
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    public List<User> searchUsers(String name) {
        return userRepository.searchByFullName(name);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public void deleteUser(Integer userId) {
        getUserById(userId); // validates existence
        userRepository.deleteById(userId);
    }
}