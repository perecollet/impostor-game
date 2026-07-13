package com.impostorgame.auth.infrastructure.adapter.out.persistence;

import com.impostorgame.auth.domain.model.RefreshToken;
import com.impostorgame.auth.domain.port.out.RefreshTokenRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;
    private final UserJpaRepository userJpaRepository;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpaRepository, UserJpaRepository userJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        UserJpaEntity userRef = userJpaRepository.getReferenceById(token.userId());
        RefreshTokenJpaEntity entity = RefreshTokenJpaEntity.builder()
                .id(token.id())
                .user(userRef)
                .tokenHash(token.tokenHash())
                .expiresAt(token.expiresAt())
                .createdAt(token.createdAt())
                .build();
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String token) {
        return jpaRepository.findByTokenHash(token).map(this::toDomain);
    }

    @Override
    public void delete(RefreshToken token) {
        jpaRepository.deleteById(token.id());
    }

    @Override
    public void deleteByUserId(UUID userId) {
        jpaRepository.deleteByUser_Id(userId);
    }

    private RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        return RefreshToken.restore(entity.getId(), entity.getUser().getId(), entity.getTokenHash(),
                entity.getExpiresAt(), entity.getCreatedAt());
    }
}
