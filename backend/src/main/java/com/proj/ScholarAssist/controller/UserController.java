package com.proj.ScholarAssist.controller;

import com.proj.ScholarAssist.dto.LoginRequest;
import com.proj.ScholarAssist.dto.LoginResponse;
import com.proj.ScholarAssist.dto.RegisterRequest;
import com.proj.ScholarAssist.dto.UserResponse;
import com.proj.ScholarAssist.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP endpoints for account registration, session login, and CSRF token retrieval. */
@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

    public UserController(
            UserService userService,
            SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy) {
        this.userService = userService;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        UserResponse response = userService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /** Returns the account in the current session so the browser can restore sign-in state. */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> currentUser(Authentication authentication) {
        return ResponseEntity.ok(userService.getUserResponse(authentication.getName()));
    }

    /** Authenticates credentials and stores the resulting authentication in the HTTP session. */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        Authentication authentication = userService.authenticate(request);
        sessionAuthenticationStrategy.onAuthentication(
                authentication,
                servletRequest,
                servletResponse);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(
                context,
                servletRequest,
                servletResponse);

        LoginResponse response = new LoginResponse(
                "Login successful",
                userService.getUserResponse(authentication.getName()));

        return ResponseEntity.ok(response);
    }

    /** Returns the CSRF token that browser clients must send with state-changing requests. */
    @GetMapping("/csrf")
    public ResponseEntity<CsrfToken> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok(csrfToken);
    }
}
