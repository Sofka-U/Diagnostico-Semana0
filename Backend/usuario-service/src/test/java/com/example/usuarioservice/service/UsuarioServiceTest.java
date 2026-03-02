package com.example.usuarioservice.service;

import com.example.usuarioservice.dto.CreateUsuarioRequest;
import com.example.usuarioservice.dto.UpdateUsuarioRequest;
import com.example.usuarioservice.exception.UsuarioNotFoundException;
import com.example.usuarioservice.exception.UsuarioYaExisteException;
import com.example.usuarioservice.model.User;
import com.example.usuarioservice.persistence.IUserPersistence;
import com.example.usuarioservice.validation.ValidationContext;
import com.example.usuarioservice.validation.ValidationContext.ValidationStrategyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService - Business Logic")
class UsuarioServiceTest {

    @Mock
    private IUserPersistence userRepository;

    @Mock
    private ValidationContext validationContext;

    @InjectMocks
    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        // No additional setup needed
    }

    // ============================================================
    // obtenerPorIdentificador tests
    // ============================================================

    @Test
    @DisplayName("obtenerPorIdentificador with valid ID should return user")
    // TEST_PLAN: obtenerPorIdentificador con ID válido - Line ~54
    void obtenerPorIdentificador_conIdValido_retornaUsuario() {
        // Given
        User usuario = new User(1, "Juan", "pass", "juan@test.com", true);
        when(userRepository.findById(1)).thenReturn(usuario);

        // When
        Optional<User> resultado = usuarioService.obtenerPorIdentificador("1");

        // Then
        assertTrue(resultado.isPresent());
        assertEquals(usuario, resultado.get());
        verify(userRepository).findById(1);
    }

    @Test
    @DisplayName("obtenerPorIdentificador with valid email should return user")
    // TEST_PLAN: obtenerPorIdentificador con email válido - Line ~54
    void obtenerPorIdentificador_conEmailValido_retornaUsuario() {
        // Given
        User usuario = new User(1, "Juan", "pass", "juan@test.com", true);
        when(userRepository.findByEmail("juan@test.com")).thenReturn(usuario);

        // When
        Optional<User> resultado = usuarioService.obtenerPorIdentificador("juan@test.com");

        // Then
        assertTrue(resultado.isPresent());
        assertEquals(usuario, resultado.get());
        verify(userRepository).findByEmail("juan@test.com");
    }

    @Test
    @DisplayName("obtenerPorIdentificador with invalid text should return empty")
    // TEST_PLAN: obtenerPorIdentificador con texto inválido - Line ~54
    void obtenerPorIdentificador_conTextoInvalido_retornaEmpty() {
        // Given
        String identificador = "abc123xyz";

        // When
        Optional<User> resultado = usuarioService.obtenerPorIdentificador(identificador);

        // Then
        assertTrue(resultado.isEmpty());
        verify(userRepository, never()).findById(anyInt());
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("obtenerPorIdentificador with blank identifier should return empty")
    // TEST_PLAN: obtenerPorIdentificador - Edge case vacío
    void obtenerPorIdentificador_conIdentificadorVacio_retornaEmpty() {
        // Given
        String identificador = "   ";

        // When
        Optional<User> resultado = usuarioService.obtenerPorIdentificador(identificador);

        // Then
        assertTrue(resultado.isEmpty());
        verify(userRepository, never()).findById(anyInt());
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("obtenerPorIdentificador with null identifier should return empty")
    // TEST_PLAN: obtenerPorIdentificador - Edge case null
    void obtenerPorIdentificador_conIdentificadorNull_retornaEmpty() {
        // When
        Optional<User> resultado = usuarioService.obtenerPorIdentificador(null);

        // Then
        assertTrue(resultado.isEmpty());
        verify(userRepository, never()).findById(anyInt());
        verify(userRepository, never()).findByEmail(anyString());
    }

    // ============================================================
    // crear usuario tests
    // ============================================================

    @Test
    @DisplayName("crear with duplicate email should throw UsuarioYaExisteException")
    // TEST_PLAN: Crear usuario con email duplicado - Line ~126
    void crear_conEmailDuplicado_lanzaException() {
        // Given
        CreateUsuarioRequest request = new CreateUsuarioRequest();
        request.setNombre("Juan");
        request.setEmail("juan@test.com");
        request.setContrasena("Pass123!");

        User usuarioExistente = new User(1, "OtroJuan", "pass", "juan@test.com", true);
        when(userRepository.findByEmail("juan@test.com")).thenReturn(usuarioExistente);
        doNothing().when(validationContext).validateForCreation(any(), any());

        // When & Then
        assertThrows(UsuarioYaExisteException.class, () -> {
            usuarioService.crear(request);
        });

        verify(validationContext).validateForCreation(request, ValidationStrategyType.LENIENT);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear with valid data should create user")
    // TEST_PLAN: Crear usuario con datos válidos - Line ~126
    void crear_conDatosValidos_creaUsuario() {
        // Given
        CreateUsuarioRequest request = new CreateUsuarioRequest();
        request.setNombre("Juan");
        request.setEmail("juan@test.com");
        request.setContrasena("Pass123!");

        User usuarioGuardado = new User(1, "Juan", "Pass123!", "juan@test.com", true);

        when(userRepository.findByEmail("juan@test.com")).thenReturn(null);
        when(userRepository.save(any(User.class))).thenReturn(usuarioGuardado);
        doNothing().when(validationContext).validateForCreation(any(), any());

        // When
        User resultado = usuarioService.crear(request);

        // Then
        assertNotNull(resultado);
        assertEquals(1, resultado.getId());
        assertEquals("juan@test.com", resultado.getMail());
        verify(validationContext).validateForCreation(request, ValidationStrategyType.LENIENT);
        verify(userRepository).save(argThat(u ->
            u.getName().equals("Juan") &&
            u.getMail().equals("juan@test.com") &&
            u.isActive()
        ));
    }

    // ============================================================
    // actualizar usuario tests
    // ============================================================

    @Test
    @DisplayName("actualizar with non-existent user should return empty")
    // TEST_PLAN: Update usuario inexistente - Line ~169
    void actualizar_usuarioInexistente_retornaEmpty() {
        // Given
        UpdateUsuarioRequest request = new UpdateUsuarioRequest();
        request.setNombre("UpdatedName");

        when(userRepository.findById(999)).thenReturn(null);

        // When
        Optional<User> resultado = usuarioService.actualizar(999, request);

        // Then
        assertTrue(resultado.isEmpty());
        verify(userRepository).findById(999);
        verify(userRepository, never()).update(anyInt(), any());
    }

    @Test
    @DisplayName("actualizar with duplicate email should throw UsuarioYaExisteException")
    // TEST_PLAN: Update usuario con email duplicado - Line ~169
    void actualizar_conEmailDuplicado_lanzaException() {
        // Given
        User usuarioExistente = new User(1, "Juan", "pass", "juan@test.com", true);
        User otroUsuario = new User(2, "Pedro", "pass", "pedro@test.com", true);

        UpdateUsuarioRequest request = new UpdateUsuarioRequest();
        request.setEmail("pedro@test.com"); // Email de otro usuario

        when(userRepository.findById(1)).thenReturn(usuarioExistente);
        when(userRepository.findByEmail("pedro@test.com")).thenReturn(otroUsuario);
        doNothing().when(validationContext).validateForUpdate(any(), any());

        // When & Then
        assertThrows(UsuarioYaExisteException.class, () -> {
            usuarioService.actualizar(1, request);
        });

        verify(userRepository, never()).update(anyInt(), any());
    }

    @Test
    @DisplayName("actualizar with valid data should update user")
    // TEST_PLAN: Update usuario válido - Line ~169
    void actualizar_conDatosValidos_actualizaUsuario() {
        // Given
        User usuarioExistente = new User(1, "Juan", "pass", "juan@test.com", true);
        UpdateUsuarioRequest request = new UpdateUsuarioRequest();
        request.setNombre("JuanUpdated");
        request.setEmail("juan.updated@test.com");

        User usuarioActualizado = new User(1, "JuanUpdated", "pass", "juan.updated@test.com", true);

        when(userRepository.findById(1)).thenReturn(usuarioExistente);
        when(userRepository.findByEmail("juan.updated@test.com")).thenReturn(null);
        when(userRepository.update(eq(1), any(User.class))).thenReturn(usuarioActualizado);
        doNothing().when(validationContext).validateForUpdate(any(), any());

        // When
        Optional<User> resultado = usuarioService.actualizar(1, request);

        // Then
        assertTrue(resultado.isPresent());
        assertEquals("JuanUpdated", resultado.get().getName());
        assertEquals("juan.updated@test.com", resultado.get().getMail());
        verify(userRepository).update(eq(1), any(User.class));
    }

    @Test
    @DisplayName("actualizar with same email should not check uniqueness")
    // TEST_PLAN: Update con mismo email - Edge case
    void actualizar_conMismoEmail_noValidaDuplicado() {
        // Given
        User usuarioExistente = new User(1, "Juan", "pass", "juan@test.com", true);
        UpdateUsuarioRequest request = new UpdateUsuarioRequest();
        request.setNombre("JuanUpdated");
        request.setEmail("juan@test.com"); // Mismo email

        User usuarioActualizado = new User(1, "JuanUpdated", "pass", "juan@test.com", true);

        when(userRepository.findById(1)).thenReturn(usuarioExistente);
        when(userRepository.update(eq(1), any(User.class))).thenReturn(usuarioActualizado);
        doNothing().when(validationContext).validateForUpdate(any(), any());

        // When
        Optional<User> resultado = usuarioService.actualizar(1, request);

        // Then
        assertTrue(resultado.isPresent());
        // findByEmail should NOT be called because email didn't change
        verify(userRepository, never()).findByEmail("juan@test.com");
    }

    // ============================================================
    // eliminar usuario tests
    // ============================================================

    @Test
    @DisplayName("eliminar with non-existent user should return false")
    // TEST_PLAN: Delete usuario inexistente - Line ~215
    void eliminar_usuarioInexistente_retornaFalse() {
        // Given
        when(userRepository.deleteById(999)).thenReturn(false);

        // When
        boolean resultado = usuarioService.eliminar(999);

        // Then
        assertFalse(resultado);
        verify(userRepository).deleteById(999);
    }

    @Test
    @DisplayName("eliminar with existing user should return true")
    // TEST_PLAN: Delete usuario existente - Line ~215
    void eliminar_usuarioExistente_retornaTrue() {
        // Given
        when(userRepository.deleteById(1)).thenReturn(true);

        // When
        boolean resultado = usuarioService.eliminar(1);

        // Then
        assertTrue(resultado);
        verify(userRepository).deleteById(1);
    }

    // ============================================================
    // obtenerTodos test
    // ============================================================

    @Test
    @DisplayName("obtenerTodos should return active users")
    // TEST_PLAN: obtenerTodos - Escenario básico
    void obtenerTodos_retornaUsuariosActivos() {
        // Given
        java.util.Collection<User> usuarios = java.util.Arrays.asList(
            new User(1, "Juan", "pass", "juan@test.com", true),
            new User(2, "Pedro", "pass", "pedro@test.com", true)
        );
        when(userRepository.findAllActive()).thenReturn(usuarios);

        // When
        java.util.Collection<User> resultado = usuarioService.obtenerTodos();

        // Then
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        verify(userRepository).findAllActive();
    }

    @Test
    @DisplayName("obtenerPorId should return user when exists")
    // TEST_PLAN: obtenerPorId - Usuario existe
    void obtenerPorId_usuarioExiste_retornaUsuario() {
        // Given
        User usuario = new User(1, "Juan", "pass", "juan@test.com", true);
        when(userRepository.findById(1)).thenReturn(usuario);

        // When
        Optional<User> resultado = usuarioService.obtenerPorId(1);

        // Then
        assertTrue(resultado.isPresent());
        assertEquals(usuario, resultado.get());
    }

    @Test
    @DisplayName("obtenerPorId should return empty when user not found")
    // TEST_PLAN: obtenerPorId - Usuario no existe
    void obtenerPorId_usuarioNoExiste_retornaEmpty() {
        // Given
        when(userRepository.findById(999)).thenReturn(null);

        // When
        Optional<User> resultado = usuarioService.obtenerPorId(999);

        // Then
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("obtenerPorEmail should return user when exists")
    // TEST_PLAN: obtenerPorEmail - Usuario existe
    void obtenerPorEmail_usuarioExiste_retornaUsuario() {
        // Given
        User usuario = new User(1, "Juan", "pass", "juan@test.com", true);
        when(userRepository.findByEmail("juan@test.com")).thenReturn(usuario);

        // When
        Optional<User> resultado = usuarioService.obtenerPorEmail("juan@test.com");

        // Then
        assertTrue(resultado.isPresent());
        assertEquals(usuario, resultado.get());
    }

    @Test
    @DisplayName("obtenerPorEmail should return empty when user not found")
    // TEST_PLAN: obtenerPorEmail - Usuario no existe
    void obtenerPorEmail_usuarioNoExiste_retornaEmpty() {
        // Given
        when(userRepository.findByEmail("nonexistent@test.com")).thenReturn(null);

        // When
        Optional<User> resultado = usuarioService.obtenerPorEmail("nonexistent@test.com");

        // Then
        assertTrue(resultado.isEmpty());
    }
}

