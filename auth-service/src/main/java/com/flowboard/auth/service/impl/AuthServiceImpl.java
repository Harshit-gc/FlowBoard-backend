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
import com.flowboard.auth.messaging.NotificationEvent;
import com.flowboard.auth.messaging.NotificationPublisher;
import com.flowboard.auth.dto.ResetPasswordRequest;
import com.flowboard.auth.dto.SecurityQuestionResponse;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;
    private final NotificationPublisher notificationPublisher;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException("Email already registered", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException("Username already taken", HttpStatus.CONFLICT);
        }

        User.Role role = request.getRole() != null ? request.getRole() : User.Role.MEMBER;

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .provider(User.Provider.LOCAL)
                .isActive(true)
                .build();

        userRepository.save(user);
        String token = jwtConfig.generateToken(user.getEmail(), user.getRole().name(), user.getUserId());

        // Notify all PLATFORM_ADMIN users about new registration
        List<User> admins = userRepository.findAllByRole(User.Role.PLATFORM_ADMIN);
        for (User admin : admins) {
            NotificationEvent event = NotificationEvent.builder()
                    .recipientId(admin.getUserId())
                    .actorId(user.getUserId())
                    .type("ASSIGNMENT")
                    .title("New User Registered")
                    .message("A new user is registered: " + user.getUsername())
                    .relatedId(user.getUserId())
                    .relatedType("USER")
                    .deepLinkUrl("/admin")
                    .build();

            notificationPublisher.publish(event);
        }

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
        if (request.getSecurityQuestion() != null)
            user.setSecurityQuestion(request.getSecurityQuestion());
        if (request.getSecurityAnswer() != null)
            user.setSecurityAnswer(
                    request.getSecurityAnswer().trim().toLowerCase());

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
    public SecurityQuestionResponse getSecurityQuestion(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(
                        "No account found with this email", HttpStatus.NOT_FOUND));

        if (user.getSecurityQuestion() == null || user.getSecurityQuestion().isBlank()) {
            throw new AppException(
                    "No security question set for this account", HttpStatus.BAD_REQUEST);
        }

        return SecurityQuestionResponse.builder()
                .userId(user.getUserId())
                .securityQuestion(user.getSecurityQuestion())
                .build();
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        // Step 1 — find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(
                        "No account found with this email", HttpStatus.NOT_FOUND));

        // Step 2 — verify username
        if (!user.getUsername().equalsIgnoreCase(request.getUsername())) {
            throw new AppException(
                    "Username does not match", HttpStatus.BAD_REQUEST);
        }

        // Step 3 — verify security answer (case-insensitive)
        if (user.getSecurityAnswer() == null ||
                !user.getSecurityAnswer().equalsIgnoreCase(
                        request.getSecurityAnswer().trim())) {
            throw new AppException(
                    "Security answer is incorrect", HttpStatus.BAD_REQUEST);
        }

        // Step 4 — set new password
        if (request.getNewPassword() == null ||
                request.getNewPassword().length() < 6) {
            throw new AppException(
                    "Password must be at least 6 characters", HttpStatus.BAD_REQUEST);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // ✅ FIX 1: deactivateAccount now verifies password before deactivating
    @Override
    public void deactivateAccount(Integer userId, String password) {
        User user = getUserById(userId);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AppException("Incorrect password", HttpStatus.BAD_REQUEST);
        }
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    public void reactivateUser(Integer userId) {
        User user = getUserById(userId);
        user.setActive(true);
        userRepository.save(user);
    }

    @Override
    public void deactivateUser(Integer userId) {
        User user = getUserById(userId);
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    public void adminUpdateUser(Integer userId, AdminUpdateUserRequest request) {
        User user = getUserById(userId);
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            if (!request.getUsername().equals(user.getUsername()) &&
                    userRepository.existsByUsername(request.getUsername())) {
                throw new AppException("Username already taken", HttpStatus.CONFLICT);
            }
            user.setUsername(request.getUsername());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (!request.getEmail().equals(user.getEmail()) &&
                    userRepository.existsByEmail(request.getEmail())) {
                throw new AppException("Email already in use", HttpStatus.CONFLICT);
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
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

    @Override
    public AuthResponse loginWithOAuth2(String email, String name,
                                        String providerId, User.Provider provider) {

        // Try to find existing user by providerId first
        User user = userRepository
                .findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    // Not found by providerId — check if email exists
                    return userRepository.findByEmail(email).orElse(null);
                });

        if (user == null) {
            String baseUsername = email.split("@")[0]
                    .replaceAll("[^a-zA-Z0-9]", "");
            String username = baseUsername;

            // Make username unique if taken
            int suffix = 1;
            while (userRepository.existsByUsername(username)) {
                username = baseUsername + suffix++;
            }

            user = User.builder()
                    .fullName(name)
                    .email(email)
                    .username(username)
                    .passwordHash(null)          // no password for OAuth users
                    .role(User.Role.MEMBER)
                    .provider(provider)
                    .providerId(providerId)
                    .isActive(true)
                    .build();

            userRepository.save(user);

        } else {
            // Existing user — update their providerId if not set yet
            if (user.getProviderId() == null) {
                user.setProviderId(providerId);
                user.setProvider(provider);
                userRepository.save(user);
            }

            if (!user.isActive()) {
                throw new AppException("Account is deactivated", HttpStatus.FORBIDDEN);
            }
        }

        String token = jwtConfig.generateToken(
                user.getEmail(),
                user.getRole().name(),
                user.getUserId()
        );

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getUserId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }
}