package com.impostorgame.auth.infrastructure.adapter.out.persistence;

import com.impostorgame.auth.domain.model.RefreshToken;
import com.impostorgame.auth.domain.port.out.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;
    private final UserJpaRepository userJpaRepository;

    @Override
    public RefreshToken save(RefreshToken token) {
        UserJpaEntity userRef = userJpaRepository.getReferenceById(token.getUserId());
        RefreshTokenJpaEntity entity = RefreshTokenJpaEntity.builder()
                .id(token.getId())
                .user(userRef)
                .token(token.getToken())
                .expiresAt(token.getExpiresAt())
                .createdAt(token.getCreatedAt())
                .build();
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return jpaRepository.findByToken(token).map(this::toDomain);
    }

    @Override
    public void delete(RefreshToken token) {
        jpaRepository.deleteById(token.getId());
    }

    @Override
    public void deleteByUserId(UUID userId) {
        jpaRepository.deleteByUser_Id(userId);
    }

    private RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        return RefreshToken.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .token(entity.getToken())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
