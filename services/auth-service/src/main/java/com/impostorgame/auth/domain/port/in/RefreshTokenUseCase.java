package com.impostorgame.auth.domain.port.in;

import com.impostorgame.auth.application.dto.AuthResponse;
import com.impostorgame.auth.application.dto.RefreshTokenRequest;

public interface RefreshTokenUseCase {
    AuthResponse refresh(RefreshTokenRequest request);
}
