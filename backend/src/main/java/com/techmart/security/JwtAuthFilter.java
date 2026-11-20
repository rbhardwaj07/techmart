package com.techmart.security;

import com.techmart.config.AppMetrics;
import com.techmart.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Runs on every request. If it finds "Authorization: Bearer <token>" with a valid token for an
 * existing user, it marks the request as authenticated. The principal is the user id (Integer).
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwtService;
    private final UserRepository users;
    private final AppMetrics metrics;

    public JwtAuthFilter(JwtService jwtService, UserRepository users, AppMetrics metrics) {
        this.metrics = metrics;
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            Integer userId = jwtService.parseUserId(header.substring(7));
            if (userId != null && users.existsById(userId)) {
                var auth = new UsernamePasswordAuthenticationToken(
                        userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                SecurityContextHolder.getContext().setAuthentication(auth);
                MDC.put("userId", String.valueOf(userId));   // appears on every log line of this request
            } else {
                // The token itself is never logged
                metrics.authAttempt("token", false);
                log.warn("invalid_token", kv("event", "invalid_token"));
            }
        }
        chain.doFilter(request, response);
    }
}
