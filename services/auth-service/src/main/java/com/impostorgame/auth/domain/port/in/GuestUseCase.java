package com.impostorgame.auth.domain.port.in;

import com.impostorgame.auth.application.dto.AuthResponse;
import com.impostorgame.auth.application.dto.GuestRequest;

public interface GuestUseCase {
    AuthResponse guest(GuestRequest request);
}
