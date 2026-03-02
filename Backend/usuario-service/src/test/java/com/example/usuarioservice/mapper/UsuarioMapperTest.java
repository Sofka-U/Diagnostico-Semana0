package com.example.usuarioservice.mapper;

import com.example.usuarioservice.dto.CreateUsuarioRequest;
import com.example.usuarioservice.dto.UpdateUsuarioRequest;
import com.example.usuarioservice.dto.UsuarioResponse;
import com.example.usuarioservice.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UsuarioMapper - comportamiento de mapeo")
class UsuarioMapperTest {

    private final UsuarioMapper mapper = new UsuarioMapper();

    @Test
    @DisplayName("toUser devuelve null cuando request es null")
    void toUser_returnsNull_whenRequestIsNull() {
        assertNull(mapper.toUser(null));
    }

    @Test
    @DisplayName("toUser mapea correctamente campos")
    void toUser_mapsFields() {
        CreateUsuarioRequest req = new CreateUsuarioRequest();
        req.setNombre("Pedro");
        req.setEmail("pedro@example.com");
        req.setContrasena("secret");

        User user = mapper.toUser(req);

        assertNotNull(user);
        assertEquals("Pedro", user.getName());
        assertEquals("pedro@example.com", user.getMail());
        assertEquals("secret", user.getPassword());
        assertTrue(user.isActive());
    }

    @Test
    @DisplayName("toUserUpdate con null request retorna existente")
    void toUserUpdate_returnsExisting_whenRequestIsNull() {
        User existing = new User(1, "X", "p", "x@e.com", true);
        User result = mapper.toUserUpdate(null, existing);
        assertSame(existing, result);
    }

    @Test
    @DisplayName("toUserUpdate aplica solo campos presentes")
    void toUserUpdate_appliesPartialFields() {
        User existing = new User(2, "Old", "oldpw", "old@example.com", true);

        UpdateUsuarioRequest req = new UpdateUsuarioRequest();
        req.setNombre("NewName");
        // email y contrasena se dejan null
        req.setActivo(false);

        User result = mapper.toUserUpdate(req, existing);

        assertEquals(2, result.getId());
        assertEquals("NewName", result.getName());
        assertEquals("oldpw", result.getPassword());
        assertEquals("old@example.com", result.getMail());
        assertFalse(result.isActive());
    }

    @Test
    @DisplayName("toResponse devuelve null cuando user es null")
    void toResponse_returnsNull_whenUserNull() {
        assertNull(mapper.toResponse(null));
    }

    @Test
    @DisplayName("toResponse mapea correctamente a UsuarioResponse")
    void toResponse_mapsFields() {
        User user = new User(10, "Laura", "pw", "l@e.com", true);

        UsuarioResponse resp = mapper.toResponse(user);

        assertNotNull(resp);
        assertEquals(10, resp.getId());
        assertEquals("Laura", resp.getNombre());
        assertEquals("l@e.com", resp.getEmail());
        assertTrue(resp.isActivo());
    }
}
