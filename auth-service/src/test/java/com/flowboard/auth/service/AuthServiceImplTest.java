package com.flowboard.auth.service;

import com.flowboard.auth.config.JwtConfig;
import com.flowboard.auth.dto.*;
import com.flowboard.auth.entity.User;
import com.flowboard.auth.exception.AppException;
import com.flowboard.auth.messaging.NotificationPublisher;
import com.flowboard.auth.repository.UserRepository;
import com.flowboard.auth.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtConfig jwtConfig;
    @Mock private NotificationPublisher notificationPublisher;

    @InjectMocks private AuthServiceImpl authService;

    // ─── Shared test fixtures ────────────────────────────────────────────────

    private User activeUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .userId(1)
                .fullName("John Doe")
                .email("john@example.com")
                .username("johndoe")
                .passwordHash("hashed_password")
                .role(User.Role.MEMBER)
                .provider(User.Provider.LOCAL)
                .isActive(true)
                .build();

        inactiveUser = User.builder()
                .userId(2)
                .fullName("Jane Inactive")
                .email("jane@example.com")
                .username("janeinactive")
                .passwordHash("hashed_password")
                .role(User.Role.MEMBER)
                .provider(User.Provider.LOCAL)
                .isActive(false)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // register()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("should register a new user and return AuthResponse with token")
        void register_success() {
            RegisterRequest req = new RegisterRequest();
            req.setFullName("John Doe");
            req.setEmail("john@example.com");
            req.setUsername("johndoe");
            req.setPassword("password123");
            req.setRole(User.Role.MEMBER);

            when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
            when(userRepository.existsByUsername(req.getUsername())).thenReturn(false);
            when(passwordEncoder.encode(req.getPassword())).thenReturn("hashed_password");
            when(userRepository.save(any(User.class))).thenReturn(activeUser);
            when(jwtConfig.generateToken(any(), any(), any())).thenReturn("mock.jwt.token");
            when(userRepository.findAllByRole(User.Role.PLATFORM_ADMIN)).thenReturn(List.of());

            AuthResponse response = authService.register(req);

            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("mock.jwt.token");
            assertThat(response.getType()).isEqualTo("Bearer");
            assertThat(response.getEmail()).isEqualTo("john@example.com");
            assertThat(response.getUsername()).isEqualTo("johndoe");
            assertThat(response.getRole()).isEqualTo("MEMBER");

            verify(userRepository).save(any(User.class));
            verify(jwtConfig).generateToken(any(), eq("MEMBER"), any());
        }

        @Test
        @DisplayName("should assign MEMBER role by default when role is null")
        void register_defaultRoleIsMember() {
            RegisterRequest req = new RegisterRequest();
            req.setFullName("John Doe");
            req.setEmail("john@example.com");
            req.setUsername("johndoe");
            req.setPassword("password123");
            req.setRole(null); // explicitly null

            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(userRepository.existsByUsername(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("hash");
            when(userRepository.save(any(User.class))).thenReturn(activeUser);
            when(jwtConfig.generateToken(any(), any(), any())).thenReturn("token");
            when(userRepository.findAllByRole(User.Role.PLATFORM_ADMIN)).thenReturn(List.of());

            authService.register(req);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getRole()).isEqualTo(User.Role.MEMBER);
        }

        @Test
        @DisplayName("should notify PLATFORM_ADMIN users when a new user registers")
        void register_notifiesAdmins() {
            RegisterRequest req = new RegisterRequest();
            req.setFullName("John Doe");
            req.setEmail("john@example.com");
            req.setUsername("johndoe");
            req.setPassword("password123");

            User admin = User.builder().userId(99).role(User.Role.PLATFORM_ADMIN).build();

            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(userRepository.existsByUsername(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("hash");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setUserId(1);
                return u;
            });
            when(jwtConfig.generateToken(any(), any(), any())).thenReturn("token");
            when(userRepository.findAllByRole(User.Role.PLATFORM_ADMIN)).thenReturn(List.of(admin));

            authService.register(req);

            verify(notificationPublisher, times(1)).publish(any());
        }

        @Test
        @DisplayName("should throw CONFLICT when email already exists")
        void register_duplicateEmail_throwsConflict() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("john@example.com");
            req.setUsername("johndoe");

            when(userRepository.existsByEmail(req.getEmail())).thenReturn(true);

            AppException ex = catchThrowableOfType(
                    () -> authService.register(req), AppException.class);

            assertThat(ex.getMessage()).isEqualTo("Email already registered");
            assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw CONFLICT when username already exists")
        void register_duplicateUsername_throwsConflict() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("newuser@example.com");
            req.setUsername("johndoe");

            when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
            when(userRepository.existsByUsername(req.getUsername())).thenReturn(true);

            AppException ex = catchThrowableOfType(
                    () -> authService.register(req), AppException.class);

            assertThat(ex.getMessage()).isEqualTo("Username already taken");
            assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // login()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("should return AuthResponse on valid credentials")
        void login_success() {
            LoginRequest req = new LoginRequest();
            req.setEmail("john@example.com");
            req.setPassword("password123");

            when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches(req.getPassword(), activeUser.getPasswordHash())).thenReturn(true);
            when(jwtConfig.generateToken(any(), any(), any())).thenReturn("mock.jwt.token");

            AuthResponse response = authService.login(req);

            assertThat(response.getToken()).isEqualTo("mock.jwt.token");
            assertThat(response.getEmail()).isEqualTo("john@example.com");
            assertThat(response.getUsername()).isEqualTo("johndoe");
        }

        @Test
        @DisplayName("should throw UNAUTHORIZED when user not found")
        void login_userNotFound_throwsUnauthorized() {
            LoginRequest req = new LoginRequest();
            req.setEmail("ghost@example.com");
            req.setPassword("any");

            when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> authService.login(req), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(ex.getMessage()).isEqualTo("Invalid email or password");
        }

        @Test
        @DisplayName("should throw FORBIDDEN when account is deactivated")
        void login_deactivatedAccount_throwsForbidden() {
            LoginRequest req = new LoginRequest();
            req.setEmail("jane@example.com");
            req.setPassword("password123");

            when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(inactiveUser));

            AppException ex = catchThrowableOfType(
                    () -> authService.login(req), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(ex.getMessage()).isEqualTo("Account is deactivated");
        }

        @Test
        @DisplayName("should throw UNAUTHORIZED when password is wrong")
        void login_wrongPassword_throwsUnauthorized() {
            LoginRequest req = new LoginRequest();
            req.setEmail("john@example.com");
            req.setPassword("wrongpassword");

            when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches(req.getPassword(), activeUser.getPasswordHash())).thenReturn(false);

            AppException ex = catchThrowableOfType(
                    () -> authService.login(req), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(ex.getMessage()).isEqualTo("Invalid email or password");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // validateToken() & refreshToken()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("validateToken() and refreshToken()")
    class TokenTests {

        @Test
        @DisplayName("validateToken() should return true for a valid token")
        void validateToken_valid() {
            when(jwtConfig.isTokenValid("valid.token")).thenReturn(true);
            assertThat(authService.validateToken("valid.token")).isTrue();
        }

        @Test
        @DisplayName("validateToken() should return false for an invalid token")
        void validateToken_invalid() {
            when(jwtConfig.isTokenValid("bad.token")).thenReturn(false);
            assertThat(authService.validateToken("bad.token")).isFalse();
        }

        @Test
        @DisplayName("refreshToken() should return a new token for a valid token")
        void refreshToken_success() {
            when(jwtConfig.isTokenValid("old.token")).thenReturn(true);
            when(jwtConfig.getEmailFromToken("old.token")).thenReturn("john@example.com");
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
            when(jwtConfig.generateToken(any(), any(), any())).thenReturn("new.token");

            String result = authService.refreshToken("old.token");

            assertThat(result).isEqualTo("new.token");
        }

        @Test
        @DisplayName("refreshToken() should throw UNAUTHORIZED when token is invalid")
        void refreshToken_invalidToken_throwsUnauthorized() {
            when(jwtConfig.isTokenValid("expired.token")).thenReturn(false);

            AppException ex = catchThrowableOfType(
                    () -> authService.refreshToken("expired.token"), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(ex.getMessage()).isEqualTo("Invalid or expired token");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getUserByEmail() & getUserById()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserByEmail() and getUserById()")
    class GetUserTests {

        @Test
        @DisplayName("getUserByEmail() should return user when found")
        void getUserByEmail_found() {
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
            User result = authService.getUserByEmail("john@example.com");
            assertThat(result).isEqualTo(activeUser);
        }

        @Test
        @DisplayName("getUserByEmail() should throw NOT_FOUND when user missing")
        void getUserByEmail_notFound() {
            when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> authService.getUserByEmail("missing@example.com"), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("getUserById() should return user when found")
        void getUserById_found() {
            when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
            User result = authService.getUserById(1);
            assertThat(result).isEqualTo(activeUser);
        }

        @Test
        @DisplayName("getUserById() should throw NOT_FOUND when user missing")
        void getUserById_notFound() {
            when(userRepository.findById(999)).thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> authService.getUserById(999), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(ex.getMessage()).isEqualTo("User not found");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateProfile()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfileTests {

        @Test
        @DisplayName("should update all non-null fields and save")
        void updateProfile_success() {
            UpdateProfileRequest req = new UpdateProfileRequest();
            req.setFullName("John Updated");
            req.setUsername("johnupdated");
            req.setAvatarUrl("http://img.com/avatar.png");
            req.setBio("Developer");
            req.setSecurityQuestion("Pet name?");
            req.setSecurityAnswer("Buddy");

            when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
            when(userRepository.existsByUsername("johnupdated")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = authService.updateProfile(1, req);

            assertThat(result.getFullName()).isEqualTo("John Updated");
            assertThat(result.getUsername()).isEqualTo("johnupdated");
            assertThat(result.getBio()).isEqualTo("Developer");
            assertThat(result.getSecurityAnswer()).isEqualTo("buddy"); // stored lowercase
        }

        @Test
        @DisplayName("should throw CONFLICT when new username is taken by another user")
        void updateProfile_usernameTaken_throwsConflict() {
            UpdateProfileRequest req = new UpdateProfileRequest();
            req.setUsername("takenname");

            when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
            when(userRepository.existsByUsername("takenname")).thenReturn(true);

            AppException ex = catchThrowableOfType(
                    () -> authService.updateProfile(1, req), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(ex.getMessage()).isEqualTo("Username already taken");
        }

        @Test
        @DisplayName("should allow keeping the same username during update")
        void updateProfile_sameUsername_noConflict() {
            UpdateProfileRequest req = new UpdateProfileRequest();
            req.setUsername("johndoe"); // same as existing

            when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatNoException().isThrownBy(() -> authService.updateProfile(1, req));
            verify(userRepository, never()).existsByUsername(any());
        }

        @Test
        @DisplayName("should not change fields that are null in request")
        void updateProfile_nullFieldsIgnored() {
            UpdateProfileRequest req = new UpdateProfileRequest(); // all null

            when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            User result = authService.updateProfile(1, req);

            assertThat(result.getFullName()).isEqualTo("John Doe");
            assertThat(result.getUsername()).isEqualTo("johndoe");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // changePassword()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("changePassword()")
    class ChangePasswordTests {

        @Test
        @DisplayName("should change password when current password is correct")
        void changePassword_success() {
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setCurrentPassword("password123");
            req.setNewPassword("newpassword456");

            when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("password123", activeUser.getPasswordHash())).thenReturn(true);
            when(passwordEncoder.encode("newpassword456")).thenReturn("new_hashed");
            when(userRepository.save(any())).thenReturn(activeUser);

            assertThatNoException().isThrownBy(() -> authService.changePassword(1, req));

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPasswordHash()).isEqualTo("new_hashed");
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when current password is wrong")
        void changePassword_wrongCurrentPassword() {
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setCurrentPassword("wrongpass");
            req.setNewPassword("newpassword456");

            when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("wrongpass", activeUser.getPasswordHash())).thenReturn(false);

            AppException ex = catchThrowableOfType(
                    () -> authService.changePassword(1, req), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getMessage()).isEqualTo("Current password is incorrect");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deactivateAccount() / reactivateUser() / deactivateUser()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Account activation / deactivation")
    class ActivationTests {

        @Test
        @DisplayName("deactivateAccount() should set isActive=false when password matches")
        void deactivateAccount_success() {
            when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("password123", activeUser.getPasswordHash())).thenReturn(true);
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authService.deactivateAccount(1, "password123");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().isActive()).isFalse();
        }

        @Test
        @DisplayName("deactivateAccount() should throw BAD_REQUEST when password is wrong")
        void deactivateAccount_wrongPassword() {
            when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("wrongpass", activeUser.getPasswordHash())).thenReturn(false);

            AppException ex = catchThrowableOfType(
                    () -> authService.deactivateAccount(1, "wrongpass"), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getMessage()).isEqualTo("Incorrect password");
        }

        @Test
        @DisplayName("reactivateUser() should set isActive=true")
        void reactivateUser_success() {
            when(userRepository.findById(2)).thenReturn(Optional.of(inactiveUser));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authService.reactivateUser(2);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().isActive()).isTrue();
        }

        @Test
        @DisplayName("deactivateUser() (admin) should set isActive=false")
        void deactivateUser_success() {
            when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authService.deactivateUser(1);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().isActive()).isFalse();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getSecurityQuestion() & resetPassword()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Forgot Password Flow")
    class ForgotPasswordTests {

        @Test
        @DisplayName("getSecurityQuestion() should return question when user has one set")
        void getSecurityQuestion_success() {
            activeUser.setSecurityQuestion("Favourite city?");

            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));

            SecurityQuestionResponse resp = authService.getSecurityQuestion("john@example.com");

            assertThat(resp.getSecurityQuestion()).isEqualTo("Favourite city?");
            assertThat(resp.getUserId()).isEqualTo(1);
        }

        @Test
        @DisplayName("getSecurityQuestion() should throw NOT_FOUND for unknown email")
        void getSecurityQuestion_unknownEmail() {
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> authService.getSecurityQuestion("ghost@example.com"), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("getSecurityQuestion() should throw BAD_REQUEST when no question is set")
        void getSecurityQuestion_noQuestionSet() {
            activeUser.setSecurityQuestion(null);

            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));

            AppException ex = catchThrowableOfType(
                    () -> authService.getSecurityQuestion("john@example.com"), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getMessage()).isEqualTo("No security question set for this account");
        }

        @Test
        @DisplayName("resetPassword() should update password when all fields are correct")
        void resetPassword_success() {
            activeUser.setSecurityQuestion("Pet?");
            activeUser.setSecurityAnswer("buddy");

            ResetPasswordRequest req = new ResetPasswordRequest();
            req.setEmail("john@example.com");
            req.setUsername("johndoe");
            req.setSecurityAnswer("Buddy"); // case-insensitive check
            req.setNewPassword("newpassword123");

            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.encode("newpassword123")).thenReturn("new_hash");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatNoException().isThrownBy(() -> authService.resetPassword(req));

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPasswordHash()).isEqualTo("new_hash");
        }

        @Test
        @DisplayName("resetPassword() should throw BAD_REQUEST when username does not match")
        void resetPassword_usernameMismatch() {
            ResetPasswordRequest req = new ResetPasswordRequest();
            req.setEmail("john@example.com");
            req.setUsername("wrongusername");
            req.setSecurityAnswer("buddy");
            req.setNewPassword("newpassword123");

            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));

            AppException ex = catchThrowableOfType(
                    () -> authService.resetPassword(req), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getMessage()).isEqualTo("Username does not match");
        }

        @Test
        @DisplayName("resetPassword() should throw BAD_REQUEST when security answer is wrong")
        void resetPassword_wrongSecurityAnswer() {
            activeUser.setSecurityAnswer("buddy");

            ResetPasswordRequest req = new ResetPasswordRequest();
            req.setEmail("john@example.com");
            req.setUsername("johndoe");
            req.setSecurityAnswer("wronganswer");
            req.setNewPassword("newpassword123");

            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));

            AppException ex = catchThrowableOfType(
                    () -> authService.resetPassword(req), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getMessage()).isEqualTo("Security answer is incorrect");
        }

        @Test
        @DisplayName("resetPassword() should throw BAD_REQUEST when new password is too short")
        void resetPassword_passwordTooShort() {
            activeUser.setSecurityAnswer("buddy");

            ResetPasswordRequest req = new ResetPasswordRequest();
            req.setEmail("john@example.com");
            req.setUsername("johndoe");
            req.setSecurityAnswer("buddy");
            req.setNewPassword("abc"); // too short

            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));

            AppException ex = catchThrowableOfType(
                    () -> authService.resetPassword(req), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getMessage()).isEqualTo("Password must be at least 6 characters");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // adminUpdateUser()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("adminUpdateUser()")
    class AdminUpdateUserTests {

        @Test
        @DisplayName("should update all provided admin-editable fields")
        void adminUpdateUser_success() {
            AdminUpdateUserRequest req = new AdminUpdateUserRequest();
            req.setFullName("Admin Updated");
            req.setUsername("newusername");
            req.setEmail("newemail@example.com");
            req.setPassword("newpassword");
            req.setRole(User.Role.BOARD_OWNER);

            when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
            when(userRepository.existsByUsername("newusername")).thenReturn(false);
            when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
            when(passwordEncoder.encode("newpassword")).thenReturn("new_hash");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authService.adminUpdateUser(1, req);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User saved = captor.getValue();
            assertThat(saved.getFullName()).isEqualTo("Admin Updated");
            assertThat(saved.getUsername()).isEqualTo("newusername");
            assertThat(saved.getEmail()).isEqualTo("newemail@example.com");
            assertThat(saved.getPasswordHash()).isEqualTo("new_hash");
            assertThat(saved.getRole()).isEqualTo(User.Role.BOARD_OWNER);
        }

        @Test
        @DisplayName("should throw CONFLICT when admin sets duplicate username")
        void adminUpdateUser_duplicateUsername() {
            AdminUpdateUserRequest req = new AdminUpdateUserRequest();
            req.setUsername("takenname");

            when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
            when(userRepository.existsByUsername("takenname")).thenReturn(true);

            AppException ex = catchThrowableOfType(
                    () -> authService.adminUpdateUser(1, req), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(ex.getMessage()).isEqualTo("Username already taken");
        }

        @Test
        @DisplayName("should throw CONFLICT when admin sets duplicate email")
        void adminUpdateUser_duplicateEmail() {
            AdminUpdateUserRequest req = new AdminUpdateUserRequest();
            req.setEmail("taken@example.com");

            when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
            when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

            AppException ex = catchThrowableOfType(
                    () -> authService.adminUpdateUser(1, req), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(ex.getMessage()).isEqualTo("Email already in use");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // searchUsers() / getAllUsers() / deleteUser()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("User listing and deletion")
    class ListAndDeleteTests {

        @Test
        @DisplayName("searchUsers() should return matching users by name")
        void searchUsers_returnsMatches() {
            when(userRepository.searchByFullName("John")).thenReturn(List.of(activeUser));

            List<User> results = authService.searchUsers("John");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getFullName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("getAllUsers() should return all users")
        void getAllUsers_returnsAll() {
            when(userRepository.findAll()).thenReturn(List.of(activeUser, inactiveUser));

            List<User> results = authService.getAllUsers();

            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("deleteUser() should call deleteById when user exists")
        void deleteUser_success() {
            when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));

            authService.deleteUser(1);

            verify(userRepository).deleteById(1);
        }

        @Test
        @DisplayName("deleteUser() should throw NOT_FOUND when user does not exist")
        void deleteUser_notFound() {
            when(userRepository.findById(999)).thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> authService.deleteUser(999), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            verify(userRepository, never()).deleteById(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // loginWithOAuth2()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("loginWithOAuth2()")
    class OAuth2Tests {

        @Test
        @DisplayName("should create a new user when no account exists for providerId or email")
        void loginWithOAuth2_newUser_created() {
            when(userRepository.findByProviderAndProviderId(User.Provider.GOOGLE, "google-uid-123"))
                    .thenReturn(Optional.empty());
            when(userRepository.findByEmail("oauth@gmail.com")).thenReturn(Optional.empty());
            when(userRepository.existsByUsername("oauth")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setUserId(10);
                return u;
            });
            when(jwtConfig.generateToken(any(), any(), any())).thenReturn("oauth.token");

            AuthResponse resp = authService.loginWithOAuth2(
                    "oauth@gmail.com", "OAuth User", "google-uid-123", User.Provider.GOOGLE);

            assertThat(resp.getToken()).isEqualTo("oauth.token");
            assertThat(resp.getEmail()).isEqualTo("oauth@gmail.com");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getProvider()).isEqualTo(User.Provider.GOOGLE);
            assertThat(captor.getValue().getProviderId()).isEqualTo("google-uid-123");
        }

        @Test
        @DisplayName("should return existing user when found by providerId")
        void loginWithOAuth2_existingUser_byProviderId() {
            activeUser.setProvider(User.Provider.GOOGLE);
            activeUser.setProviderId("google-uid-123");

            when(userRepository.findByProviderAndProviderId(User.Provider.GOOGLE, "google-uid-123"))
                    .thenReturn(Optional.of(activeUser));
            when(jwtConfig.generateToken(any(), any(), any())).thenReturn("oauth.token");

            AuthResponse resp = authService.loginWithOAuth2(
                    "john@example.com", "John Doe", "google-uid-123", User.Provider.GOOGLE);

            assertThat(resp.getEmail()).isEqualTo("john@example.com");
            verify(userRepository, never()).save(any()); // no save needed
        }

        @Test
        @DisplayName("should throw FORBIDDEN when existing OAuth user is deactivated")
        void loginWithOAuth2_deactivatedUser_throwsForbidden() {
            inactiveUser.setProvider(User.Provider.GOOGLE);
            inactiveUser.setProviderId("google-uid-999");

            when(userRepository.findByProviderAndProviderId(User.Provider.GOOGLE, "google-uid-999"))
                    .thenReturn(Optional.of(inactiveUser));

            AppException ex = catchThrowableOfType(
                    () -> authService.loginWithOAuth2(
                            "jane@example.com", "Jane", "google-uid-999", User.Provider.GOOGLE),
                    AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(ex.getMessage()).isEqualTo("Account is deactivated");
        }

        @Test
        @DisplayName("should generate unique username with suffix if base username is taken")
        void loginWithOAuth2_usernameCollision_appendsSuffix() {
            when(userRepository.findByProviderAndProviderId(any(), any())).thenReturn(Optional.empty());
            when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
            when(userRepository.existsByUsername("oauth")).thenReturn(true);   // base taken
            when(userRepository.existsByUsername("oauth1")).thenReturn(false); // suffix free
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setUserId(11);
                return u;
            });
            when(jwtConfig.generateToken(any(), any(), any())).thenReturn("token");

            authService.loginWithOAuth2("oauth@gmail.com", "OAuth", "uid-x", User.Provider.GOOGLE);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getUsername()).isEqualTo("oauth1");
        }
    }
}