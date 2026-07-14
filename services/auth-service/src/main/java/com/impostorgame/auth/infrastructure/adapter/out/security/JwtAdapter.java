package com.impostorgame.auth.infrastructure.adapter.out.security;

import com.impostorgame.auth.domain.model.Role;
import com.impostorgame.auth.domain.port.out.JwtPort;
import com.impostorgame.auth.infrastructure.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtAdapter implements JwtPort {

    private final JwtProperties jwtProperties;
    private final PrivateKey privateKey;

    public JwtAdapter(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;

        try {
            byte[] pem = jwtProperties.privateKeyLocation().getInputStream().readAllBytes();
            String content = extractKeyContent(pem);
            byte[] decoded = Base64.getDecoder().decode(content);
            this.privateKey = KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA private key", e);
        }
    }

    @Override
    public String generateToken(UUID userId, String displayName, Role role) {
        Duration expiration = role == Role.GUEST
                ? jwtProperties.guestExpiration()
                : jwtProperties.expiration();
        Instant now = Instant.now();
        return Jwts.builder()
                .header().add("kid", jwtProperties.kid()).and()
                .subject(userId.toString())
                .claim("role", role.name())
                .claim("displayName", displayName)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    private String extractKeyContent(byte[] pem) {
        return new String(pem, StandardCharsets.UTF_8)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
    }
}
