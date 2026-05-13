package com.impostorgame.auth.service;

import com.impostorgame.auth.config.JwtProperties;
import com.impostorgame.auth.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

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

    public Claims validateAndExtract(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUserId(String token) {
        return validateAndExtract(token).getSubject();
    }

    public String extractRole(String token) {
        return validateAndExtract(token).get("role", String.class);
    }

    public boolean isValid(String token) {
        try {
            validateAndExtract(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(
                jwtProperties.secret().getBytes(StandardCharsets.UTF_8)
        );
    }
}
