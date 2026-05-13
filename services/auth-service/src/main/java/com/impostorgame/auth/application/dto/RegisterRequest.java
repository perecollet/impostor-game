package com.impostorgame.auth.application.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Email
        String email,

        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotBlank @Size(min = 3, max = 50)
        String displayName
) {}
