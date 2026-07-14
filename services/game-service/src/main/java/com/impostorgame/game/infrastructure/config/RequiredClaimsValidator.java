package com.impostorgame.game.infrastructure.config;

import com.impostorgame.game.domain.model.Role;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class RequiredClaimsValidator implements OAuth2TokenValidator<Jwt> {

    private static final String ERROR_CODE = "invalid_token";

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String subject = jwt.getSubject();
        String displayName = jwt.getClaimAsString("displayName");
        String role = jwt.getClaimAsString("role");

        if (subject == null || subject.isBlank()) {
            return failure("Token is missing the 'sub' claim");
        }

        if (displayName == null || displayName.isBlank()) {
            return failure("Token is missing the 'displayName' claim");
        }

        if (role == null || role.isBlank()) {
            return failure("Token is missing the 'role' claim");
        }

        try {
            Role.valueOf(role);
        } catch (IllegalArgumentException e) {
            return failure("Token has an unknown 'role' claim");
        }

        return OAuth2TokenValidatorResult.success();
    }

    private OAuth2TokenValidatorResult failure(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(ERROR_CODE, description, null));
    }
}
