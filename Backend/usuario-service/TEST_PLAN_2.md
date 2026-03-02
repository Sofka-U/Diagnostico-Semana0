# TEST_PLAN.md - Usuario Service

## 1. Overview

| Campo | Valor |
|-------|-------|
| **Versión del documento** | 1.2 |
| **Fecha** | 01 de marzo de 2026 |
| **Microservicio** | `usuario-service` |
| **Cobertura actual (JaCoCo)** | 82% instrucciones, 65% branches (informe local) |
| **Meta de cobertura** | ≥70% global (crítico: ≥80% instrucciones, ≥60% branches) |

### Resumen Ejecutivo

El servicio `usuario-service` gestiona usuarios con persistencia PostgreSQL/JSON y comunicación asíncrona vía RabbitMQ. Tras la iteración anterior, la mayoría de paquetes (`controller`, `service`, `dto`, `validation`, `exception`, `messaging`) alcanzaron cobertura alta. Las brechas restantes se concentran en tres clases específicas detectadas por JaCoCo:

| Clase | Cobertura actual | Instrucciones missed | Prioridad |
|-------|-----------------|----------------------|-----------|
| `CachedUserPersistenceDecorator` | ~51% | 150 | 🔴 CRÍTICO |
| `UserJpaPersistence` | ~60.7% | 114 | 🔴 CRÍTICO |
| `UserServiceProducer` | ~20% | 12 | 🟡 ALTO |

> **Nota:** Los demás paquetes (`mapper`, `controller`, `service`, `dto`, `validation`, `exception`) fueron cubiertos en la iteración anterior y **no son prioritarios** en este ciclo.

### Riesgos Brownfield Identificados

- Dependencias de RabbitMQ sin mocks adecuados en `UserServiceProducer`.
- `CachedUserPersistenceDecorator` con lógica de concurrencia difícil de testear.
- `UserJpaPersistence` con edge-cases de `parseBoolean` y `partialUpdate` no cubiertos.

---

## 2. Alcance

### 2.1 En alcance (prioridad)

- **`persistence`:**
  - `CachedUserPersistenceDecorator` — comportamientos de cache: store/retrieve, delegación, concurrencia.
  - `UserJpaPersistence` — `partialUpdate`, `update`, `deleteById`, parseBoolean edge-cases.
  - `UserRepository` (JSON persistence) — `init()`, `writeToFile()`, `loadUsers()`, `partialUpdate()`.

- **`messaging`:**
  - `UserServiceProducer` — verificar `convertAndSend` con exchange/routingKey correctos y manejo de excepciones.

### 2.2 Fuera de alcance (por ahora)

- `controller`, `service`, `mapper`, `dto`, `validation`, `exception` — cobertura alta alcanzada en iteración anterior.

---

## 3. Niveles de Prueba y Herramientas

| Herramienta | Propósito | Configuración |
|-------------|-----------|---------------|
| JUnit 5 | Framework de pruebas | Via Spring Boot Starter Test |
| Mockito | Framework de mocking | `@ExtendWith(MockitoExtension.class)` |
| `@DataJpaTest` | Tests de integración JPA | H2 in-memory, `@Transactional` |
| H2 Database | BD in-memory para tests | `spring.datasource.url=jdbc:h2:mem:testdb` |
| JaCoCo | Reporte de cobertura | Plugin Maven configurado |
| AssertJ | Assertions fluidas | Opcional, mejora legibilidad |

### Configuración de Perfil de Test

```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
user:
  service:
    timeout: 100
```

---

## 4. Observaciones JaCoCo (Detalles Críticos)

Al generar el informe JaCoCo local se identificaron tres clases con brechas de cobertura concretas que requieren intervención prioritaria. Se listan las métricas extraídas del informe y las acciones propuestas:

- **`com.example.usuarioservice.persistence.CachedUserPersistenceDecorator`** — Cobertura actual ~51% (150 instrucciones missed / 156 covered). Acciones propuestas:
  - Operaciones de cache: store/retrieve, comportamiento ante keys inexistentes.
  - Delegación correcta hacia la persistencia subyacente en aciertos/misses.
  - Comportamiento en escenarios concurrentes simples (uso de `CountDownLatch`) y eviction si aplica.

- **`com.example.usuarioservice.persistence.UserJpaPersistence`** — Cobertura actual ~60.7% (114 missed / 176 covered). Acciones propuestas:
  - `partialUpdate` con varios tipos de payloads (strings, booleans, nulos).
  - `update`, `deleteById` y casos de error (entidad no encontrada).
  - Edge-cases de `parseBoolean`: valores nulos y formatos inesperados.

- **`com.example.usuarioservice.messaging.UserServiceProducer`** — Cobertura ~20% (12 missed / 3 covered). Acciones propuestas:
  - `convertAndSend` invocado con exchange/routingKey correctos y payload `UserRequest`.
  - Manejo de excepciones al enviar mensajes (reintentos o wrapping según implementación).

> **Nota:** Los demás paquetes y clases (`mapper`, `UserRepository`, `controller`, manejadores de excepción) **fueron cubiertos** en la iteración anterior y se marcan como cubiertos para esta fase. Las tres clases anteriores son las vulnerabilidades más importantes a mitigar en esta iteración.

---

## 5. Priorización JaCoCo-driven

| Prioridad | Paquete/Clase | Cobertura | Instrucciones missed | Ganancia estimada |
|-----------|---------------|-----------|----------------------|-------------------|
| 🔴 CRÍTICO | `CachedUserPersistenceDecorator` | ~51% | 150 | +10% |
| 🔴 CRÍTICO | `UserJpaPersistence` | ~60.7% | 114 | +8% |
| 🟡 ALTO | `UserServiceProducer` | ~20% | 12 | +1% |

**Ganancia total estimada:** +12–18% instrucciones → Cobertura proyectada: ≥80%

---

## 6. Técnicas de Diseño y Escenarios Mapeados

- **Partición de Equivalencia:** cache hit/miss, payloads de `partialUpdate` con keys válidas/inválidas.
- **Valores Límite:** inputs `null`, strings vacíos, `id` inexistente, valores booleanos en distintos formatos.
- **Tabla de Decisión:** `parseBoolean` con `"true"`, `"false"`, `"True"`, `""` y `null`.
- **Concurrencia controlada:** `CountDownLatch` con timeout < 500ms para tests de cache.

---

## 7. Escenarios Gherkin por Clase

### 7.1 CachedUserPersistenceDecorator — Unitario (`@critical`)

- **Tipo:** Unitario
- **Mocks:** persistencia subyacente con Mockito (`UserRepository` o `UserJpaPersistence`)
- **Estimación:** 3–4 tests, 2–3h

```gherkin
Feature: CachedUserPersistenceDecorator - Cache behaviour

  Scenario: store guarda en cache y no invoca persistencia en hit
    Given un CachedUserPersistenceDecorator con persistencia subyacente mockeada
    When se invoca cache.store(user)
    Then el cache contiene el user
    And la persistencia subyacente no es invocada en caso de hit

  Scenario: retrieve devuelve valor del cache si existe
    Given un CachedUserPersistenceDecorator con user precargado en cache
    When se invoca cache.get(userId)
    Then se retorna el User desde cache sin invocar la persistencia

  Scenario: miss delega a la persistencia subyacente y actualiza cache
    Given un CachedUserPersistenceDecorator con cache vacío y persistencia que retorna user
    When se invoca cache.get(userId)
    Then la persistencia subyacente es invocada
    And el cache se actualiza con el user retornado

  Scenario: concurrencia simple no corrompe cache
    Given múltiples hilos consultando/almacenando el mismo user
    When se ejecutan operaciones concurrentes controladas (CountDownLatch)
    Then no debe haber excepciones y la última escritura persiste
```

**Notas de implementación:**
- Usar `@ExtendWith(MockitoExtension.class)` y `when(...).thenReturn(...)`.
- Usar `CountDownLatch` con timeout corto (< 500ms) para tests de concurrencia.

---

### 7.2 UserJpaPersistence — Integración (`@critical`)

- **Tipo:** `@DataJpaTest` (H2 in-memory)
- **Mocks:** ninguno; Spring provee repositorio real.
- **Estimación:** 4–6 tests, 3–4h

```gherkin
Feature: UserJpaPersistence - JPA operations

  Scenario: partialUpdate aplica solo campos presentes y persiste cambios
    Given una entidad User en la BD con id existente
    When se invoca partialUpdate(id, {"name":"Nuevo","active":"false"})
    Then el registro en BD tiene name="Nuevo" y active=false

  Scenario: partialUpdate con valores nulos no sobreescribe campos no provistos
    Given una entidad User con name="A" y mail="a@x.com"
    When se invoca partialUpdate(id, {"name":null})
    Then el name en BD permanece como "A"

  Scenario: deleteById elimina la entidad (o soft-delete si aplica)
    Given una entidad User existente
    When se invoca deleteById(id)
    Then la entidad no debe encontrarse via findById

  Scenario: parseBoolean y formatos inesperados manejados sin lanzar excepción no controlada
    Given payloads con "true", "false", "True", "" y null
    When se invoca partialUpdate con dichos valores
    Then los valores booleanos se interpretan razonablemente o se reporta error controlado
```

**Notas de implementación:**
- Anotar con `@DataJpaTest` y `@Transactional`.
- Preferir H2; usar `TestEntityManager` o repositorio real para verificar cambios.
- Usar `@AutoConfigureTestDatabase(replace = Replace.NONE)` solo si se requiere control avanzado.

---

### 7.3 UserServiceProducer — Unitario (`@high`)

- **Tipo:** Unitario
- **Mocks:** `RabbitTemplate` con `@Mock` o `@MockBean`
- **Estimación:** 2–3 tests, 1–2h

```gherkin
Feature: UserServiceProducer - send requests

  Scenario: requestUserInfo invoca rabbitTemplate.convertAndSend con parámetros correctos
    Given un UserServiceProducer con RabbitTemplate mockeado
    When se invoca producer.requestUserInfo(10)
    Then rabbitTemplate.convertAndSend debe ser invocado con exchange "user-exchange" y routingKey "user.request"
    And el payload debe ser UserRequest con userId=10

  Scenario: excepción al enviar es manejada según implementación
    Given un RabbitTemplate que lanza RuntimeException en convertAndSend
    When se invoca producer.requestUserInfo(10)
    Then el método no debe propagar excepciones no controladas (o debe reintentar según política)
```

**Notas de implementación:**
- Usar `verify(rabbitTemplate).convertAndSend(eq(exchange), eq(routingKey), any(UserRequest.class))`.
- Para el path de excepción: `doThrow(new RuntimeException("fail")).when(rabbitTemplate).convertAndSend(...)`.

---

## 8. Plan de Ejecución

| Fase | Actividad | Esfuerzo Est. | Prioridad |
|------|-----------|---------------|-----------|
| **Fase 1** | Tests unitarios: `CachedUserPersistenceDecorator` | 2–3h | 🔴 Crítica |
| **Fase 2** | Tests integración: `UserJpaPersistence` (`@DataJpaTest`) | 3–4h | 🔴 Crítica |
| **Fase 3** | Tests unitarios: `UserServiceProducer` | 1–2h | 🟡 Alta |
| **Fase 4** | Ejecución completa + análisis JaCoCo (`mvn test jacoco:report`) | 30min | — |

**Esfuerzo total estimado:** 7–10 horas

---

## 9. Gestión de Riesgos

| ID | Riesgo | Probabilidad | Impacto | Severidad | Mitigación |
|----|--------|--------------|---------|-----------|------------|
| R01 | Tests fallan en CI por RabbitMQ | Alta | Alto | 🔴 Alto | `spring.autoconfigure.exclude=RabbitAutoConfiguration` |
| R02 | Contaminación de estado de BD entre tests | Media | Alto | 🔴 Alto | `@Transactional` + `@Rollback` + H2 in-memory |
| R03 | Meta de cobertura no alcanzada | Media | Alto | 🔴 Alto | Priorizar escenarios CRÍTICOS primero |
| R04 | Mocks incorrectos no detectan bugs reales | Media | Alto | 🔴 Alto | Combinar tests unitarios con integración |
| R05 | Concurrencia en `CachedUserPersistenceDecorator` difícil de testear | Alta | Medio | 🟡 Medio | `CountDownLatch` con timeouts < 500ms |

### Umbrales de Riesgo por Cobertura

| Umbral | Acción |
|--------|--------|
| < 50% | 🔴 Pipeline bloqueado — release no permitido |
| 50%–69% | 🟡 Advertencia — requiere aprobación manual |
| ≥80% | ✅ Aceptable — CI/CD continúa |

---

## 10. Trazabilidad de Escenarios

| ID Escenario | Clase / Brecha | Técnica Aplicada | Tipo Test |
|--------------|----------------|------------------|-----------|
| CACHE-01 | `CachedUserPersistenceDecorator` — store/hit | Partición equivalencia | Unitaria |
| CACHE-02 | `CachedUserPersistenceDecorator` — retrieve hit | Partición equivalencia | Unitaria |
| CACHE-03 | `CachedUserPersistenceDecorator` — miss/delegación | Partición equivalencia | Unitaria |
| CACHE-04 | `CachedUserPersistenceDecorator` — concurrencia | Concurrencia controlada | Unitaria |
| JPA-01 | `UserJpaPersistence` — partialUpdate con cambios | Partición equivalencia | Integración |
| JPA-02 | `UserJpaPersistence` — partialUpdate sin sobreescribir nulos | Valor límite | Integración |
| JPA-03 | `UserJpaPersistence` — deleteById | Partición equivalencia | Integración |
| JPA-04 | `UserJpaPersistence` — parseBoolean edge-cases | Tabla de decisión | Integración |
| MSG-01 | `UserServiceProducer` — convertAndSend correcto | Partición equivalencia | Unitaria |
| MSG-02 | `UserServiceProducer` — manejo de excepción | Partición inválida | Unitaria |

---

## 11. Criterios de Éxito

| Métrica | Valor Objetivo |
|---------|----------------|
| Cobertura de instrucciones | ≥80% |
| Cobertura de branches | ≥60% |
| Tests pasando | 100% |
| Tiempo de ejecución total | < 60 segundos |
| Escenarios CRÍTICOS implementados | 100% |

> **Regla del request:** No añadir más tests si la cobertura supera el 90% en un paquete.

---

**Fin del documento TEST_PLAN.md — v1.2**