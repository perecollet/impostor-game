package com.impostorgame.auth.domain.port.in;

import com.impostorgame.auth.application.dto.AuthResponse;
import com.impostorgame.auth.application.dto.LoginRequest;

public interface LoginUseCase {
    AuthResponse login(LoginRequest request);
}
