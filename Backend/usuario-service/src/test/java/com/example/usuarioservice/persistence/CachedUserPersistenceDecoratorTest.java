package com.example.usuarioservice.persistence;

import com.example.usuarioservice.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CachedUserPersistenceDecorator - caching behavior")
class CachedUserPersistenceDecoratorTest {

    @Mock
    private IUserPersistence delegate;

    private CachedUserPersistenceDecorator cachedDecorator;

    @BeforeEach
    void setUp() {
        cachedDecorator = new CachedUserPersistenceDecorator(delegate);
    }

    @Test
    @DisplayName("findById cache miss calls delegate")
    void findById_cacheMiss_shouldCallDelegate() {
        User user = new User(1, "Juan", "pass", "juan@test.com", true);
        when(delegate.findById(1)).thenReturn(user);

        User result = cachedDecorator.findById(1);

        assertEquals(user, result);
        verify(delegate, times(1)).findById(1);
    }

    @Test
    @DisplayName("findById cache hit does not call delegate")
    void findById_cacheHit_shouldNotCallDelegate() {
        User user = new User(1, "Juan", "pass", "juan@test.com", true);
        when(delegate.findById(1)).thenReturn(user);

        cachedDecorator.findById(1);
        User result = cachedDecorator.findById(1);

        assertEquals(user, result);
        verify(delegate, times(1)).findById(1);
    }

    @Test
    @DisplayName("findByEmail cache miss calls delegate")
    void findByEmail_cacheMiss_shouldCallDelegate() {
        User user = new User(1, "Juan", "pass", "juan@test.com", true);
        when(delegate.findByEmail("juan@test.com")).thenReturn(user);

        User result = cachedDecorator.findByEmail("juan@test.com");

        assertEquals(user, result);
        verify(delegate, times(1)).findByEmail("juan@test.com");
    }

    @Test
    @DisplayName("findByEmail cache hit does not call delegate")
    void findByEmail_cacheHit_shouldNotCallDelegate() {
        User user = new User(1, "Juan", "pass", "juan@test.com", true);
        when(delegate.findByEmail("juan@test.com")).thenReturn(user);

        cachedDecorator.findByEmail("juan@test.com");
        User result = cachedDecorator.findByEmail("juan@test.com");

        assertEquals(user, result);
        verify(delegate, times(1)).findByEmail("juan@test.com");
    }

    @Test
    @DisplayName("save invalidates cache")
    void save_shouldInvalidateCache() {
        User user = new User(1, "Juan", "pass", "juan@test.com", true);
        when(delegate.save(user)).thenReturn(user);

        cachedDecorator.save(user);

        verify(delegate, times(1)).save(user);
    }

    @Test
    @DisplayName("clearCache removes all entries")
    void clearCache_shouldRemoveAllEntries() {
        User user = new User(1, "Juan", "pass", "juan@test.com", true);
        when(delegate.findById(1)).thenReturn(user);

        cachedDecorator.findById(1);
        cachedDecorator.clearCache();
        cachedDecorator.findById(1);

        verify(delegate, times(2)).findById(1);
    }

    // ============================================================
    // SUB-FASE 1.1: Edge cases de findById y findByEmail
    // Objetivo: Cubrir branches faltantes en operaciones de consulta
    // ============================================================

    @Test
    @DisplayName("findById when user not found should not cache null")
    void findById_userNotFound_shouldNotCacheNull() {
        // Given: El delegate retorna null para ID inexistente
        when(delegate.findById(999)).thenReturn(null);

        // When: Se consulta un ID inexistente
        User result = cachedDecorator.findById(999);

        // Then: El resultado es null y no se cachea
        assertNull(result);
        verify(delegate, times(1)).findById(999);

        // When: Se consulta nuevamente el mismo ID
        User secondResult = cachedDecorator.findById(999);

        // Then: El delegate debe ser invocado nuevamente (no se cacheó el null)
        assertNull(secondResult);
        verify(delegate, times(2)).findById(999);
    }

    @Test
    @DisplayName("findByEmail with null email should return null without calling delegate")
    void findByEmail_nullEmail_shouldReturnNullWithoutDelegate() {
        // When: Se consulta con email null
        User result = cachedDecorator.findByEmail(null);

        // Then: Retorna null sin llamar al delegate
        assertNull(result);
        verify(delegate, never()).findByEmail(any());
    }

    @Test
    @DisplayName("findByEmail should normalize email to lowercase for cache key")
    void findByEmail_upperCaseEmail_shouldNormalizeLookup() {
        // Arrange: Un usuario existe
        User user = new User(1, "Juan", "pass", "juan@test.com", true);
        // El delegate será invocado con el email ORIGINAL (sin normalizar)
        when(delegate.findByEmail("JUAN@TEST.COM")).thenReturn(user);

        // Act: Primera consulta con email en mayúsculas
        User firstResult = cachedDecorator.findByEmail("JUAN@TEST.COM");

        // Assert: El usuario es encontrado y el delegate fue invocado con el email original
        assertEquals(user, firstResult);
        verify(delegate, times(1)).findByEmail("JUAN@TEST.COM");

        // Act: Segunda consulta con el mismo email en minúsculas
        User secondResult = cachedDecorator.findByEmail("juan@test.com");

        // Assert: El resultado viene del caché porque ambos emails tienen la misma clave normalizada
        // El delegate NO debe ser invocado nuevamente (resultado viene del caché)
        assertEquals(user, secondResult);
        // Verificar que el delegate solo fue llamado 1 vez en total (con el email en mayúsculas)
        verify(delegate, times(1)).findByEmail(anyString());
        // Confirmar que nunca fue llamado con minúsculas
        verify(delegate, never()).findByEmail("juan@test.com");
    }

    @Test
    @DisplayName("findByEmail when user not found should not cache null")
    void findByEmail_userNotFound_shouldNotCacheNull() {
        // Given: El delegate retorna null para email inexistente
        when(delegate.findByEmail("nonexistent@test.com")).thenReturn(null);

        // When: Se consulta un email inexistente
        User result = cachedDecorator.findByEmail("nonexistent@test.com");

        // Then: El resultado es null
        assertNull(result);
        verify(delegate, times(1)).findByEmail("nonexistent@test.com");

        // When: Se consulta nuevamente el mismo email
        User secondResult = cachedDecorator.findByEmail("nonexistent@test.com");

        // Then: El delegate debe ser invocado nuevamente (no se cacheó el null)
        assertNull(secondResult);
        verify(delegate, times(2)).findByEmail("nonexistent@test.com");
    }

    @Test
    @DisplayName("findByEmail should also cache by ID")
    void findByEmail_shouldAlsoCacheById() {
        // Given: Un usuario existe
        User user = new User(42, "Juan", "pass", "juan@test.com", true);
        when(delegate.findByEmail("juan@test.com")).thenReturn(user);

        // When: Se consulta por email
        cachedDecorator.findByEmail("juan@test.com");

        // Then: El usuario también debe estar en el idCache
        // When: Se consulta por ID
        User resultById = cachedDecorator.findById(42);

        // Then: El delegate NO debe ser llamado (viene del idCache)
        assertEquals(user, resultById);
        verify(delegate, never()).findById(42);
    }
}
