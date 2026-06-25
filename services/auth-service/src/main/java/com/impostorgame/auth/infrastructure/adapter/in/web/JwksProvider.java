package com.impostorgame.auth.infrastructure.adapter.in.web;

import com.impostorgame.auth.infrastructure.config.JwtProperties;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwksProvider {

    private final JwtProperties jwtProperties;
    private RSAPublicKey publicKey;

    @PostConstruct
    private void loadPublicKey(){
        try {
            byte[] pem = jwtProperties.publicKeyLocation().getInputStream().readAllBytes();
            String content = new String(pem, StandardCharsets.UTF_8)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(content);
            this.publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(decoded));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA public key", e);
        }
    }

    public Map<String, Object> getJwks(){
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .keyID(jwtProperties.kid())
                .keyUse(KeyUse.SIGNATURE)
                .build();
        return new JWKSet(rsaKey).toPublicJWKSet().toJSONObject();
    }
}
