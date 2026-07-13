package com.impostorgame.auth.domain.model;

import com.impostorgame.auth.domain.exception.InvalidUserException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class UserTest {

    @Test
    void create_generatesIdAndCreatedAt() {
        User user = User.create("user@example.com", "hashed", "Alice", Role.USER);

        assertThat(user.id()).isNotNull();
        assertThat(user.createdAt()).isNotNull();
        assertThat(user.email()).isEqualTo("user@example.com");
        assertThat(user.passwordHash()).isEqualTo("hashed");
        assertThat(user.displayName()).isEqualTo("Alice");
        assertThat(user.role()).isEqualTo(Role.USER);
    }

    @Test
    void create_generatesDistinctIds() {
        User first = User.create("a@example.com", "hashed", "Alice", Role.USER);
        User second = User.create("b@example.com", "hashed", "Bob", Role.USER);

        assertThat(first.id()).isNotEqualTo(second.id());
    }

    @Test
    void restore_preservesIdAndCreatedAt() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");

        User user = User.restore(id, "user@example.com", "hashed", "Alice", Role.USER, createdAt);

        assertThat(user.id()).isEqualTo(id);
        assertThat(user.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void create_throws_whenEmailIsBlank() {
        assertThatThrownBy(() -> User.create("   ", "hashed", "Alice", Role.USER))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void create_throws_whenEmailIsNull() {
        assertThatThrownBy(() -> User.create(null, "hashed", "Alice", Role.USER))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void create_throws_whenPasswordHashIsBlank() {
        assertThatThrownBy(() -> User.create("user@example.com", "  ", "Alice", Role.USER))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void create_throws_whenDisplayNameIsTooShort() {
        assertThatThrownBy(() -> User.create("user@example.com", "hashed", "ab", Role.USER))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void create_throws_whenDisplayNameIsTooLong() {
        String tooLong = "a".repeat(51);

        assertThatThrownBy(() -> User.create("user@example.com", "hashed", tooLong, Role.USER))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void create_throws_whenRoleIsNull() {
        assertThatThrownBy(() -> User.create("user@example.com", "hashed", "Alice", null))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void restore_throws_whenIdIsNull() {
        Instant createdAt = Instant.now();

        assertThatThrownBy(() -> User.restore(null, "user@example.com", "hashed", "Alice", Role.USER, createdAt))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void restore_throws_whenCreatedAtIsNull() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> User.restore(id, "user@example.com", "hashed", "Alice", Role.USER, null))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void equals_comparesById() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.now();
        User first = User.restore(id, "user@example.com", "hashed", "Alice", Role.USER, createdAt);
        User second = User.restore(id, "other@example.com", "other", "Bob", Role.GUEST, createdAt);

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
    }
}