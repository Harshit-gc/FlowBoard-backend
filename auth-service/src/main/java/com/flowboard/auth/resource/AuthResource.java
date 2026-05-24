package com.flowboard.auth.resource;

import com.flowboard.auth.config.JwtConfig;
import com.flowboard.auth.dto.*;
import com.flowboard.auth.entity.User;
import com.flowboard.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.flowboard.auth.dto.ResetPasswordRequest;
import com.flowboard.auth.dto.SecurityQuestionResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth & User Management", description = "Register, login, profile, JWT, and admin user ops")
public class AuthResource {

    private final AuthService authService;
    private final JwtConfig jwtConfig;

    // ─── PUBLIC ENDPOINTS ────────────────────────────────────────────────────

    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(201).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT token")
    public ResponseEntity<Map<String, String>> refresh(@RequestHeader("Authorization") String bearerToken) {
        String token = bearerToken.replace("Bearer ", "");
        String newToken = authService.refreshToken(token);
        return ResponseEntity.ok(Map.of("token", newToken, "type", "Bearer"));
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate a JWT token (inter-service use)")
    public ResponseEntity<Map<String, Boolean>> validate(@RequestBody ValidateTokenRequest request) {
        boolean valid = authService.validateToken(request.getToken());
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    @GetMapping("/forgot-password/question")
    @Operation(summary = "Get security question by email (public)")
    public ResponseEntity<SecurityQuestionResponse> getSecurityQuestion(
            @RequestParam String email) {
        return ResponseEntity.ok(authService.getSecurityQuestion(email));
    }

    @PostMapping("/forgot-password/reset")
    @Operation(summary = "Reset password using security answer (public)")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(
                Map.of("message", "Password reset successfully"));
    }

    // ─── AUTHENTICATED ENDPOINTS ─────────────────────────────────────────────

    @GetMapping("/profile/{userId}")
    @Operation(summary = "Get user profile by ID", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<User> getProfile(@PathVariable Integer userId) {
        return ResponseEntity.ok(authService.getUserById(userId));
    }

    @PutMapping("/profile/{userId}")
    @Operation(summary = "Update user profile", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<User> updateProfile(
            @PathVariable Integer userId,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateProfile(userId, request));
    }

    @PutMapping("/password/{userId}")
    @Operation(summary = "Change user password", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, String>> changePassword(
            @PathVariable Integer userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userId, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PutMapping("/deactivate/{userId}")
    @Operation(summary = "Deactivate own account", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, String>> deactivate(
            @PathVariable Integer userId,
            @Valid @RequestBody DeactivateRequest request) {
        authService.deactivateAccount(userId, request.getPassword());
        return ResponseEntity.ok(Map.of("message", "Account deactivated"));
    }

    @GetMapping("/search")
    @Operation(summary = "Search users by full name", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<User>> searchUsers(@RequestParam String name) {
        return ResponseEntity.ok(authService.searchUsers(name));
    }

    @GetMapping("/user/{email}")
    @Operation(summary = "Get user by email (inter-service)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(authService.getUserByEmail(email));
    }

    // ─── PLATFORM ADMIN ENDPOINTS ────────────────────────────────────────────

    @GetMapping("/admin/users")
    @Operation(summary = "Get all users (Admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @DeleteMapping("/admin/users/{userId}")
    @Operation(summary = "Permanently delete a user (Admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Integer userId) {
        authService.deleteUser(userId);
        return ResponseEntity.ok(Map.of("message", "User deleted"));
    }

    @PutMapping("/admin/users/{userId}/reactivate")
    @Operation(summary = "Reactivate a suspended user (Admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, String>> reactivateUser(@PathVariable Integer userId) {
        authService.reactivateUser(userId);
        return ResponseEntity.ok(Map.of("message", "User reactivated"));
    }

    @PutMapping("/admin/users/{userId}/deactivate")
    @Operation(summary = "Deactivate a user (Admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, String>> deactivateUser(@PathVariable Integer userId) {
        authService.deactivateUser(userId);
        return ResponseEntity.ok(Map.of("message", "User deactivated"));
    }

    @PutMapping("/admin/users/{userId}")
    @Operation(summary = "Admin update user details", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, String>> adminUpdateUser(
            @PathVariable Integer userId,
            @RequestBody AdminUpdateUserRequest request) {
        authService.adminUpdateUser(userId, request);
        return ResponseEntity.ok(Map.of("message", "User updated successfully"));
    }
}