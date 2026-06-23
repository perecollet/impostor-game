package com.impostorgame.auth.infrastructure.config;

import com.impostorgame.auth.domain.model.Role;
import com.impostorgame.auth.domain.port.out.JwtPort;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAdapter implements JwtPort {

    private final JwtProperties jwtProperties;
    private PrivateKey privateKey;

    @PostConstruct
    private void loadPrivateKey() {
        try {
            byte[] pem = jwtProperties.privateKeyLocation().getInputStream().readAllBytes();
            String content = new String(pem, StandardCharsets.UTF_8)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(content);
            this.privateKey = KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA private key", e);
        }
    }

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
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }
}
