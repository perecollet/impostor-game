package com.impostorgame.auth.domain.port.in;

import com.impostorgame.auth.application.dto.AuthResponse;
import com.impostorgame.auth.application.dto.RegisterRequest;

public interface RegisterUseCase {
    AuthResponse register(RegisterRequest request);
}
