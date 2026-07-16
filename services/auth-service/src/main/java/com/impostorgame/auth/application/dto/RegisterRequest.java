package com.impostorgame.auth.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email
        String email,

        @NotBlank @Size(min = 8, max = 72, message = "Password must be at least 8 characters")
        String password,

        @NotBlank @Size(min = 3, max = 50)
        String displayName
) {}
