# Proposal: crear-parametros-de-plataforma

## Why

La regla "Nada quemado" de AGENTS.md exige que todo plazo, límite, tiempo o cantidad configurable salga del módulo `parametros`, con mínimo y máximo. Ese módulo todavía no existe, y todos los casos de uso lo necesitan; el primero es el CU-00, que no se puede implementar sin sus parámetros de sesión, código de verificación y contraseña.

**Origen:**
- La regla 1 de AGENTS.md §4 ("Nada quemado"). Es la base que usarán todos los CU.
- Los parámetros iniciales salen del **CU-00, tabla "Parámetros que usa"**.

## What Changes

- Nuevo módulo `com.reservas.parametros`, con la estructura hexagonal de AGENTS.md §3 (`domain`, `application/port/input`, `application/port/output`, `application/service`, `infrastructure/adapter/output/persistence`). No tiene adaptador REST en este cambio.
- **Definición de parámetro**, que llega por migración, con:
  - clave única (por ejemplo `sesion.cliente.dias_inactividad`);
  - descripción en español;
  - tipo: entero, minutos, horas, días, booleano o lista de enteros;
  - valor por defecto, mínimo y máximo;
  - nivel máximo de personalización: plataforma, negocio o profesional.
- **Valores personalizados**, guardados aparte de la definición:
  - Es una tabla de valores, cada uno con su nivel.
  - Hoy solo existe el nivel plataforma. Los niveles negocio y profesional llegarán después, cada uno con el id de su negocio o profesional.
  - La jerarquía **profesional > negocio > plataforma** queda prevista en el modelo.
  - En este cambio la tabla queda vacía: los valores los cambiará el administrador cuando exista el panel (CU-13).
- **Valor vigente:** es el de la tabla de valores si existe; si no, el valor por defecto de la definición.
- **Validación de mínimo y máximo:** aplica tanto al valor por defecto como al valor guardado.
  - En **booleano** no aplica.
  - En **lista de enteros** aplica a cada elemento.
- **Puerto de entrada** para que otros módulos consulten el valor vigente de un parámetro por su clave. Responde con errores de dominio cuyos códigos y mensajes son constantes (AGENTS.md §5):
  - `PARAMETRO_NO_EXISTE`: la clave no existe;
  - `PARAMETRO_FUERA_DE_RANGO`: el valor vigente está fuera del mínimo y el máximo.
- **Primeras migraciones Flyway** del proyecto: crean las tablas de definiciones y de valores, e insertan las 8 definiciones iniciales (tabla abajo). El design las divide en esquema y datos.
- **Pruebas unitarias de `compartido.error`** (`ManejadorGlobalErrores` y `RespuestaError`). Hacen falta porque con este cambio empieza a aplicarse la cobertura mínima de JaCoCo.
- No hay cambios de contrato de la API. No hay endpoints nuevos, así que nada es **BREAKING**.

### Parámetros iniciales

Los valores por defecto vienen del CU-00, tabla "Parámetros que usa". Los mínimos, los máximos, los tipos y los niveles están aprobados.

| Fila del CU-00 | Clave | Descripción | Tipo | Por defecto | Mín. | Máx. | Nivel máx. |
|---|---|---|---|---|---|---|---|
| Cierre de sesión por inactividad | `sesion.cliente.dias_inactividad` | Días sin uso tras los cuales se cierra la sesión de un cliente | días | 60 | 1 | 365 | plataforma |
| Cierre de sesión por inactividad | `sesion.negocio.dias_inactividad` | Días sin uso tras los cuales se cierra la sesión de un profesional o dueño | días | 30 | 1 | 90 | plataforma |
| Cierre de sesión por inactividad | `sesion.panel.horas` | Horas que dura la sesión del panel desde que se inicia | horas | 8 | 1 | 24 | plataforma |
| Código de verificación | `codigo_verificacion.digitos` | Cantidad de dígitos del código de verificación | entero | 6 | 6 | 8 | plataforma |
| Código de verificación | `codigo_verificacion.minutos_vigencia` | Minutos de vigencia del código de verificación | minutos | 10 | 5 | 30 | plataforma |
| Código de verificación | `codigo_verificacion.intentos` | Intentos permitidos para ingresar el código de verificación | entero | 5 | 3 | 10 | plataforma |
| Intentos fallidos de contraseña | `contrasena.intentos_fallidos` | Intentos fallidos de contraseña antes de bloquear | entero | 5 | 3 | 10 | plataforma |
| Intentos fallidos de contraseña | `contrasena.minutos_bloqueo` | Minutos que dura el bloqueo por intentos fallidos de contraseña | minutos | 15 | 5 | 1440 | plataforma |

En el CU-00, "quién lo define" es el **Administrador** para estas tres filas. En este cambio eso se traduce en el valor por defecto que entra por migración. Que el administrador pueda cambiarlo llega con el CU-13.

Otras dos filas de la tabla del CU-00 quedan **fuera de alcance**:
- "Máximo de citas pendientes por invitado en un negocio": llega con reservas (CU-05).
- "Huella o PIN al abrir el tablero": llega con el tablero del dueño.

### Alcance

| Elemento | Estado |
|---|---|
| Módulo `parametros` hexagonal (dominio sin Spring) | incluido |
| Definición de parámetro (clave, descripción, tipo, por defecto, mín., máx., nivel máximo) | incluido |
| Tabla de valores con nivel; jerarquía profesional > negocio > plataforma prevista | incluido |
| Nivel plataforma | incluido |
| Valor vigente = valor guardado si existe, si no el por defecto | incluido |
| Validación de mín. y máx. sobre el por defecto y el valor guardado (no aplica en booleano; en lista, por elemento) | incluido |
| Puerto de entrada: consultar el valor vigente por clave | incluido |
| Error `PARAMETRO_NO_EXISTE` | incluido |
| Error `PARAMETRO_FUERA_DE_RANGO` | incluido |
| Migración Flyway con las 8 definiciones del CU-00 | incluido |
| Pruebas unitarias de `ManejadorGlobalErrores` y `RespuestaError` | incluido |
| Niveles negocio y profesional (guardar y resolver valores) | fuera de alcance |
| Endpoints REST para que el administrador vea o cambie parámetros | fuera de alcance (llegan con el CU-13, cuando haya autenticación) |
| Bitácora de cambios de parámetros | fuera de alcance |
| Parámetro "Máximo de citas pendientes por invitado en un negocio" | fuera de alcance (CU-05) |
| Parámetro "Huella o PIN al abrir el tablero" | fuera de alcance (tablero del dueño) |

### Escenarios del CU-00

Este cambio solo crea los parámetros. Los escenarios del CU-00 describen comportamiento de cuentas y sesiones, así que se implementan en el cambio del CU-00.

| Escenario del CU-00 | Estado en este cambio |
|---|---|
| Crear cuenta con Google | fuera de alcance (cambio del CU-00) |
| Misma cuenta con Google y con contraseña | fuera de alcance (cambio del CU-00) |
| Reservar como invitado sin código | fuera de alcance (cambio del CU-00) |
| El invitado crea cuenta después | fuera de alcance (cambio del CU-00) |
| Entrada del profesional | fuera de alcance (cambio del CU-00) |
| Dos negocios | fuera de alcance (cambio del CU-00) |
| Sesión vencida por no usar la app | fuera de alcance (cambio del CU-00). Depende de `sesion.cliente.dias_inactividad`, que crea este cambio. |
| Cerrar sesión en todos los dispositivos | fuera de alcance (cambio del CU-00) |
| Huella para el tablero | fuera de alcance (tablero del dueño). Su parámetro tampoco entra aquí. |

## Capabilities

### New Capabilities

- `parametros`: definición de los parámetros configurables (clave, tipo, valor por defecto, mínimo, máximo y nivel máximo de personalización), valores guardados por nivel, y consulta del valor vigente por clave, con errores cuando la clave no existe o el valor está fuera de rango. En este cambio solo existe el nivel plataforma.

### Modified Capabilities

Ninguna. No hay specs previas en `openspec/specs/`.

## Impact

- **Código nuevo:** el paquete `com.reservas.parametros` con su dominio, puertos, servicio y adaptador de persistencia (entidades JPA, repositorios Spring Data y mappers MapStruct).
- **Base de datos:** primera migración `V1__...sql` en `src/main/resources/db/migration`, con las tablas de definiciones y de valores. Con `ddl-auto: validate`, las entidades deben coincidir con el esquema.
- **API REST y OpenAPI:** sin cambios. reservas-app no se ve afectado.
- **Otros módulos:** quedan habilitados para consultar parámetros por el puerto de entrada. Ninguno existe todavía.
- **Dependencias:** ninguna nueva. Todo lo necesario ya está en el `pom.xml`.
- **Pruebas y cobertura:**
  - Es el primer cambio con pruebas, así que la regla de JaCoCo (80% de líneas y ramas) empieza a aplicarse sobre todo el código medido. Por eso se incluyen las pruebas de `compartido.error`.
  - Las pruebas se etiquetan con `@Tag("PARAMETROS")`.

## Puntos a confirmar

Ninguno pendiente. Las nueve preguntas de la versión anterior quedaron resueltas en la revisión:
1. Los parámetros se cotejaron con el CU-00, tabla "Parámetros que usa".
2. Mínimos y máximos aprobados, con tus ajustes en el código de verificación.
3. Se agrega el tipo horas.
4. Nivel máximo plataforma en los 8.
5. Valor vigente: el valor guardado si existe; si no, el por defecto.
6. Booleano sin mínimo ni máximo; lista de enteros por elemento.
7. Códigos de error aprobados.
8. Las pruebas de `compartido.error` entran en este cambio.
9. Etiqueta `@Tag("PARAMETROS")`.
