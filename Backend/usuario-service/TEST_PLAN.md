# TEST_PLAN.md - usuario-service

## 1. Overview

| Campo | Valor                            |
|-------|----------------------------------|
| **Versión del documento** | 1.0                              |
| **Fecha** | 26 de febrero de 2026            |
| **Microservicio** | usuario-service                  |
| **Cobertura actual (línea base)** | 82% instrucciones / 65% branches |
| **Meta mínima de cobertura** | 90% instrucciones / 80% branches |
| **Instrucciones totales** | 2,865 (508 missed)               |
| **Branches totales** | 229 (78 missed)                  |

### Resumen de Riesgos Brownfield

Este microservicio tiene código legacy con múltiples estrategias de persistencia (JSON, JPA, Caché), lo que genera:
- Dependencias ocultas entre decoradores de caché y persistencia
- Múltiples paths de ejecución en validadores con estrategias STRICT/LENIENT
- Código de inicialización de archivos JSON que puede fallar silenciosamente
- Configuración condicional de beans que afecta el comportamiento en runtime

---

## 2. Alcance

### 2.1 En Alcance

| Paquete | Clases | Tipo de Prueba |
|---------|--------|----------------|
| `persistence` | `CachedUserPersistenceDecorator`, `UserJpaPersistence`, `UserRepository` | Unitaria + Integración |
| `mapper` | `UsuarioMapper` | Unitaria |
| `validation` | `StrictValidationStrategy`, `LenientValidationStrategy` | Unitaria |
| `service` | `UsuarioService` | Unitaria |
| `messaging` | `UserServiceProducer` | Unitaria |
| `config` | `PersistenceConfig`, `UserPersistenceFactory` | Integración |
| `exception` | `UsuarioNotFoundException`, `UsuarioYaExisteException` | Unitaria |

### 2.2 Fuera de Alcance

| Clase/Paquete | Razón |
|---------------|-------|
| `UsuarioController` | 100% cobertura alcanzada |
| `UserEntityMapper` | 100% cobertura alcanzada |
| `UsuarioResponse` | 100% cobertura (DTO) |
| `GlobalExceptionHandler` | 100% cobertura alcanzada |
| `ValidationContext` | 100% cobertura alcanzada |
| `UserServiceConsumer` | 100% cobertura alcanzada |
| `RabbitMQConfig`, `CorsConfig` | Configuración estática, 100% cobertura |

---

## 3. Niveles de Prueba

### 3.1 Pruebas Unitarias

| Aspecto | Detalle |
|---------|---------|
| **Objetivo** | Validar lógica de negocio aislada en componentes individuales |
| **Herramientas** | JUnit 5, Mockito |
| **Alcance** | `UsuarioService`, `UsuarioMapper`, `StrictValidationStrategy`, `LenientValidationStrategy`, `CachedUserPersistenceDecorator`, `UserServiceProducer` |
| **Estrategia de aislamiento** | Mock de `IUserPersistence`, `ValidationContext`, `RabbitTemplate` |
| **Contribución estimada** | +10-14% cobertura |

### 3.2 Pruebas de Integración

| Aspecto | Detalle |
|---------|---------|
| **Objetivo** | Validar comunicación entre componentes y con infraestructura |
| **Herramientas** | `@SpringBootTest`, `@DataJpaTest`, `MockMvc`, `@MockBean` |
| **Alcance** | `UserJpaPersistence↔JpaRepository`, `UserRepository↔JSON File`, `PersistenceConfig↔Beans` |
| **Contribución estimada** | +6-8% cobertura |

---

## 4. Principios de Testing Aplicados

| Principio | Justificación |
|-----------|---------------|
| **Testing Shows Presence of Defects** | El microservicio tiene múltiples paths sin cubrir (50% en CachedDecorator) - las pruebas detectarán regresiones |
| **Exhaustive Testing is Impossible** | Priorizamos branches y edge cases con mayor impacto de negocio (validación de emails, caché invalidation) |
| **Early Testing** | La deuda técnica en persistencia requiere pruebas antes de nuevas features |
| **Defect Clustering** | El paquete `persistence` concentra 73% de las instrucciones perdidas - priorizar |

---

## 5. Aplicación de Técnicas de Diseño

### 5.1 Partición de Equivalencia

#### CachedUserPersistenceDecorator

| Partición | Tipo | Válida/Inválida | Escenario Mapeado |
|-----------|------|-----------------|-------------------|
| Usuario existente en caché | Valid | Válida | `findById_cachedUser_returnsFromCache` |
| Usuario no en caché | Valid | Válida | `findById_uncachedUser_delegatesToPersistence` |
| Email null | Valid | Inválida | `findByEmail_nullEmail_returnsNull` |
| Email normalizado (mayúsculas) | Valid | Válida | `findByEmail_upperCaseEmail_normalizedLookup` |
| Update invalida caché anterior | Valid | Válida | `update_invalidatesOldAndNewCache` |
| Delete invalida caché | Valid | Válida | `deleteById_invalidatesCache` |

#### UsuarioMapper

| Partición | Tipo | Válida/Inválida | Escenario Mapeado |
|-----------|------|-----------------|-------------------|
| Request null | Input | Inválida | `toUser_nullRequest_returnsNull` |
| Request válido completo | Input | Válida | `toUser_validRequest_createsUserWithAllFields` |
| Update request parcial | Input | Válida | `toUserUpdate_partialFields_updatesOnlyProvided` |
| User null para response | Input | Inválida | `toResponse_nullUser_returnsNull` |

#### UserJpaPersistence

| Partición | Tipo | Válida/Inválida | Escenario Mapeado |
|-----------|------|-----------------|-------------------|
| Usuario existente para update | Valid | Válida | `update_existingUser_updatesAllFields` |
| Usuario inexistente para update | Valid | Inválida | `update_nonExistingUser_returnsNull` |
| Partial update con campo active Boolean | Valid | Válida | `partialUpdate_activeAsBoolean_parsesCorrectly` |
| Partial update con campo active String | Valid | Válida | `partialUpdate_activeAsString_parsesCorrectly` |

### 5.2 Análisis de Valores Límite

#### Validación de Contraseñas (StrictValidationStrategy)

| Campo | Mín | Máx | Entradas Límite | Escenario Mapeado |
|-------|-----|-----|-----------------|-------------------|
| password.length | 12 | - | 11, 12, 13 | `validatePassword_11chars_throwsException`, `validatePassword_12chars_passes` |
| nombre.length | 3 | - | 2, 3, 4 | `validateName_2chars_throwsException`, `validateName_3chars_passes` |

#### Validación de Contraseñas (LenientValidationStrategy)

| Campo | Mín | Máx | Entradas Límite | Escenario Mapeado |
|-------|-----|-----|-----------------|-------------------|
| password.length | 8 | - | 7, 8, 9 | `validatePassword_7chars_throwsException`, `validatePassword_8chars_passes` |
| nombre.length | 2 | - | 1, 2, 3 | `validateName_1char_throwsException`, `validateName_2chars_passes` |

#### UserRepository (JSON persistence)

| Campo | Mín | Máx | Entradas Límite | Escenario Mapeado |
|-------|-----|-----|-----------------|-------------------|
| user.id | 0 | Integer.MAX | 0, 1, -1 | `save_userWithZeroId_generatesNewId`, `save_userWithNegativeId_generatesNewId` |

### 5.3 Tabla de Decisión

#### CachedUserPersistenceDecorator - Invalidación de Caché

| # | Usuario en idCache | Usuario en emailCache | Email cambió | Acción esperada |
|---|--------------------|-----------------------|--------------|-----------------|
| 1 | Sí | Sí | No | Invalidar ambas entradas |
| 2 | Sí | No | No | Invalidar solo idCache |
| 3 | No | Sí | No | Invalidar solo emailCache |
| 4 | Sí | Sí | Sí | Invalidar viejo email + nuevo email + id |
| 5 | No | No | - | No hacer nada (no-op) |

**Escenarios derivados:**
- `update_userInBothCaches_invalidatesBoth`
- `update_emailChanged_invalidatesOldAndNewEmail`
- `partialUpdate_userNotInCache_noInvalidation`

#### StrictValidationStrategy - Validación de Email

| # | Email null | Email con espacios | Dominio válido | Dominio temporal | Acción esperada |
|---|------------|--------------------|--------------------|------------------|-----------------|
| 1 | Sí | - | - | - | Throw "email vacío" |
| 2 | No | Sí | - | - | Throw "espacios" |
| 3 | No | No | No | - | Throw "formato inválido" |
| 4 | No | No | Sí | Sí | Throw "email temporal" |
| 5 | No | No | Sí | No | Pasa validación |

**Escenarios derivados:**
- `validateEmail_null_throwsEmptyException`
- `validateEmail_withSpaces_throwsSpaceException`
- `validateEmail_invalidDomain_throwsFormatException`
- `validateEmail_tempMailDomain_throwsTemporaryEmailException`
- `validateEmail_valid_passes`

---

## 6. Escenarios Gherkin

### 6.1 Escenarios de Pruebas Unitarias

#### Feature: CachedUserPersistenceDecorator Cache Management

```gherkin
Feature: Gestión de caché en CachedUserPersistenceDecorator
  Como sistema de persistencia
  Quiero cachear usuarios consultados
  Para mejorar el rendimiento de consultas repetidas

  Background:
    Given un delegate mock de IUserPersistence
    And un CachedUserPersistenceDecorator configurado

  # Cubre: CachedUserPersistenceDecorator 50% - findById branch sin cubrir
  @critical
  Scenario: Consulta por ID cachea el usuario
    Given un usuario con ID 1 existe en el delegate
    When se consulta findById(1) por primera vez
    Then el delegate debe ser invocado
    And el usuario debe estar en idCache
    When se consulta findById(1) por segunda vez
    Then el delegate NO debe ser invocado nuevamente

  # Cubre: CachedUserPersistenceDecorator - findById retorna null
  @high
  Scenario: Consulta por ID inexistente no cachea null
    Given ningún usuario con ID 999 existe en el delegate
    When se consulta findById(999)
    Then el delegate debe ser invocado
    And idCache NO debe contener la entrada 999

  # Cubre: CachedUserPersistenceDecorator - findByEmail normalización
  @high
  Scenario: Consulta por email normaliza mayúsculas
    Given un usuario con email "TEST@EXAMPLE.COM" existe en el delegate
    When se consulta findByEmail("TEST@EXAMPLE.COM")
    Then el caché debe almacenar con clave "test@example.com"
    When se consulta findByEmail("test@example.com")
    Then el delegate NO debe ser invocado nuevamente

  # Cubre: CachedUserPersistenceDecorator - findByEmail null
  @medium
  Scenario: Consulta por email null retorna null
    When se consulta findByEmail(null)
    Then el resultado debe ser null
    And el delegate NO debe ser invocado

  # Cubre: CachedUserPersistenceDecorator - update invalida caché
  @critical
  Scenario: Actualización invalida caché del usuario anterior
    Given un usuario con ID 1 y email "old@test.com" está cacheado
    When se actualiza el usuario 1 con email "new@test.com"
    Then el caché de email "old@test.com" debe ser invalidado
    And el caché de email "new@test.com" debe ser invalidado
    And el caché de ID 1 debe ser invalidado

  # Cubre: CachedUserPersistenceDecorator - partialUpdate con usuario null
  @high
  Scenario: Actualización parcial con usuario inexistente no falla
    Given ningún usuario con ID 999 existe en el delegate
    When se ejecuta partialUpdate(999, {"name": "Test"})
    Then el resultado debe ser null
    And no debe ocurrir NullPointerException

  # Cubre: CachedUserPersistenceDecorator - deleteById invalida caché
  @high
  Scenario: Eliminación invalida caché del usuario
    Given un usuario con ID 1 y email "test@test.com" está cacheado
    When se ejecuta deleteById(1)
    Then el caché de email "test@test.com" debe ser invalidado
    And el caché de ID 1 debe ser invalidado

  # Cubre: CachedUserPersistenceDecorator - deleteAll limpia caché
  @medium
  Scenario: Eliminar todos limpia todo el caché
    Given varios usuarios están cacheados
    When se ejecuta deleteAll()
    Then emailCache debe estar vacío
    And idCache debe estar vacío

  # Cubre: CachedUserPersistenceDecorator - invalidateUserCache con null
  @medium
  Scenario: Invalidar caché con usuario null no falla
    When se invoca invalidateUserCache(null) via método privado
    Then no debe ocurrir NullPointerException

  # Cubre: CachedUserPersistenceDecorator - getCacheStats
  @low
  Scenario: Obtener estadísticas de caché
    Given 3 usuarios están en idCache
    And 2 usuarios están en emailCache
    When se obtiene getCacheStats()
    Then debe retornar "Cache Stats - Email entries: 2, ID entries: 3"
```

#### Feature: UserJpaPersistence Database Operations

```gherkin
Feature: Operaciones de persistencia JPA
  Como capa de persistencia
  Quiero operar con la base de datos PostgreSQL
  Para persistir y recuperar usuarios

  Background:
    Given un UserJpaRepository mock
    And un UserEntityMapper mock
    And un UserJpaPersistence configurado

  # Cubre: UserJpaPersistence 60% - update branch no cubierto
  @critical
  Scenario: Actualizar usuario existente modifica todos los campos
    Given un usuario con ID 1 existe en el repositorio JPA
    When se ejecuta update(1, usuarioModificado)
    Then el name debe actualizarse
    And el password debe actualizarse
    And el mail debe actualizarse
    And el active debe actualizarse
    And el usuario actualizado debe retornarse mapeado a dominio

  # Cubre: UserJpaPersistence - update usuario inexistente
  @high
  Scenario: Actualizar usuario inexistente retorna null
    Given ningún usuario con ID 999 existe en el repositorio JPA
    When se ejecuta update(999, usuario)
    Then el resultado debe ser null

  # Cubre: UserJpaPersistence - partialUpdate branch Boolean
  @high
  Scenario: Actualización parcial con active como Boolean
    Given un usuario con ID 1 existe en el repositorio JPA
    When se ejecuta partialUpdate(1, {"active": true})
    Then el campo active debe ser true (parseado como Boolean)

  # Cubre: UserJpaPersistence - partialUpdate branch String
  @high
  Scenario: Actualización parcial con active como String "true"
    Given un usuario con ID 1 existe en el repositorio JPA
    When se ejecuta partialUpdate(1, {"active": "true"})
    Then el campo active debe ser true (parseado desde String)

  # Cubre: UserJpaPersistence - partialUpdate branch String "false"
  @medium
  Scenario: Actualización parcial con active como String "false"
    Given un usuario con ID 1 existe en el repositorio JPA
    When se ejecuta partialUpdate(1, {"active": "false"})
    Then el campo active debe ser false

  # Cubre: UserJpaPersistence - partialUpdate usuario inexistente
  @high
  Scenario: Actualización parcial de usuario inexistente retorna null
    Given ningún usuario con ID 999 existe en el repositorio JPA
    When se ejecuta partialUpdate(999, {"name": "Test"})
    Then el resultado debe ser null

  # Cubre: UserJpaPersistence - applyUpdates con todos los campos
  @medium
  Scenario: Actualización parcial aplica múltiples campos
    Given un usuario con ID 1 existe en el repositorio JPA
    When se ejecuta partialUpdate(1, {"name": "Nuevo", "mail": "new@test.com", "password": "newpass"})
    Then todos los campos proporcionados deben actualizarse
```

#### Feature: UserRepository JSON Persistence

```gherkin
Feature: Persistencia de usuarios en JSON
  Como sistema legacy
  Quiero persistir usuarios en archivo JSON
  Para mantener compatibilidad con sistemas existentes

  Background:
    Given un archivo JSON temporal para tests
    And un UserRepository configurado con el archivo

  # Cubre: UserRepository 76% - init() con USERS_FILE env variable
  @critical
  Scenario: Inicialización con variable de entorno USERS_FILE
    Given la variable de entorno USERS_FILE apunta a un archivo válido
    When se ejecuta initialize()
    Then los usuarios deben cargarse desde USERS_FILE

  # Cubre: UserRepository - init() sin configuración
  @high
  Scenario: Inicialización sin configuración lanza excepción
    Given filePath es null
    And USERS_FILE no está configurado
    When se ejecuta initialize()
    Then debe lanzarse IllegalStateException con mensaje "users.persistence.file no está configurado"

  # Cubre: UserRepository - loadUsers con IDs inválidos
  @high
  Scenario: Carga de usuarios con ID null genera nuevo ID
    Given el archivo JSON contiene un usuario con ID null
    When se ejecuta initialize()
    Then el usuario debe tener un ID generado automáticamente

  # Cubre: UserRepository - loadUsers con ID <= 0
  @high
  Scenario: Carga de usuarios con ID cero o negativo genera nuevo ID
    Given el archivo JSON contiene un usuario con ID 0
    When se ejecuta initialize()
    Then el usuario debe tener un ID positivo generado

  # Cubre: UserRepository - writeToFile con jsonFile null
  @high
  Scenario: Escribir archivo cuando jsonFile es null
    Given jsonFile es null
    And filePath está configurado correctamente
    When se ejecuta writeToFile()
    Then el archivo debe crearse en la ruta configurada

  # Cubre: UserRepository - writeToFile con filePath null
  @medium
  Scenario: Escribir archivo sin configuración lanza excepción
    Given jsonFile es null
    And filePath es null
    When se ejecuta writeToFile()
    Then debe lanzarse IllegalStateException

  # Cubre: UserRepository - save con ID existente
  @medium
  Scenario: Guardar usuario con ID ya asignado preserva el ID
    Given no hay usuarios en el repositorio
    When se guarda un usuario con ID 100
    Then el usuario debe guardarse con ID 100
    And nextId debe ser al menos 101

  # Cubre: UserRepository - partialUpdate usuario inexistente
  @medium
  Scenario: Actualización parcial de usuario inexistente retorna null
    Given ningún usuario con ID 999 existe
    When se ejecuta partialUpdate(999, {"name": "Test"})
    Then el resultado debe ser null
```

#### Feature: UsuarioMapper Transformations

```gherkin
Feature: Transformaciones de UsuarioMapper
  Como capa de presentación
  Quiero convertir entre DTOs y entidades de dominio
  Para separar las capas de la aplicación

  # Cubre: UsuarioMapper 51% - toUser con request null
  @high
  Scenario: Convertir CreateUsuarioRequest null a User
    When se invoca toUser(null)
    Then el resultado debe ser null

  # Cubre: UsuarioMapper - toUser con valores válidos
  @high
  Scenario: Convertir CreateUsuarioRequest válido a User
    Given un CreateUsuarioRequest con nombre "Juan", email "juan@test.com", contraseña "Pass123!"
    When se invoca toUser(request)
    Then el User debe tener name "Juan"
    And el User debe tener mail "juan@test.com"
    And el User debe tener password "Pass123!"
    And el User debe tener active true

  # Cubre: UsuarioMapper - toUserUpdate con request null
  @high
  Scenario: Convertir UpdateUsuarioRequest null retorna usuario existente
    Given un User existente con name "Original"
    When se invoca toUserUpdate(null, existente)
    Then el resultado debe ser el usuario existente sin cambios

  # Cubre: UsuarioMapper - toUserUpdate parcial
  @high
  Scenario: Convertir UpdateUsuarioRequest parcial actualiza solo campos proporcionados
    Given un User existente con name "Original", email "original@test.com"
    And un UpdateUsuarioRequest con solo nombre "Nuevo"
    When se invoca toUserUpdate(request, existente)
    Then el User debe tener name "Nuevo"
    And el User debe mantener email "original@test.com"

  # Cubre: UsuarioMapper - toUserUpdate con todos los campos
  @medium
  Scenario: Convertir UpdateUsuarioRequest completo actualiza todos los campos
    Given un User existente
    And un UpdateUsuarioRequest con nombre, email, contraseña y activo
    When se invoca toUserUpdate(request, existente)
    Then todos los campos del User deben actualizarse

  # Cubre: UsuarioMapper - toResponse con user null
  @high
  Scenario: Convertir User null a UsuarioResponse
    When se invoca toResponse(null)
    Then el resultado debe ser null
```

#### Feature: StrictValidationStrategy Validation

```gherkin
Feature: Validación estricta de usuarios
  Como sistema de seguridad
  Quiero aplicar validaciones rigurosas
  Para cuentas de administrador y usuarios privilegiados

  # Cubre: StrictValidationStrategy 91% - validateName con espacios
  @high
  Scenario: Validar nombre con espacios al inicio o final
    Given un CreateUsuarioRequest con nombre " Juan "
    When se ejecuta validateForCreation
    Then debe lanzarse ValidationException con mensaje "espacios al inicio o final"

  # Cubre: StrictValidationStrategy - validateEmail con dominio temporal
  @high
  Scenario: Validar email con dominio temporal rechazado
    Given un CreateUsuarioRequest con email "user@tempmail.com"
    When se ejecuta validateForCreation
    Then debe lanzarse ValidationException con mensaje "emails temporales"

  # Cubre: StrictValidationStrategy - validateEmail con trash-mail.com
  @medium
  Scenario: Validar email con dominio trash-mail.com rechazado
    Given un CreateUsuarioRequest con email "user@trash-mail.com"
    When se ejecuta validateForCreation
    Then debe lanzarse ValidationException con mensaje "emails temporales"

  # Cubre: StrictValidationStrategy - isCommonPassword con contraseñas comunes
  @high
  Scenario: Validar contraseña común "Password123!"
    Given un CreateUsuarioRequest con contraseña "Password123!"
    When se ejecuta validateForCreation
    Then debe lanzarse ValidationException con mensaje "demasiado común"

  # Cubre: StrictValidationStrategy - isCommonPassword case insensitive
  @medium
  Scenario: Validar contraseña común en mayúsculas
    Given un CreateUsuarioRequest con contraseña "ADMIN123456!"
    When se ejecuta validateForCreation
    Then debe lanzarse ValidationException con mensaje "demasiado común"

  # Cubre: StrictValidationStrategy - validateForUpdate solo campos presentes
  @medium
  Scenario: Validación de actualización ignora campos null
    Given un UpdateUsuarioRequest con solo nombre válido (otros campos null)
    When se ejecuta validateForUpdate
    Then la validación debe pasar (no valida campos null)
```

#### Feature: LenientValidationStrategy Validation

```gherkin
Feature: Validación leniente de usuarios
  Como sistema de auto-registro
  Quiero aplicar validaciones básicas
  Para facilitar la creación de usuarios estándar

  # Cubre: LenientValidationStrategy 95% - edge cases faltantes
  @medium
  Scenario: Validar nombre con solo espacios
    Given un CreateUsuarioRequest con nombre "   "
    When se ejecuta validateForCreation
    Then debe lanzarse ValidationException con mensaje "vacío"

  # Cubre: LenientValidationStrategy - validateForUpdate todos los campos null
  @medium
  Scenario: Validación de actualización con todos los campos null
    Given un UpdateUsuarioRequest con todos los campos null
    When se ejecuta validateForUpdate
    Then la validación debe pasar (no hay campos que validar)
```

#### Feature: UsuarioService Business Logic

```gherkin
Feature: Lógica de negocio de usuarios
  Como servicio de usuarios
  Quiero orquestar operaciones de usuario
  Para mantener la coherencia del negocio

  Background:
    Given un IUserPersistence mock
    And un ValidationContext mock
    And un UsuarioService configurado

  # Cubre: UsuarioService 94% - obtenerPorIdentificador con ID inválido
  @high
  Scenario: Obtener usuario por identificador no numérico ni email
    When se invoca obtenerPorIdentificador("abc123")
    Then el resultado debe ser Optional.empty()

  # Cubre: UsuarioService - tryParseId con NumberFormatException
  @medium
  Scenario: Parsear identificador con texto mixto
    When se invoca obtenerPorIdentificador("123abc")
    Then el resultado debe ser Optional.empty()

  # Cubre: UsuarioService - validateEmailUniqueness con email duplicado
  @high
  Scenario: Crear usuario con email duplicado lanza excepción
    Given un usuario existente con email "existing@test.com"
    And un CreateUsuarioRequest con email "existing@test.com"
    When se invoca crear(request)
    Then debe lanzarse UsuarioYaExisteException

  # Cubre: UsuarioService - actualizar email a uno ya existente
  @high
  Scenario: Actualizar usuario con email que ya pertenece a otro
    Given un usuario con ID 1 y email "user1@test.com"
    And un usuario con ID 2 y email "user2@test.com"
    And un UpdateUsuarioRequest para usuario 1 con nuevo email "user2@test.com"
    When se invoca actualizar(1, request)
    Then debe lanzarse UsuarioYaExisteException
```

#### Feature: UserServiceProducer Messaging

```gherkin
Feature: Productor de mensajes RabbitMQ
  Como sistema de mensajería
  Quiero enviar eventos de usuario
  Para comunicación asíncrona entre servicios

  Background:
    Given un RabbitTemplate mock

  # Cubre: UserServiceProducer 20% - envío de mensajes
  @critical
  Scenario: Enviar mensaje de usuario creado
    Given un usuario recién creado con ID 1
    When se invoca sendUserCreatedMessage(usuario)
    Then RabbitTemplate.convertAndSend debe ser invocado
    And el mensaje debe contener los datos del usuario

  # Cubre: UserServiceProducer - manejo de errores
  @high
  Scenario: Error al enviar mensaje no propaga excepción
    Given RabbitTemplate lanza AmqpException
    When se invoca sendUserCreatedMessage(usuario)
    Then la excepción debe ser logueada
    And no debe propagarse la excepción
```

#### Feature: Exception Constructors

```gherkin
Feature: Constructores de excepciones personalizadas
  Como sistema de manejo de errores
  Quiero excepciones con mensajes descriptivos
  Para facilitar el debugging

  # Cubre: UsuarioNotFoundException 44% - constructor alternativo
  @medium
  Scenario: Crear UsuarioNotFoundException con mensaje
    When se crea new UsuarioNotFoundException("Usuario 1 no encontrado")
    Then el mensaje debe ser "Usuario 1 no encontrado"

  # Cubre: UsuarioYaExisteException 44% - constructor alternativo  
  @medium
  Scenario: Crear UsuarioYaExisteException con mensaje
    When se crea new UsuarioYaExisteException("Email ya registrado")
    Then el mensaje debe ser "Email ya registrado"
```

### 6.2 Escenarios de Pruebas de Integración

#### Feature: UserJpaPersistence Integration

```gherkin
Feature: Integración de persistencia JPA con PostgreSQL
  Como capa de persistencia
  Quiero operar con PostgreSQL real
  Para validar el mapeo ORM y queries

  Background:
    Given un contexto @DataJpaTest con H2 en memoria
    And el schema de usuarios creado

  # Cubre: UserJpaPersistence↔UserJpaRepository integración
  @critical
  Scenario: Guardar y recuperar usuario completo
    Given un User con todos los campos válidos
    When se invoca save(user)
    Then el usuario debe persistirse en la base de datos
    When se invoca findById(savedId)
    Then el usuario recuperado debe tener todos los campos correctos

  # Cubre: UserJpaPersistence - findByMailIgnoreCase query
  @high
  Scenario: Buscar usuario por email case-insensitive
    Given un usuario guardado con email "Test@Example.COM"
    When se invoca findByEmail("test@example.com")
    Then el usuario debe ser encontrado

  # Cubre: UserJpaPersistence - findByActiveTrue query
  @high
  Scenario: Obtener solo usuarios activos excluye inactivos
    Given un usuario activo y un usuario inactivo guardados
    When se invoca findAllActive()
    Then solo el usuario activo debe retornarse
```

#### Feature: PersistenceConfig Integration

```gherkin
Feature: Configuración de persistencia condicional
  Como sistema de configuración
  Quiero configurar la persistencia según el entorno
  Para soportar JSON en desarrollo y PostgreSQL en producción

  # Cubre: PersistenceConfig 68% - bean condicional
  @high
  Scenario: Crear bean de persistencia JSON cuando JPA no está disponible
    Given el perfil "test-no-jpa" activo
    And UserJpaRepository NO disponible
    When el contexto de Spring se inicializa
    Then debe crearse un UserRepository bean
    And debe envolverse en CachedUserPersistenceDecorator

  # Cubre: PersistenceConfig - con JPA disponible
  @high
  Scenario: Usar JPA persistence cuando JpaRepository disponible
    Given el perfil "default" activo
    And UserJpaRepository disponible
    When el contexto de Spring se inicializa
    Then UserJpaPersistence debe ser el bean primario

  # Cubre: UserPersistenceFactory - selección de estrategia
  @medium
  Scenario: Factory selecciona implementación correcta
    Given múltiples implementaciones de IUserPersistence disponibles
    When se solicita el bean IUserPersistence
    Then debe retornarse el decorador de caché envolviendo la implementación primaria
```

#### Feature: UserRepository JSON File Integration

```gherkin
Feature: Integración de persistencia JSON con sistema de archivos
  Como sistema legacy
  Quiero persistir en archivos JSON reales
  Para validar lectura/escritura en disco

  Background:
    Given un directorio temporal para tests
    And un archivo users.json vacío

  # Cubre: UserRepository - writeToFile crea directorio padre
  @high
  Scenario: Escribir archivo crea directorios padre si no existen
    Given filePath apunta a "/tmp/test/nested/users.json"
    And el directorio "/tmp/test/nested" NO existe
    When se ejecuta writeToFile()
    Then el directorio debe ser creado
    And el archivo debe ser escrito

  # Cubre: UserRepository - init() con archivo corrupto
  @medium
  Scenario: Inicialización con JSON corrupto crea nuevo archivo
    Given un archivo users.json con contenido inválido "{invalid"
    When se ejecuta initialize()
    Then el error debe ser logueado
    And un nuevo archivo vacío debe ser creado

  # Cubre: UserRepository - persistencia completa CRUD
  @high
  Scenario: Ciclo completo de operaciones CRUD persiste correctamente
    When se crea un usuario
    And se actualiza el usuario
    And se reinicializa el repositorio
    Then el usuario debe mantener los cambios actualizados
```

---

## 7. Priorización por Cobertura (JaCoCo-driven)

| Prioridad | Clase | Cobertura Actual | Instrucciones Perdidas | Tipo de Prueba | Escenarios |
|-----------|-------|-----------------|----------------------|----------------|------------|
| 🔴 CRÍTICO | `CachedUserPersistenceDecorator` | 50% | 150 | Unitaria | 9 |
| 🔴 CRÍTICO | `UserJpaPersistence` | 60% | 114 | Unitaria + Integración | 7 |
| 🟠 ALTO | `UserRepository` | 76% | 107 | Unitaria + Integración | 8 |
| 🟠 ALTO | `UsuarioMapper` | 51% | 41 | Unitaria | 6 |
| 🟠 ALTO | `UserServiceProducer` | 20% | 12 | Unitaria | 2 |
| 🟡 MEDIO | `StrictValidationStrategy` | 91% | 23 | Unitaria | 6 |
| 🟡 MEDIO | `PersistenceConfig` | 68% | 21 | Integración | 3 |
| 🟡 MEDIO | `UsuarioService` | 94% | 17 | Unitaria | 4 |
| 🟢 BAJO | `LenientValidationStrategy` | 95% | 5 | Unitaria | 2 |
| 🟢 BAJO | `UsuarioNotFoundException` | ~44% | 5 | Unitaria | 1 |
| 🟢 BAJO | `UsuarioYaExisteException` | ~44% | 5 | Unitaria | 1 |

### Ganancia Estimada de Cobertura por Grupo de Tests

| Grupo de Tests | Clases Afectadas | Ganancia Estimada |
|----------------|------------------|-------------------|
| CachedUserPersistenceDecorator tests | `CachedUserPersistenceDecorator` | +5-6% global |
| UserJpaPersistence tests | `UserJpaPersistence` | +4-5% global |
| UserRepository tests | `UserRepository` | +3-4% global |
| UsuarioMapper tests | `UsuarioMapper` | +1-2% global |
| UserServiceProducer tests | `UserServiceProducer` | +0.5% global |
| Validation tests (edge cases) | `StrictValidationStrategy`, `LenientValidationStrategy` | +1% global |

**Total estimado:** +14-18% cobertura adicional → Meta 90%+ alcanzable

---

## 8. Gestión de Riesgos

### 8.1 Registro de Riesgos

| ID | Riesgo | Probabilidad | Impacto | Severidad | Mitigación |
|----|--------|-------------|---------|-----------|------------|
| R01 | Tests pasan local pero fallan en CI por variables de entorno faltantes (`USERS_FILE`) | Media | Alto | 🔴 Alto | Usar `@TestPropertySource` con `application-test.properties` |
| R02 | ConcurrentHashMap en caché genera race conditions en tests paralelos | Media | Alto | 🔴 Alto | Ejecutar tests de `CachedUserPersistenceDecorator` en modo single-thread |
| R03 | Archivo JSON se corrompe durante tests concurrentes | Alta | Medio | 🟡 Medio | Usar `@TempDir` de JUnit 5 para aislamiento de archivos |
| R04 | Tests de integración JPA fallan por schema incompatible | Media | Alto | 🔴 Alto | Usar H2 con modo PostgreSQL (`MODE=PostgreSQL`) |
| R05 | Validadores con regex complejos timeout en edge cases | Baja | Medio | 🟡 Medio | Agregar timeout a tests de validación con `@Timeout(5)` |
| R06 | Meta de cobertura no alcanzada por código inalcanzable | Media | Medio | 🟡 Medio | Documentar código muerto, considerar refactoring |
| R07 | `UserServiceProducer` no testeado correctamente sin RabbitMQ | Alta | Alto | 🔴 Alto | Usar `@MockBean RabbitTemplate` para tests unitarios |
| R08 | Efectos secundarios brownfield de `writeToFile()` corrompen datos | Media | Alto | 🔴 Alto | Aislar con `@TempDir`, nunca usar archivo de producción |

### 8.2 Estrategia de Respuesta a Riesgos

| ID | Estrategia |
|----|------------|
| **R01** | Agregar `src/test/resources/application-test.properties` con `users.persistence.file=${java.io.tmpdir}/test-users.json` |
| **R02** | Anotar tests de caché con `@Execution(ExecutionMode.SAME_THREAD)` |
| **R03** | Usar `@TempDir Path tempDir` en cada test y crear archivo fresco |
| **R04** | Configurar H2 en `application-test.properties` con `spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL` |
| **R05** | Agregar `@Timeout(value = 5, unit = TimeUnit.SECONDS)` a tests de regex |
| **R06** | Marcar código inalcanzable con `// NOSONAR` o refactorizar para eliminar |
| **R07** | Crear `UserServiceProducerTest` con `@MockBean RabbitTemplate` |
| **R08** | Nunca usar ruta de archivo real en tests; siempre `@TempDir` |

### 8.3 Umbrales de Riesgo por Cobertura

| Rango de Cobertura | Estado | Acción |
|--------------------|--------|--------|
| < 50% | 🔴 Pipeline bloqueado | Acción inmediata requerida - no merge |
| 50% - 69% | 🟠 Warning | Sprint debt - debe mejorar antes de release |
| 70% - 84% | 🟡 Aceptable | Mantener, mejorar incrementalmente |
| 85%+ | 🟢 Óptimo | Mantener, focus en branches |

---

## 9. Calendario de Pruebas

| Fase | Actividad | Clases Target | Esfuerzo Estimado | Responsable |
|------|-----------|---------------|-------------------|-------------|
| **Fase 1** | Tests unitarios `CachedUserPersistenceDecorator` | `CachedUserPersistenceDecorator` | 3-4h | Dev/QA |
| **Fase 2** | Tests unitarios + integración `UserJpaPersistence` | `UserJpaPersistence` | 3-4h | Dev/QA |
| **Fase 3** | Tests unitarios `UserRepository` (JSON) | `UserRepository` | 2-3h | Dev/QA |
| **Fase 4** | Tests unitarios `UsuarioMapper` | `UsuarioMapper` | 1-2h | Dev/QA |
| **Fase 5** | Tests unitarios `UserServiceProducer` | `UserServiceProducer` | 1h | Dev/QA |
| **Fase 6** | Tests edge cases `StrictValidationStrategy` | `StrictValidationStrategy` | 1h | Dev/QA |
| **Fase 7** | Tests integración `PersistenceConfig` | `PersistenceConfig`, `UserPersistenceFactory` | 2h | Dev/QA |
| **Fase 8** | Tests `UsuarioService` branches faltantes | `UsuarioService` | 1h | Dev/QA |
| **Fase 9** | Ejecución suite completa + reporte cobertura | Todos | 30min | Dev/QA |
| **Fase 10** | Análisis de brechas y ajustes finales | Todos | 1-2h | QA |

**Esfuerzo total estimado:** 16-20 horas

---

## 10. Herramientas y Entorno

| Herramienta | Propósito | Versión |
|-------------|-----------|---------|
| **JUnit 5** | Framework de pruebas | 5.x (via Spring Boot 3.x) |
| **Mockito** | Framework de mocking | Latest (via Spring Boot) |
| **MockMvc** | Testing capa HTTP | Via `@WebMvcTest` |
| **@DataJpaTest** | Testing capa repositorio | Via Spring Boot Test |
| **H2 Database** | Base de datos en memoria para tests | 2.x |
| **JaCoCo** | Reporte de cobertura | 0.8.11 |
| **AssertJ** | Aserciones fluentes | Latest |
| **@TempDir** | Aislamiento de archivos | JUnit 5 |
| **Testcontainers** | PostgreSQL/RabbitMQ reales (opcional) | 1.19.x |

### Configuración de Entorno de Tests

**application-test.properties:**
```properties
# Database - H2 con modo PostgreSQL
spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
spring.jpa.hibernate.ddl-auto=create-drop

# Archivo JSON para tests
users.persistence.file=${java.io.tmpdir}/test-users.json

# RabbitMQ deshabilitado en tests
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672

# Logging reducido
logging.level.root=WARN
logging.level.com.example.usuarioservice=DEBUG
```

---

## 11. Trazabilidad Escenarios → Brechas de Cobertura

| Escenario | Brecha de Cobertura | Instrucciones Recuperables |
|-----------|---------------------|---------------------------|
| `findById_cachedUser_returnsFromCache` | CachedUserPersistenceDecorator L63-72 | ~15 |
| `findByEmail_nullEmail_returnsNull` | CachedUserPersistenceDecorator L74-75 | ~8 |
| `update_invalidatesOldAndNewCache` | CachedUserPersistenceDecorator L103-120 | ~25 |
| `partialUpdate_userNotInCache_noInvalidation` | CachedUserPersistenceDecorator L122-140 | ~20 |
| `deleteById_invalidatesCache` | CachedUserPersistenceDecorator L156-170 | ~15 |
| `update_existingUser_updatesAllFields` | UserJpaPersistence L71-82 | ~30 |
| `partialUpdate_activeAsBoolean_parsesCorrectly` | UserJpaPersistence L117-125 | ~15 |
| `toUser_nullRequest_returnsNull` | UsuarioMapper L19-21 | ~8 |
| `toUserUpdate_partialFields_updatesOnlyProvided` | UsuarioMapper L35-55 | ~20 |
| `sendUserCreatedMessage` | UserServiceProducer L20-35 | ~12 |

---

## 12. Criterios de Aceptación del Plan

| Criterio | Descripción | Métrica |
|----------|-------------|---------|
| **Cobertura Mínima** | Alcanzar meta de cobertura | ≥90% instrucciones, ≥80% branches |
| **Tests Ejecutables** | Todos los escenarios Gherkin implementados como tests JUnit | 100% implementación |
| **Sin Regresiones** | Tests existentes continúan pasando | 0 tests rotos |
| **CI/CD Verde** | Pipeline completo sin errores | Build exitoso |
| **Documentación** | Cada test tiene comentario de trazabilidad | 100% documentados |

---

*Documento generado: 1 de marzo de 2026*
*Próxima revisión: Post-implementación de Fase 1*
