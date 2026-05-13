package com.impostorgame.auth.application.dto;

import jakarta.validation.constraints.*;

public record LoginRequest(
        @NotBlank @Email
        String email,

        @NotBlank
        String password
) {}
