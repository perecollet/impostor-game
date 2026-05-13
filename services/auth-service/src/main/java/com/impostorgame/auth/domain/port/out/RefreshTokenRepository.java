package com.impostorgame.auth.domain.port.out;

import com.impostorgame.auth.domain.model.RefreshToken;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken token);
    Optional<RefreshToken> findByToken(String token);
    void delete(RefreshToken token);
    void deleteByUserId(UUID userId);
}
