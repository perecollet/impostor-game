package com.impostorgame.auth.domain.model;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private UUID id;
    private String email;
    private String password;
    private String displayName;
    private Role role;
    private Instant createdAt;
}
