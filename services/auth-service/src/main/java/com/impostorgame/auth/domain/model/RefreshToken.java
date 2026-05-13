package com.impostorgame.auth.domain.model;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefreshToken {
    private UUID id;
    private UUID userId;
    private String token;
    private Instant expiresAt;
    private Instant createdAt;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
