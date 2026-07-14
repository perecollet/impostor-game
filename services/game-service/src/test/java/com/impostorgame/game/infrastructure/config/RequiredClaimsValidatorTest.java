package com.impostorgame.game.infrastructure.config;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class RequiredClaimsValidatorTest {

    private final RequiredClaimsValidator validator = new RequiredClaimsValidator();

    private Jwt.Builder jwtBuilder() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .header("kid", "auth-key-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));
    }

    private Jwt validJwt() {
        return jwtBuilder()
                .subject("11111111-1111-1111-1111-111111111111")
                .claim("displayName", "Alice")
                .claim("role", "USER")
                .build();
    }

    @Test
    void validate_succeeds_whenAllClaimsPresent() {
        OAuth2TokenValidatorResult result = validator.validate(validJwt());

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void validate_succeeds_forGuestRole() {
        Jwt jwt = jwtBuilder()
                .subject("11111111-1111-1111-1111-111111111111")
                .claim("displayName", "Anonymous42")
                .claim("role", "GUEST")
                .build();

        assertThat(validator.validate(jwt).hasErrors()).isFalse();
    }

    @Test
    void validate_fails_whenSubjectMissing() {
        Jwt jwt = jwtBuilder()
                .claim("displayName", "Alice")
                .claim("role", "USER")
                .build();

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void validate_fails_whenDisplayNameMissing() {
        Jwt jwt = jwtBuilder()
                .subject("11111111-1111-1111-1111-111111111111")
                .claim("role", "USER")
                .build();

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void validate_fails_whenDisplayNameIsBlank() {
        Jwt jwt = jwtBuilder()
                .subject("11111111-1111-1111-1111-111111111111")
                .claim("displayName", "   ")
                .claim("role", "USER")
                .build();

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void validate_fails_whenRoleMissing() {
        Jwt jwt = jwtBuilder()
                .subject("11111111-1111-1111-1111-111111111111")
                .claim("displayName", "Alice")
                .build();

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void validate_fails_whenRoleIsUnknown() {
        Jwt jwt = jwtBuilder()
                .subject("11111111-1111-1111-1111-111111111111")
                .claim("displayName", "Alice")
                .claim("role", "ADMIN")
                .build();

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void validate_fails_whenRoleIsLowercase() {
        Jwt jwt = jwtBuilder()
                .subject("11111111-1111-1111-1111-111111111111")
                .claim("displayName", "Alice")
                .claim("role", "user")
                .build();

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }
}