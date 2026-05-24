package com.flowboard.auth.config;

import com.flowboard.auth.entity.User;
import com.flowboard.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // ← @Lazy breaks the circular dependency
    public OAuth2SuccessHandler(@Lazy AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email      = oAuth2User.getAttribute("email");
        String name       = oAuth2User.getAttribute("name");
        String providerId = oAuth2User.getAttribute("sub");

        var auth = authService.loginWithOAuth2(
                email, name, providerId, User.Provider.GOOGLE
        );

        String redirectUrl = frontendUrl
                + "/auth/callback"
                + "?token="    + auth.getToken()
                + "&userId="   + auth.getUserId()
                + "&email="    + auth.getEmail()
                + "&username=" + auth.getUsername()
                + "&role="     + auth.getRole();

        response.sendRedirect(redirectUrl);
    }
}