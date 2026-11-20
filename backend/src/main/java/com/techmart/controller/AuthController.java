package com.techmart.controller;

import com.techmart.dto.AuthDtos.AuthResponse;
import com.techmart.dto.AuthDtos.LoginRequest;
import com.techmart.dto.AuthDtos.RegisterRequest;
import com.techmart.dto.AuthDtos.UserDto;
import com.techmart.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    /** POST /api/auth/register */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return service.register(request);
    }

    /** POST /api/auth/login */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    /** GET /api/auth/me (requires token) */
    @GetMapping("/me")
    public UserDto me(Authentication authentication) {
        return service.me((Integer) authentication.getPrincipal());
    }
}
