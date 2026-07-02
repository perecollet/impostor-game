package com.impostorgame.auth.infrastructure.adapter.in.web;

import com.impostorgame.auth.application.dto.AuthResponse;
import com.impostorgame.auth.application.dto.GuestRequest;
import com.impostorgame.auth.application.dto.LoginRequest;
import com.impostorgame.auth.application.dto.RefreshTokenRequest;
import com.impostorgame.auth.application.dto.RegisterRequest;
import com.impostorgame.auth.domain.port.in.GuestUseCase;
import com.impostorgame.auth.domain.port.in.LoginUseCase;
import com.impostorgame.auth.domain.port.in.RefreshTokenUseCase;
import com.impostorgame.auth.domain.port.in.RegisterUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;
    private final GuestUseCase guestUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;

    public AuthController(RegisterUseCase registerUseCase, LoginUseCase loginUseCase, GuestUseCase guestUseCase
            , RefreshTokenUseCase refreshTokenUseCase) {
        this.registerUseCase = registerUseCase;
        this.loginUseCase = loginUseCase;
        this.guestUseCase = guestUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registerUseCase.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginUseCase.login(request));
    }

    @PostMapping("/guest")
    public ResponseEntity<AuthResponse> guest(@RequestBody(required = false) GuestRequest request) {
        return ResponseEntity.ok(guestUseCase.guest(request != null ? request : new GuestRequest(null)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(refreshTokenUseCase.refresh(request));
    }
}
