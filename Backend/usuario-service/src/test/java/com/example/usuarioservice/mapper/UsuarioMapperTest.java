package com.example.usuarioservice.mapper;

import com.example.usuarioservice.dto.CreateUsuarioRequest;
import com.example.usuarioservice.dto.UpdateUsuarioRequest;
import com.example.usuarioservice.dto.UsuarioResponse;
import com.example.usuarioservice.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UsuarioMapper - DTO to Entity Transformations")
class UsuarioMapperTest {

    private UsuarioMapper usuarioMapper;

    @BeforeEach
    void setUp() {
        usuarioMapper = new UsuarioMapper();
    }

    // ============================================================
    // toUser tests
    // ============================================================

    @Test
    @DisplayName("toUser with null request should return null")
    // TEST_PLAN: toUser con request null - Lines 424-428
    void toUser_nullRequest_shouldReturnNull() {
        // When
        User resultado = usuarioMapper.toUser(null);

        // Then
        assertNull(resultado);
    }

    @Test
    @DisplayName("toUser with valid request should convert all fields")
    // TEST_PLAN: toUser con valores válidos - Lines 430-438
    void toUser_validRequest_shouldConvertAllFields() {
        // Given
        CreateUsuarioRequest request = new CreateUsuarioRequest();
        request.setNombre("Juan");
        request.setEmail("juan@test.com");
        request.setContrasena("Pass123!");

        // When
        User resultado = usuarioMapper.toUser(request);

        // Then
        assertNotNull(resultado);
        assertEquals("Juan", resultado.getName());
        assertEquals("juan@test.com", resultado.getMail());
        assertEquals("Pass123!", resultado.getPassword());
        assertTrue(resultado.isActive());
    }

    @Test
    @DisplayName("toUser should set active to true by default")
    // TEST_PLAN: toUser debe establecer active en true - Lines 430-438
    void toUser_shouldSetActiveTrue() {
        // Given
        CreateUsuarioRequest request = new CreateUsuarioRequest();
        request.setNombre("Test");
        request.setEmail("test@test.com");
        request.setContrasena("Password123");

        // When
        User resultado = usuarioMapper.toUser(request);

        // Then
        assertTrue(resultado.isActive());
    }

    @Test
    @DisplayName("toUser should preserve all field values without null loss")
    // TEST_PLAN: toUser debe preservar todos los valores sin pérdida
    void toUser_shouldPreserveAllFields() {
        // Given
        CreateUsuarioRequest request = new CreateUsuarioRequest();
        request.setNombre("Juan Diego");
        request.setEmail("juan.diego@example.com");
        request.setContrasena("SecurePass@123");

        // When
        User resultado = usuarioMapper.toUser(request);

        // Then
        assertNotNull(resultado.getName());
        assertNotNull(resultado.getMail());
        assertNotNull(resultado.getPassword());
        assertEquals("Juan Diego", resultado.getName());
        assertEquals("juan.diego@example.com", resultado.getMail());
        assertEquals("SecurePass@123", resultado.getPassword());
    }

    // ============================================================
    // toUserUpdate tests
    // ============================================================

    @Test
    @DisplayName("toUserUpdate with null request should return existing user unchanged")
    // TEST_PLAN: toUserUpdate con request null - Lines 440-445
    void toUserUpdate_nullRequest_shouldReturnExistingUnchanged() {
        // Given
        User existente = new User(1, "Original", "pass", "original@test.com", true);

        // When
        User resultado = usuarioMapper.toUserUpdate(null, existente);

        // Then
        assertNotNull(resultado);
        assertEquals(existente, resultado);
        assertEquals("Original", resultado.getName());
        assertEquals("original@test.com", resultado.getMail());
    }

    @Test
    @DisplayName("toUserUpdate with partial request should update only provided fields")
    // TEST_PLAN: toUserUpdate parcial - Lines 447-454
    void toUserUpdate_partialRequest_shouldUpdateOnlyProvidedFields() {
        // Given
        User existente = new User(1, "Original", "pass", "original@test.com", true);
        UpdateUsuarioRequest request = new UpdateUsuarioRequest();
        request.setNombre("Nuevo");
        // Email, password, activo are null

        // When
        User resultado = usuarioMapper.toUserUpdate(request, existente);

        // Then
        assertNotNull(resultado);
        assertEquals("Nuevo", resultado.getName());
        assertEquals("original@test.com", resultado.getMail()); // No cambió
        assertEquals("pass", resultado.getPassword()); // No cambió
        assertTrue(resultado.isActive()); // No cambió
    }

    @Test
    @DisplayName("toUserUpdate with all fields should update all fields")
    // TEST_PLAN: toUserUpdate con todos los campos - Lines 456-462
    void toUserUpdate_allFields_shouldUpdateAllFields() {
        // Given
        User existente = new User(1, "Original", "oldpass", "original@test.com", true);
        UpdateUsuarioRequest request = new UpdateUsuarioRequest();
        request.setNombre("Nuevo");
        request.setEmail("nuevo@test.com");
        request.setContrasena("newpass");
        request.setActivo(false);

        // When
        User resultado = usuarioMapper.toUserUpdate(request, existente);

        // Then
        assertNotNull(resultado);
        assertEquals("Nuevo", resultado.getName());
        assertEquals("nuevo@test.com", resultado.getMail());
        assertEquals("newpass", resultado.getPassword());
        assertFalse(resultado.isActive());
    }

    @Test
    @DisplayName("toUserUpdate with null name should preserve original name")
    // TEST_PLAN: toUserUpdate - Null-safe para nombre
    void toUserUpdate_nullName_shouldPreserveOriginal() {
        // Given
        User existente = new User(1, "Original", "pass", "test@test.com", true);
        UpdateUsuarioRequest request = new UpdateUsuarioRequest();
        request.setNombre(null); // Explícitamente null
        request.setEmail("nuevo@test.com");

        // When
        User resultado = usuarioMapper.toUserUpdate(request, existente);

        // Then
        assertEquals("Original", resultado.getName()); // Preservado
        assertEquals("nuevo@test.com", resultado.getMail()); // Actualizado
    }

    @Test
    @DisplayName("toUserUpdate with null email should preserve original email")
    // TEST_PLAN: toUserUpdate - Null-safe para email
    void toUserUpdate_nullEmail_shouldPreserveOriginal() {
        // Given
        User existente = new User(1, "Juan", "pass", "juan@test.com", true);
        UpdateUsuarioRequest request = new UpdateUsuarioRequest();
        request.setNombre("JuanUpdated");
        request.setEmail(null);

        // When
        User resultado = usuarioMapper.toUserUpdate(request, existente);

        // Then
        assertEquals("JuanUpdated", resultado.getName()); // Actualizado
        assertEquals("juan@test.com", resultado.getMail()); // Preservado
    }

    @Test
    @DisplayName("toUserUpdate with null password should preserve original password")
    // TEST_PLAN: toUserUpdate - Null-safe para password
    void toUserUpdate_nullPassword_shouldPreserveOriginal() {
        // Given
        User existente = new User(1, "Juan", "originalpass", "juan@test.com", true);
        UpdateUsuarioRequest request = new UpdateUsuarioRequest();
        request.setNombre("Juan");
        request.setContrasena(null);

        // When
        User resultado = usuarioMapper.toUserUpdate(request, existente);

        // Then
        assertEquals("originalpass", resultado.getPassword()); // Preservado
    }

    @Test
    @DisplayName("toUserUpdate with null active should preserve original active status")
    // TEST_PLAN: toUserUpdate - Null-safe para active
    void toUserUpdate_nullActive_shouldPreserveOriginal() {
        // Given
        User existente = new User(1, "Juan", "pass", "juan@test.com", false);
        UpdateUsuarioRequest request = new UpdateUsuarioRequest();
        request.setActivo(null);
        request.setNombre("JuanUpdated");

        // When
        User resultado = usuarioMapper.toUserUpdate(request, existente);

        // Then
        assertFalse(resultado.isActive()); // Preservado (false)
    }

    @Test
    @DisplayName("toUserUpdate should preserve user ID")
    // TEST_PLAN: toUserUpdate - Preservar ID
    void toUserUpdate_shouldPreserveId() {
        // Given
        User existente = new User(42, "Original", "pass", "original@test.com", true);
        UpdateUsuarioRequest request = new UpdateUsuarioRequest();
        request.setNombre("Nuevo");

        // When
        User resultado = usuarioMapper.toUserUpdate(request, existente);

        // Then
        assertEquals(42, resultado.getId()); // ID preservado
    }

    // ============================================================
    // toResponse tests
    // ============================================================

    @Test
    @DisplayName("toResponse with null user should return null")
    // TEST_PLAN: toResponse con user null - Lines 464-468
    void toResponse_nullUser_shouldReturnNull() {
        // When
        UsuarioResponse resultado = usuarioMapper.toResponse(null);

        // Then
        assertNull(resultado);
    }

    @Test
    @DisplayName("toResponse with valid user should convert all fields")
    // TEST_PLAN: toResponse con user válido
    void toResponse_validUser_shouldConvertAllFields() {
        // Given
        User user = new User(1, "Juan", "pass", "juan@test.com", true);

        // When
        UsuarioResponse resultado = usuarioMapper.toResponse(user);

        // Then
        assertNotNull(resultado);
        assertEquals(1, resultado.getId());
        assertEquals("Juan", resultado.getNombre());
        assertEquals("juan@test.com", resultado.getEmail());
        assertTrue(resultado.isActivo());
    }

    @Test
    @DisplayName("toResponse should preserve all user attributes")
    // TEST_PLAN: toResponse - Preservar todos los atributos
    void toResponse_shouldPreserveAllAttributes() {
        // Given
        User user = new User(99, "TestUser", "testpass", "test@example.com", false);

        // When
        UsuarioResponse resultado = usuarioMapper.toResponse(user);

        // Then
        assertNotNull(resultado);
        assertEquals(99, resultado.getId());
        assertEquals("TestUser", resultado.getNombre());
        assertEquals("test@example.com", resultado.getEmail());
        assertFalse(resultado.isActivo());
    }

    @Test
    @DisplayName("toResponse with inactive user should map active field correctly")
    // TEST_PLAN: toResponse - Mapear active correctamente
    void toResponse_inactiveUser_shouldMapActiveCorrectly() {
        // Given
        User user = new User(5, "InactiveUser", "pass", "inactive@test.com", false);

        // When
        UsuarioResponse resultado = usuarioMapper.toResponse(user);

        // Then
        assertFalse(resultado.isActivo());
    }

    @Test
    @DisplayName("toResponse should use User mail field for response email field")
    // TEST_PLAN: toResponse - Mapear mail a email
    void toResponse_shouldMapMailToEmail() {
        // Given
        User user = new User(1, "Juan", "pass", "juan@example.com", true);

        // When
        UsuarioResponse resultado = usuarioMapper.toResponse(user);

        // Then
        assertEquals("juan@example.com", resultado.getEmail());
        assertEquals(user.getMail(), resultado.getEmail());
    }

    @Test
    @DisplayName("toResponse should use User name field for response nombre field")
    // TEST_PLAN: toResponse - Mapear name a nombre
    void toResponse_shouldMapNameToNombre() {
        // Given
        User user = new User(1, "JuanDiego", "pass", "juan@test.com", true);

        // When
        UsuarioResponse resultado = usuarioMapper.toResponse(user);

        // Then
        assertEquals("JuanDiego", resultado.getNombre());
        assertEquals(user.getName(), resultado.getNombre());
    }
}


