# AGENTS.md — back-app-movil

Reglas para cualquier agente de IA (Claude Code, Cursor u otro) que trabaje en este repositorio. Léelas completas antes de cada tarea.

## 1. Qué es este proyecto

Backend de una plataforma de reservas de citas para negocios con agenda (barberías, uñas, spa, peluquería, estética) en Colombia.

- **Este repo es solo el backend.** La app móvil (Flutter) vive en otro repositorio (`reservas-app`) y consume el contrato OpenAPI de este backend.
- **Roles:** cliente, invitado, profesional, dueño y administrador de plataforma.
- **Fuente funcional:** los casos de uso CU-00 a CU-13 aprobados. Cada uno se implementa como un cambio de OpenSpec. Sus reglas y escenarios se copian tal cual; **no se inventa comportamiento**.

## 2. Stack

Java 21 · Spring Boot · Maven (wrapper) · PostgreSQL 17 · Flyway · Spring Security · Spring Data JPA · MapStruct · Lombok · springdoc-openapi · JUnit 5 + Mockito · JaCoCo · Spotless (google-java-format).

## 3. Arquitectura

Monolito modular. Un módulo por dominio: `cuentas`, `negocios`, `servicios`, `agenda`, `reservas`, `notificaciones`, `planes`, `administracion` y `parametros`.

Cada módulo es hexagonal:

```
com.reservas.<modulo>/
├── domain/                              ← modelo y reglas, SIN dependencias de Spring
├── application/port/input/              ← casos de uso (interfaces)
├── application/port/output/             ← puertos de salida (repositorios, notificaciones, reloj)
├── application/service/                 ← implementación de los casos de uso
└── infrastructure/adapter/
    ├── input/rest/                      ← controladores y DTOs
    └── output/persistence/              ← entidades JPA, repositorios Spring Data, mappers
```

Lo compartido va en `com.reservas.compartido` (`config`, `error`).

- Un módulo no usa clases internas de otro: se comunican por sus puertos de entrada.
- Los controladores no tienen lógica de negocio: validan, llaman al caso de uso y mapean la respuesta.
- La hora actual se obtiene de un puerto (reloj), nunca con `LocalDateTime.now()` directo, para poder probarla.

## 4. Principios obligatorios

1. **Nada quemado.** Todo plazo, límite, tiempo o cantidad configurable sale del módulo `parametros`, con mínimo y máximo, y con jerarquía **profesional > negocio > plataforma**. Prohibido escribir esos valores como números en el código.
2. **Multi-negocio.** Toda consulta de datos de un negocio se filtra por ese negocio. Nadie ve datos de otro negocio. El administrador de plataforma nunca ve clientes, citas ni tableros de los negocios.
3. **Permisos en el backend.** Cada endpoint valida el rol y el negocio del usuario. Que la app oculte un botón no cuenta como seguridad.
4. **Ley 1581 de 2012.** La autorización de datos es previa, expresa e informada, y se guarda con la versión del texto, la fecha y la hora. **Nunca** hay datos personales (nombre, correo, celular, documento) en logs, mensajes de error ni respuestas que no los necesiten.
5. **Hora de Colombia** (`America/Bogota`) en toda la agenda. **Dinero** en pesos colombianos, enteros (`long`), sin decimales.
6. **Cambio mínimo.** No refactorices lo que no se pidió.
7. **Si hay duda, detente y pregunta.** No supongas reglas, datos ni contratos.

## 5. API

- REST, JSON en **snake_case**.
- Rutas con versión: `/api/v1/...`.
- Todo endpoint documentado con OpenAPI **en español**: descripción, request, respuestas y errores. Es el contrato que usa la app.
- Errores con la respuesta estándar de `compartido.error`: `codigo`, `mensaje` y `campos`. Los códigos y mensajes son constantes, nunca texto suelto en el código.
- Cambiar un contrato existente es **BREAKING**: se marca así en el proposal y se avisa, porque rompe la app.

## 6. Base de datos

- Todo cambio de esquema va en una migración de Flyway: `src/main/resources/db/migration/V<n>__<descripcion>.sql`.
- Nunca se edita una migración que ya se aplicó: se crea una nueva.
- `ddl-auto` siempre en `validate`.
- Toda tabla con datos de un negocio tiene `negocio_id`, con índice.
- Nada de `SELECT *`.

## 7. Pruebas

- Pruebas unitarias con JUnit 5 + Mockito para todo caso de uso, regla de dominio y adaptador.
- Pruebas de integración para cada controlador: camino feliz, errores, acceso denegado por rol y acceso denegado a datos de otro negocio.
- Cobertura mínima: **80% de líneas y de ramas**. El build falla si baja (`JaCoCo check`).
- Cada prueba lleva la etiqueta de su caso de uso: `@Tag("CU-05")`.
- Si el código no cumple un escenario del caso de uso, la prueba falla. **Nunca** se debilita una validación para que pase.
- Un `Then` que no puede fallar no es una prueba: revisa que cada aserción pueda fallar de verdad.

## 8. Flujo de trabajo con OpenSpec

Cada caso de uso es un cambio en `openspec/changes/`, con nombre en español y kebab-case (`crear-reserva`).

1. `/opsx:new <nombre>` con el CU de origen.
2. `/opsx:continue`: proposal → specs → design → tasks. **Una persona revisa cada documento antes de seguir.**
3. `/opsx:apply`: implementa por grupos de tareas y para entre grupos para revisión.
4. `/opsx:verify`: compara lo implementado con las specs.
5. `/opsx:archive`: cierra el cambio para que `openspec/specs/` quede como la verdad actual. **Todo cambio terminado se archiva.**

Cada cambio mantiene un `traceability.md` con la tabla: escenario del CU, prueba que lo cubre y estado.

## 9. Comandos

```powershell
.\mvnw.cmd spotless:apply        # formatear
.\mvnw.cmd verify                # compilar, probar, cobertura y formato
.\mvnw.cmd spring-boot:run       # levantar la API
```

- Salud: http://localhost:8080/actuator/health
- Documentación de la API: http://localhost:8080/docs
- La conexión a la base se toma de `.env` (copia `.env.example`).

## 10. Archivos protegidos

No se tocan sin permiso explícito:
- `.env` y cualquier `.env.*` (salvo `.env.example`);
- las dependencias y plugins del `pom.xml`;
- `openspec/config.yaml`;
- este `AGENTS.md`;
- las migraciones de Flyway ya aplicadas.

No se agregan dependencias nuevas sin autorización.

## 11. Git

- Commits en español, con prefijo: `feat:`, `fix:`, `test:`, `docs:`, `refactor:` o `chore:`.
- Un cambio de OpenSpec por rama: `feature/<nombre-del-cambio>`.
- Nunca se suben secretos ni datos personales reales.

## 12. Al terminar cualquier tarea, reporta

- Qué se hizo y qué archivos se tocaron.
- Qué se ejecutó para validarlo y su resultado real.
- Qué quedó pendiente y por qué.

**Nunca afirmes que algo pasa o funciona si no lo ejecutaste.**
