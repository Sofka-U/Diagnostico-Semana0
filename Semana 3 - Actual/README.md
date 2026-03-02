Resumen de la Semana 3
=====================

**Propósito:** Resumen de actividades, decisiones arquitectónicas, refactors, dockerización y estrategia de calidad continua ejecutadas durante la semana 3.

**Ubicación de fuentes y evidencia:** este README enlaza los documentos y artefactos principales generados durante la semana.

**Contenido rápido:**
- **Arquitectura:** ver [Semana 3 - Actual/ARCHITECTURE.md](Semana%203%20-%20Actual/ARCHITECTURE.md)
- **Informe previo / evidencia de cambios:** ver [Semana 3 - Actual/API_AUDIT_REPORT_BEFORE_CHANGES.md](Semana%203%20-%20Actual/API_AUDIT_REPORT_BEFORE_CHANGES.md)
- **Pruebas y planos:** ver los test plans en [Backend/pedido-service/TEST_PLAN_v.3.0 - pedido-service.md](Backend/pedido-service/TEST_PLAN_v.3.0%20-%20pedido-service.md) y [Backend/usuario-service/TEST_PLAN_v.3.0 - usuario-service.md](Backend/usuario-service/TEST_PLAN_v.3.0%20-%20usuario-service.md)
- **User stories (Semana 3):** [Semana 3 - Actual/USER_STORIES](Semana%203%20-%20Actual/USER_STORIES)

**Estructura del README**
- **Arquitectura**
- **Cambios y refactors**
- **Dockerización**
- **Estrategia de Calidad Continua (QA & DEV)**

**Arquitectura**
----------------

Resumen ejecutivo y decisiones principales:
- Revisión y recomendaciones completas en [Semana 3 - Actual/ARCHITECTURE.md](Semana%203%20-%20Actual/ARCHITECTURE.md).
- Se documentan las convenciones de endpoints, uso de verbos HTTP y códigos de estado (por ejemplo: `POST /orders` → `201 Created` con `Location`, `DELETE` → `204 No Content`, manejo de `404` para recursos inexistentes).
- Debate arquitectónico y recomendaciones sobre separación de responsabilidades, inyección por constructor y límites del refactor están en el mismo archivo.

**Cambios y refactors**
-----------------------

- El estado anterior y las recomendaciones se encuentran en [Semana 3 - Actual/API_AUDIT_REPORT_BEFORE_CHANGES.md](Semana%203%20-%20Actual/API_AUDIT_REPORT_BEFORE_CHANGES.md).
- Se registraron propuestas y acciones de refactor en varias ubicaciones del repo (guías y checkpoints de refactor en `Backend/usuario-service/README_AUDIT.md` y documentos TDD/REFACTOR en `Semana 2/USER_STORIES_DONE`).

- Para `pedido-service` hay una carpeta con auditorías previas a las pruebas en [Backend/pedido-service/Audits before test](Backend/pedido-service/Audits%20before%20test) que contiene `API_AUDIT_REPORT.md`, `TECHNICAL-AUDIT-REPORT.md` y `TECHNICAL-AUDIT-REPORT-v2.0.md`. Estas auditorías documentan el estado previo del servicio y justifican los cambios y refactors aplicados antes de ejecutar los tests y los cambios en los endpoints de la API.

**Dockerización**
-----------------

- Dockerfiles principales:
  - [Backend/usuario-service/Dockerfile](Backend/usuario-service/Dockerfile)
  - [Backend/pedido-service/Dockerfile](Backend/pedido-service/Dockerfile)
  - [Frontend/Dockerfile](Frontend/Dockerfile)
- Orquestación completa y configuración de servicios: [Backend/docker-compose.yml](Backend/docker-compose.yml)

Breve explicación de volúmenes y bind mounts utilizados en `docker-compose.yml`:
- **Named volumes:** se usan para persistencia de PostgreSQL (`postgres_data`) — son gestionados por Docker y persisten datos entre reinicios del contenedor.
- **Bind mounts:** se usan para inicializar la base de datos con scripts desde el repo (ej. `../Backend/usuario-service/init-db/01-init-users.sql` montado en `/docker-entrypoint-initdb.d`) — permiten que archivos locales sean leídos por el contenedor al arrancar.

**Estrategia de Calidad Continua (QA & DEV)**
--------------------------------------------

Resumen y artefactos clave:
- Test plans principales: [Backend/pedido-service/TEST_PLAN_v.3.0 - pedido-service.md](Backend/pedido-service/TEST_PLAN_v.3.0%20-%20pedido-service.md) y [Backend/usuario-service/TEST_PLAN_v.3.0 - usuario-service.md](Backend/usuario-service/TEST_PLAN_v.3.0%20-%20usuario-service.md).
- Las HU asociadas y su trazabilidad están en [Semana 3 - Actual/USER_STORIES](Semana%203%20-%20Actual/USER_STORIES). Estas historias fueron creadas siguiendo principios INVEST y se apoyaron en el agente de requisitos: [.github/agents/irish-requirements-liftup.agent.md](.github/agents/irish-requirements-liftup.agent.md).

Gestión de riesgos (extracto):
- Riesgos identificados en los test plans incluyen: dependencias de RabbitMQ en CI, contaminación de BD entre tests, y riesgo de no alcanzar la meta de cobertura.
- Mitigaciones: aislar RabbitMQ en tests (`spring.autoconfigure.exclude`), usar H2 en perfil `test`, y separar `@WebMvcTest` de `@SpringBootTest`.

Pruebas manuales y evidencia Postman:
- Casos de prueba y evidencias (fotos) están documentadas en [Semana 3 - Actual/proof-api-v2.md](Semana%203%20-%20Actual/proof-api-v2.md). Ese documento contiene la matriz Gherkin y enlaces a las imágenes de ejecución.

Integración continua (CI):
- Los pipelines de GitHub Actions y workflows del repo se encuentran en [.github/workflows](.github/workflows). Se configuraron jobs para tests unitarios e integración en los microservicios y frontend.
- Meta de cobertura para permitir que el pipeline sea exitoso: **≥70%** (medida con JaCoCo). Si la cobertura baja de ese umbral o si pruebas críticas fallan, el pipeline debe marcarse como fallido.
- Ver acciones en GitHub Actions: https://github.com/Sofka-U/Diagnostico-Semana0/actions

**Notas finales y siguientes pasos**
- Para reproducir los entornos locales usar `docker-compose` definido en [Backend/docker-compose.yml](Backend/docker-compose.yml).
- Revisar los test plans y priorizar las pruebas críticas (`GlobalExceptionHandler`, `OrderController`) para alcanzar la meta de cobertura JaCoCo del 70%.
- Si quieres, puedo añadir un índice con enlaces directos a cada HU o generar un changelog más detallado de los commits de refactor.

---
Archivo generado automáticamente: README de la semana 3.
