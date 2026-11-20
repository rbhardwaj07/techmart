package com.techmart.dto;

import com.techmart.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request/response bodies for registration and login. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 8, max = 72, message = "must be 8 to 72 characters") String password) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record UserDto(Integer id, String name, String email) {
        public static UserDto from(User u) {
            return new UserDto(u.getId(), u.getName(), u.getEmail());
        }
    }

    public record AuthResponse(String token, UserDto user) {
    }
}
