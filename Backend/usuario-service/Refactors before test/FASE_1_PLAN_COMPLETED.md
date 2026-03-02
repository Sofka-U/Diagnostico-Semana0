# FASE 1 – Persistence Layer (CRÍTICO)

Implementación de tests unitarios para la capa de persistencia siguiendo TEST_PLAN.md

**Objetivo:** Maximizar cobertura en `persistence/` y `repository/`  
**Alcance:** EXCLUSIVAMENTE capa de persistencia  
**Métrica:** Cubrir todos los escenarios Gherkin del TEST_PLAN.md

---

## 1. CachedUserPersistenceDecorator

### Clase objetivo
- `com.example.usuarioservice.persistence.CachedUserPersistenceDecorator`

### Feature: CachedUserPersistenceDecorator Cache Management

Referencia TEST_PLAN.md: Lines 165-274

#### Escenarios obligatorios a cubrir

| # | Escenario | TEST_PLAN Line | Status |
|----|-----------|---|--------|
| 1 | findById → cache hit | 180-189 | 🔴 PENDIENTE |
| 2 | findById → cache miss | 175-189 | 🟡 PARCIAL* |
| 3 | findById → null no se cachea | 204-210 | 🟢 IMPLEMENTADO |
| 4 | findByEmail → normalización | 212-219 | 🔴 CORREGIDO |
| 5 | findByEmail → null | 221-226 | 🟢 IMPLEMENTADO |
| 6 | update → invalida caché anterior | 228-235 | 🔴 PENDIENTE |
| 7 | partialUpdate → usuario no existe | 237-243 | 🔴 PENDIENTE |
| 8 | deleteById → invalida caché | 245-251 | 🔴 PENDIENTE |
| 9 | deleteAll → limpia caché | 253-259 | 🔴 PENDIENTE |
| 10 | invalidateUserCache → null safe | 261-265 | 🔴 PENDIENTE |
| 11 | getCacheStats → estadísticas | 267-273 | 🔴 PENDIENTE |

*Algunos tests existentes en CachedUserPersistenceDecoratorTest.java

### Tests faltantes a implementar

```java
// TEST_PLAN: findById cache hit - Lines 180-189
@Test
@DisplayName("findById should return cached user on subsequent calls")
void findById_cachHit_shouldReturnFromCache() {
    // Given: Usuario en caché después de primera consulta
    User user = new User(1, "Juan", "pass", "juan@test.com", true);
    when(delegate.findById(1)).thenReturn(user);
    
    // When: Primera consulta
    User first = cachedDecorator.findById(1);
    
    // Then
    assertEquals(user, first);
    verify(delegate, times(1)).findById(1);
    
    // When: Segunda consulta (cache hit)
    User second = cachedDecorator.findById(1);
    
    // Then: No debe invocar delegate nuevamente
    assertEquals(user, second);
    verify(delegate, times(1)).findById(1); // Mismo count
}

// TEST_PLAN: update invalida caché - Lines 228-235
@Test
@DisplayName("update should invalidate both old and new email cache entries")
void update_invalidatesCacheEmailEntries() {
    // Given: Usuario cacheado con email anterior
    User oldUser = new User(1, "Juan", "pass", "old@test.com", true);
    User newUser = new User(1, "Juan", "pass", "new@test.com", true);
    
    when(delegate.findById(1)).thenReturn(oldUser);
    cachedDecorator.findById(1);
    
    when(delegate.findByEmail("old@test.com")).thenReturn(oldUser);
    cachedDecorator.findByEmail("old@test.com");
    
    when(delegate.update(1, newUser)).thenReturn(newUser);
    
    // When: Se actualiza
    cachedDecorator.update(1, newUser);
    
    // Then: Cachés invalidados
    when(delegate.findById(1)).thenReturn(newUser);
    User result = cachedDecorator.findById(1);
    verify(delegate, atLeastOnce()).findById(1);
}

// TEST_PLAN: partialUpdate usuario no existe - Lines 237-243
@Test
@DisplayName("partialUpdate with non-existent user should not throw NPE")
void partialUpdate_nonExistentUser_nullSafe() {
    // Given
    Map<String, Object> updates = Map.of("nombre", "NewName");
    when(delegate.partialUpdate(999, updates)).thenReturn(null);
    
    // When/Then: No NPE
    User result = cachedDecorator.partialUpdate(999, updates);
    assertNull(result);
    verify(delegate).partialUpdate(999, updates);
}

// TEST_PLAN: deleteById invalida caché - Lines 245-251
@Test
@DisplayName("deleteById should invalidate both ID and email cache")
void deleteById_invalidatesUserCache() {
    // Given: Usuario cacheado
    User user = new User(1, "Juan", "pass", "juan@test.com", true);
    when(delegate.findById(1)).thenReturn(user);
    cachedDecorator.findById(1);
    
    when(delegate.findByEmail("juan@test.com")).thenReturn(user);
    cachedDecorator.findByEmail("juan@test.com");
    
    when(delegate.deleteById(1)).thenReturn(true);
    
    // When
    cachedDecorator.deleteById(1);
    
    // Then
    when(delegate.findById(1)).thenReturn(null);
    User result = cachedDecorator.findById(1);
    verify(delegate, atLeastOnce()).findById(1);
}

// TEST_PLAN: deleteAll limpia caché - Lines 253-259
@Test
@DisplayName("deleteAll should clear both caches completely")
void deleteAll_clearsAllCaches() {
    // Given
    User user1 = new User(1, "Juan", "pass", "juan@test.com", true);
    User user2 = new User(2, "Pedro", "pass", "pedro@test.com", true);
    
    when(delegate.findById(1)).thenReturn(user1);
    when(delegate.findById(2)).thenReturn(user2);
    cachedDecorator.findById(1);
    cachedDecorator.findById(2);
    
    // When
    cachedDecorator.deleteAll();
    
    // Then
    when(delegate.findById(1)).thenReturn(null);
    cachedDecorator.findById(1);
    verify(delegate, atLeastOnce()).findById(1);
}

// TEST_PLAN: getCacheStats - Lines 267-273
@Test
@DisplayName("getCacheStats should return correct cache statistics")
void getCacheStats_returnsCacheMetrics() {
    // Given
    User user1 = new User(1, "Juan", "pass", "juan@test.com", true);
    User user2 = new User(2, "Pedro", "pass", "pedro@test.com", true);
    User user3 = new User(3, "Ana", "pass", "ana@test.com", true);
    
    when(delegate.findById(1)).thenReturn(user1);
    when(delegate.findById(2)).thenReturn(user2);
    when(delegate.findById(3)).thenReturn(user3);
    
    cachedDecorator.findById(1);
    cachedDecorator.findById(2);
    cachedDecorator.findById(3);
    
    when(delegate.findByEmail("juan@test.com")).thenReturn(user1);
    when(delegate.findByEmail("pedro@test.com")).thenReturn(user2);
    
    cachedDecorator.findByEmail("juan@test.com");
    cachedDecorator.findByEmail("pedro@test.com");
    
    // When
    String stats = cachedDecorator.getCacheStats();
    
    // Then
    assertNotNull(stats);
    assertTrue(stats.contains("Email entries: 2"));
    assertTrue(stats.contains("ID entries: 3"));
}
```

### Reglas técnicas

- ✅ Usar Mockito para mockear `IUserPersistence`
- ✅ Verificar con `verify(...)`
- ✅ Cubrir TODOS los branches
- ✅ NO usar lenient stubbing
- ✅ Evitar `UnnecessaryStubbingException`
- ✅ Nombrado: `methodName_condition_behavior`
- ✅ Comentario: `// TEST_PLAN: [escenario] - Line XXX`
- ✅ Patrón Given-When-Then

---

## 2. UserJpaPersistence

### Clase objetivo
- `com.example.usuarioservice.persistence.UserJpaPersistence`

### Feature: UserJpaPersistence Database Operations

Referencia TEST_PLAN.md: Lines 276-341

#### Escenarios obligatorios a cubrir

| # | Escenario | TEST_PLAN Line | Status |
|----|-----------|---|--------|
| 1 | update usuario existente | 289-298 | 🟢 IMPLEMENTADO |
| 2 | update usuario inexistente → null | 300-305 | 🔴 PENDIENTE |
| 3 | partialUpdate Boolean | 307-312 | 🔴 PENDIENTE |
| 4 | partialUpdate String "true" | 314-319 | 🔴 PENDIENTE |
| 5 | partialUpdate String "false" | 321-326 | 🔴 PENDIENTE |
| 6 | partialUpdate inexistente → null | 328-333 | 🔴 PENDIENTE |
| 7 | applyUpdates múltiples campos | 336-340 | 🔴 PENDIENTE |

#### Tests faltantes a implementar

```java
// TEST_PLAN: update inexistente - Lines 300-305
@Test
@DisplayName("update should return null when user does not exist")
void update_nonExistentUser_shouldReturnNull() {
    User updateUser = new User(999, "Name", "pass", "mail@test.com", true);
    when(jpaRepository.findById(999)).thenReturn(Optional.empty());
    
    User result = persistence.update(999, updateUser);
    
    assertNull(result);
    verify(jpaRepository).findById(999);
    verify(jpaRepository, never()).save(any());
}

// TEST_PLAN: partialUpdate Boolean - Lines 307-312
@Test
@DisplayName("partialUpdate should parse Boolean value for active field")
void partialUpdate_withBoolean_shouldParseCorrectly() {
    UserEntity existing = UserEntity.builder()
        .id(1).name("Juan").password("pass").mail("juan@test.com").active(false).build();
    Map<String, Object> updates = Map.of("active", Boolean.TRUE);
    
    when(jpaRepository.findById(1)).thenReturn(Optional.of(existing));
    when(jpaRepository.save(any(UserEntity.class))).thenReturn(existing);
    when(mapper.toDomain(any())).thenReturn(new User(1, "Juan", "pass", "juan@test.com", true));
    
    User result = persistence.partialUpdate(1, updates);
    
    assertNotNull(result);
    assertTrue(result.isActive());
    verify(jpaRepository).save(argThat(e -> e.isActive()));
}

// TEST_PLAN: partialUpdate String "true" - Lines 314-319
@Test
@DisplayName("partialUpdate should parse String 'true' to boolean")
void partialUpdate_withStringTrue_shouldParseCorrectly() {
    UserEntity existing = UserEntity.builder()
        .id(1).name("Juan").password("pass").mail("juan@test.com").active(false).build();
    Map<String, Object> updates = Map.of("active", "true");
    
    when(jpaRepository.findById(1)).thenReturn(Optional.of(existing));
    when(jpaRepository.save(any(UserEntity.class))).thenReturn(existing);
    when(mapper.toDomain(any())).thenReturn(new User(1, "Juan", "pass", "juan@test.com", true));
    
    User result = persistence.partialUpdate(1, updates);
    
    assertTrue(result.isActive());
    verify(jpaRepository).save(argThat(e -> e.isActive()));
}

// TEST_PLAN: partialUpdate String "false" - Lines 321-326
@Test
@DisplayName("partialUpdate should parse String 'false' to boolean")
void partialUpdate_withStringFalse_shouldParseCorrectly() {
    UserEntity existing = UserEntity.builder()
        .id(1).name("Juan").password("pass").mail("juan@test.com").active(true).build();
    Map<String, Object> updates = Map.of("active", "false");
    
    when(jpaRepository.findById(1)).thenReturn(Optional.of(existing));
    when(jpaRepository.save(any(UserEntity.class))).thenReturn(existing);
    when(mapper.toDomain(any())).thenReturn(new User(1, "Juan", "pass", "juan@test.com", false));
    
    User result = persistence.partialUpdate(1, updates);
    
    assertFalse(result.isActive());
}

// TEST_PLAN: partialUpdate inexistente - Lines 328-333
@Test
@DisplayName("partialUpdate should return null when user does not exist")
void partialUpdate_nonExistentUser_shouldReturnNull() {
    Map<String, Object> updates = Map.of("name", "NewName");
    when(jpaRepository.findById(999)).thenReturn(Optional.empty());
    
    User result = persistence.partialUpdate(999, updates);
    
    assertNull(result);
    verify(jpaRepository, never()).save(any());
}

// TEST_PLAN: applyUpdates múltiples - Lines 336-340
@Test
@DisplayName("partialUpdate should update multiple fields selectively")
void partialUpdate_multipleFields_shouldUpdateOnlyProvided() {
    UserEntity existing = UserEntity.builder()
        .id(1).name("OldName").password("oldpass").mail("old@test.com").active(true).build();
    Map<String, Object> updates = Map.of(
        "name", "NewName",
        "mail", "new@test.com",
        "password", "newpass"
    );
    
    when(jpaRepository.findById(1)).thenReturn(Optional.of(existing));
    when(jpaRepository.save(any(UserEntity.class))).thenReturn(existing);
    when(mapper.toDomain(any())).thenReturn(
        new User(1, "NewName", "newpass", "new@test.com", true)
    );
    
    User result = persistence.partialUpdate(1, updates);
    
    assertNotNull(result);
    assertEquals("NewName", result.getName());
    assertEquals("new@test.com", result.getMail());
    assertEquals("newpass", result.getPassword());
    assertTrue(result.isActive());
    
    verify(jpaRepository).save(argThat(e -> 
        e.getName().equals("NewName") &&
        e.getMail().equals("new@test.com") &&
        e.getPassword().equals("newpass") &&
        e.isActive()
    ));
}
```

### Reglas técnicas

- ✅ Mockear `UserJpaRepository`
- ✅ Mockear `UserEntityMapper`
- ✅ Verificar mapping con `ArgumentMatchers`
- ✅ Cubrir TODOS los branches
- ✅ No duplicar lógica productiva
- ✅ Nombrado: `methodName_condition_behavior`
- ✅ Comentario: `// TEST_PLAN: [escenario] - Line XXX`

---

## 3. UserRepository (JSON)

### Clase objetivo
- `com.example.usuarioservice.persistence.UserRepository`

### Feature: UserRepository JSON Persistence

Referencia TEST_PLAN.md: Lines 343-450

#### Escenarios obligatorios a cubrir

| # | Escenario | TEST_PLAN Line | Status |
|----|-----------|---|--------|
| 1 | initialize() con USERS_FILE | 355-360 | 🔴 PENDIENTE |
| 2 | initialize() sin config | 362-368 | 🔴 PENDIENTE |
| 3 | loadUsers ID null → generar | 370-375 | 🔴 PENDIENTE |
| 4 | loadUsers ID <= 0 → generar | 377-382 | 🔴 PENDIENTE |
| 5 | writeToFile() jsonFile null | 384-390 | 🔴 PENDIENTE |
| 6 | writeToFile() sin config | 392-398 | 🔴 PENDIENTE |
| 7 | CRUD completo con @TempDir | N/A | 🔴 PENDIENTE |

#### Reglas técnicas

- ✅ Usar `@TempDir` para archivos temporales
- ✅ NO mockear FileSystem
- ✅ Validar contenido REAL del archivo JSON
- ✅ Cubrir escenarios de error
- ✅ Limpiar archivos después
- ✅ Nombrado: `methodName_condition_behavior`
- ✅ Comentario: `// TEST_PLAN: [escenario] - Line XXX`

---

## Reglas Globales FASE 1

✅ **Cada test DEBE:**
- Mapear a escenario TEST_PLAN.md
- Comentario: `// TEST_PLAN: [escenario] - Line XXX`
- Estructura Given-When-Then
- Nombres: `methodName_condition_behavior`
- Verificaciones con `verify()`
- Evitar `UnnecessaryStubbingException`

❌ **NO:**
- Avanzar a otras capas
- Código fuera de `persistence/` y `repository/`
- Usar `lenient()` o desactivar strict mode
- Duplicar lógica productiva
- Usar Reflection innecesariamente
- Modificar código sin bug real

✅ **Build verde:**
- `mvn clean compile test` → 100% ✅
- JaCoCo report generado
- Todos los tests ejecutables

---

## Métricas de Aceptación

| Clase | Tests Base | Faltantes | Total |
|-------|-----------|-----------|--------|
| `CachedUserPersistenceDecorator` | 5 | 6 | **11** |
| `UserJpaPersistence` | 7 | 6 | **13** |
| `UserRepository` | 0 | 7 | **7** |
| **TOTAL** | **12** | **19** | **31** |

**Cobertura esperada:**
- Persistence: ≥90% instrucciones
- Persistence: ≥85% branches

---

## Próximos Pasos

1. ✅ Implementar tests faltantes `CachedUserPersistenceDecoratorTest.java`
2. ✅ Implementar tests faltantes `UserJpaPersistenceTest.java`
3. ✅ Crear `UserRepositoryTest.java` (7 tests)
4. ✅ Ejecutar: `mvn clean test jacoco:report`
5. ✅ Validar cobertura ≥90% instrucciones
6. ✅ BUILD GREEN ✅
7. ✅ Documentar en `FASE_1_RESULTS.md`

