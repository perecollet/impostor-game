package com.impostorgame.auth.infrastructure.config;

import com.impostorgame.auth.domain.model.Role;
import com.impostorgame.auth.domain.port.out.JwtPort;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAdapter implements JwtPort {

    private final JwtProperties jwtProperties;

    @Override
    public String generateToken(UUID userId, String displayName, Role role) {
        long expiration = role == Role.GUEST
                ? jwtProperties.guestExpiration()
                : jwtProperties.expiration();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role.name())
                .claim("displayName", displayName)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSecretKey())
                .compact();
    }

    @Override
    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(getSecretKey()).build().parseSignedClaims(token).getPayload();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }
}
