package com.impostorgame.auth.infrastructure.adapter.out.persistence;

import com.impostorgame.auth.domain.model.User;
import com.impostorgame.auth.domain.port.out.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    public UserRepositoryAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        return toDomain(jpaRepository.save(toEntity(user)));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    private UserJpaEntity toEntity(User user) {
        return UserJpaEntity.builder()
                .id(user.id())
                .email(user.email())
                .passwordHash(user.passwordHash())
                .displayName(user.displayName())
                .role(user.role())
                .createdAt(user.createdAt())
                .build();
    }

    private User toDomain(UserJpaEntity entity) {
        return User.restore(entity.getId(), entity.getEmail(), entity.getPasswordHash(), entity.getDisplayName(), entity.getRole(), entity.getCreatedAt());
    }
}
