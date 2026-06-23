package com.impostorgame.auth.domain.port.out;

import com.impostorgame.auth.domain.model.Role;
import java.util.UUID;

public interface JwtPort {
    String generateToken(UUID userId, String displayName, Role role);
}
