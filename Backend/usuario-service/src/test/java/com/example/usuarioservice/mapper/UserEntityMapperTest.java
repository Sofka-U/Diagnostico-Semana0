package com.example.usuarioservice.mapper;

import com.example.usuarioservice.entity.UserEntity;
import com.example.usuarioservice.model.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserEntityMapperTest {

    private final UserEntityMapper mapper = new UserEntityMapper();

    @Test
    void toDomain_null_returnsNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toEntity_null_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toDomain_and_toEntity_mapFields() {
        UserEntity entity = UserEntity.builder()
                .id(7)
                .name("Alice")
                .password("pwd")
                .mail("a@example.com")
                .active(true)
                .build();

        User domain = mapper.toDomain(entity);

        assertThat(domain).isNotNull();
        assertThat(domain.getId()).isEqualTo(7);
        assertThat(domain.getName()).isEqualTo("Alice");
        assertThat(domain.getMail()).isEqualTo("a@example.com");
        assertThat(domain.isActive()).isTrue();

        UserEntity back = mapper.toEntity(domain);
        assertThat(back).isNotNull();
        assertThat(back.getId()).isEqualTo(domain.getId());
        assertThat(back.getName()).isEqualTo(domain.getName());
        assertThat(back.getMail()).isEqualTo(domain.getMail());
        assertThat(back.isActive()).isEqualTo(domain.isActive());
    }
}
