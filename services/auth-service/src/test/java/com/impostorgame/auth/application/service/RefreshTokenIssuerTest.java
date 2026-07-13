package com.impostorgame.auth.application.service;

import com.impostorgame.auth.TestJwtProperties;
import com.impostorgame.auth.application.security.TokenHasher;
import com.impostorgame.auth.domain.model.RefreshToken;
import com.impostorgame.auth.domain.port.out.RefreshTokenRepository;
import com.impostorgame.auth.infrastructure.config.JwtProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class RefreshTokenIssuerTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;

    private final JwtProperties jwtProperties = TestJwtProperties.create();

    private RefreshTokenIssuer issuer() {
        return new RefreshTokenIssuer(refreshTokenRepository, jwtProperties);
    }

    private RefreshToken capturePersistedToken() {
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void issue_persistsHashNotPlainToken() {
        UUID userId = UUID.randomUUID();

        String plain = issuer().issue(userId);

        RefreshToken persisted = capturePersistedToken();
        assertThat(persisted.tokenHash()).isEqualTo(TokenHasher.sha256(plain));
        assertThat(persisted.tokenHash()).isNotEqualTo(plain);
    }

    @Test
    void issue_persistsTokenForGivenUser() {
        UUID userId = UUID.randomUUID();

        issuer().issue(userId);

        assertThat(capturePersistedToken().userId()).isEqualTo(userId);
    }

    @Test
    void issue_generatesDistinctTokensOnEachCall() {
        UUID userId = UUID.randomUUID();

        String first = issuer().issue(userId);
        String second = issuer().issue(userId);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void issue_setsExpiryFromConfiguredTtl() {
        UUID userId = UUID.randomUUID();

        issuer().issue(userId);

        RefreshToken persisted = capturePersistedToken();
        assertThat(persisted.expiresAt())
                .isCloseTo(persisted.createdAt().plus(TestJwtProperties.REFRESH_EXPIRATION),
                        within(1, ChronoUnit.SECONDS));
    }

    @Test
    void issue_createsTokenThatIsNotExpired() {
        UUID userId = UUID.randomUUID();

        issuer().issue(userId);

        assertThat(capturePersistedToken().isExpired()).isFalse();
    }
}