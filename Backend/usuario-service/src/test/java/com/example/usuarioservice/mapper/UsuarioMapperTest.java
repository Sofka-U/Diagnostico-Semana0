package com.example.usuarioservice.mapper;

import com.example.usuarioservice.dto.CreateUsuarioRequest;
import com.example.usuarioservice.dto.UpdateUsuarioRequest;
import com.example.usuarioservice.dto.UsuarioResponse;
import com.example.usuarioservice.model.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioMapperTest {

    private final UsuarioMapper mapper = new UsuarioMapper();

    @Test
    void toUser_null_returnsNull() {
        assertThat(mapper.toUser(null)).isNull();
    }

    @Test
    void toUser_mapsFields_and_defaultsActive() {
        CreateUsuarioRequest req = new CreateUsuarioRequest();
        req.setNombre("Bob");
        req.setEmail("bob@example.com");
        req.setContrasena("secret");

        User u = mapper.toUser(req);

        assertThat(u).isNotNull();
        assertThat(u.getName()).isEqualTo("Bob");
        assertThat(u.getMail()).isEqualTo("bob@example.com");
        assertThat(u.getPassword()).isEqualTo("secret");
        assertThat(u.isActive()).isTrue();
    }

    @Test
    void toUserUpdate_partial_and_nullBehavior() {
        User existing = new User(1, "Old", "p", "old@example.com", true);

        UpdateUsuarioRequest req = new UpdateUsuarioRequest();
        req.setNombre("NewName");
        // leave other fields null to ensure no overwrite

        User updated = mapper.toUserUpdate(req, existing);
        assertThat(updated.getName()).isEqualTo("NewName");
        assertThat(updated.getMail()).isEqualTo("old@example.com");

        // null request returns existing
        User same = mapper.toUserUpdate(null, existing);
        assertThat(same).isSameAs(existing);
    }

    @Test
    void toResponse_null_and_mapping() {
        assertThat(mapper.toResponse(null)).isNull();

        User u = new User(2, "X", "pw", "x@example.com", false);
        UsuarioResponse resp = mapper.toResponse(u);
        assertThat(resp).isNotNull();
        assertThat(resp.getId()).isEqualTo(2);
        assertThat(resp.getNombre()).isEqualTo("X");
        assertThat(resp.getEmail()).isEqualTo("x@example.com");
        assertThat(resp.isActivo()).isFalse();
    }
}
