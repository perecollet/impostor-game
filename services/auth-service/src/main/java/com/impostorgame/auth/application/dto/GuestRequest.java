package com.impostorgame.auth.application.dto;

import jakarta.validation.constraints.Size;

public record GuestRequest(
        @Size(min = 3, max = 50)
        String displayName
) {}
