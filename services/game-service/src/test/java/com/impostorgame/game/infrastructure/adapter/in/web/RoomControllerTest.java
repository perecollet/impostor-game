package com.impostorgame.game.infrastructure.adapter.in.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.impostorgame.game.domain.port.in.RoomResponse;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * RequiredClaimsValidator only runs as part of the real JwtDecoder chain, so a mocked
 * JwtDecoder bean would bypass exactly the logic under test. Instead we sign real RS256
 * JWTs with an ephemeral test key pair and serve the matching JWKS from a throwaway
 * com.sun.net.httpserver.HttpServer, so the production jwtDecoder() bean (unchanged,
 * still built with withJwkSetUri) exercises the full signature + claims validation path.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
class RoomControllerTest {

    private static final String KID = "test-key-1";

    @Container
    @ServiceConnection("redis")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379);

    private static KeyPair keyPair;
    private static HttpServer jwksServer;

    @LocalServerPort
    private int port;

    @BeforeAll
    static void startJwksServer() throws NoSuchAlgorithmException, IOException {
        keyPair = generateRsaKeyPair();
        jwksServer = serveJwks(keyPair);
    }

    @AfterAll
    static void stopJwksServer() {
        jwksServer.stop(0);
    }

    @DynamicPropertySource
    static void jwkSetUri(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://localhost:" + jwksServer.getAddress().getPort() + "/.well-known/jwks.json");
    }

    private RestTestClient client() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void createRoom_withValidJwt_returns201AndRoomResponse() throws JOSEException {
        String token = signJwt(Map.of("sub", "user-1", "displayName", "Alice", "role", "USER"));

        client().post().uri("/rooms")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(RoomResponse.class)
                .value(body -> {
                    assertThat(body.roomCode()).hasSize(6);
                    assertThat(body.phase()).isEqualTo("LOBBY");
                    assertThat(body.players()).hasSize(1);
                    assertThat(body.players().get(0).id()).isEqualTo("user-1");
                    assertThat(body.players().get(0).displayName()).isEqualTo("Alice");
                    assertThat(body.players().get(0).isHost()).isTrue();
                });
    }

    @Test
    void createRoom_withoutToken_returns401() {
        client().post().uri("/rooms")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void createRoom_withJwtMissingRoleClaim_returns401() throws JOSEException {
        String token = signJwt(Map.of("sub", "user-2", "displayName", "Bob"));

        client().post().uri("/rooms")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private static String signJwt(Map<String, Object> claims) throws JOSEException {
        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .expirationTime(new Date(System.currentTimeMillis() + 3_600_000));
        claims.forEach(claimsBuilder::claim);

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KID).build(),
                claimsBuilder.build());
        signedJWT.sign(new RSASSASigner(keyPair.getPrivate()));
        return signedJWT.serialize();
    }

    private static KeyPair generateRsaKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static HttpServer serveJwks(KeyPair keyPair) throws IOException {
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .keyID(KID)
                .keyUse(KeyUse.SIGNATURE)
                .build();

        String jwksJson;
        try {
            jwksJson = new ObjectMapper().writeValueAsString(new JWKSet(rsaKey).toPublicJWKSet().toJSONObject());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize test JWKS", e);
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/.well-known/jwks.json", exchange -> {
            byte[] body = jwksJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        return server;
    }
}
