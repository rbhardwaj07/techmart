package com.techmart.service;

import com.techmart.config.AppMetrics;
import com.techmart.dto.AuthDtos.AuthResponse;
import com.techmart.dto.AuthDtos.LoginRequest;
import com.techmart.dto.AuthDtos.RegisterRequest;
import com.techmart.dto.AuthDtos.UserDto;
import com.techmart.exception.ConflictException;
import com.techmart.exception.UnauthorizedException;
import com.techmart.model.User;
import com.techmart.repository.UserRepository;
import com.techmart.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AppMetrics metrics;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt, AppMetrics metrics) {
        this.metrics = metrics;
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = normalize(req.email());
        if (users.existsByEmail(email)) {
            metrics.authAttempt("register", false);
            log.warn("registration_failed", kv("event", "registration_failed"), kv("reason", "email_exists"));
            throw new ConflictException("An account with this email already exists");
        }
        User user = users.save(new User(req.name().trim(), email, encoder.encode(req.password())));
        metrics.authAttempt("register", true);
        log.info("user_registered", kv("event", "user_registered"), kv("userId", user.getId()));
        return new AuthResponse(jwt.generateToken(user), UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        // Same error for "unknown email" and "wrong password" so attackers can't tell which emails exist.
        User user = users.findByEmail(normalize(req.email()))
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> {
                    // never log the password (or the token); the email is not logged either
                    metrics.authAttempt("login", false);
                    log.warn("login_failed", kv("event", "login_failed"));
                    return new UnauthorizedException("Invalid email or password");
                });
        metrics.authAttempt("login", true);
        log.info("login_succeeded", kv("event", "login_succeeded"), kv("userId", user.getId()));
        return new AuthResponse(jwt.generateToken(user), UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public UserDto me(int userId) {
        return users.findById(userId).map(UserDto::from)
                .orElseThrow(() -> new UnauthorizedException("User no longer exists"));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
