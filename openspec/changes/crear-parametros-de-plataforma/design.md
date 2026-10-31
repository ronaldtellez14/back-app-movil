# Design: crear-parametros-de-plataforma

## Context

Ver `proposal.md` para el porqué y el alcance.

Estado actual:
- El proyecto tiene solo la base: `ApiApplication`, `compartido.config` y `compartido.error`.
- No hay ninguna migración Flyway ni ningún módulo de negocio. `ddl-auto` está en `validate`.

Restricciones que marcan el diseño:
- **El dominio no depende de Spring** (AGENTS.md §3). `compartido.error.CodigoError` usa `HttpStatus` de Spring, así que el dominio de `parametros` no puede usarlo.
- **No hay autenticación ni endpoints de administración**, así que nada escribe valores personalizados. La tabla de valores existe pero queda vacía.
- **No se agregan dependencias** sin autorización (AGENTS.md §10). Para este cambio están autorizados:
  - agregar `maven-failsafe-plugin` (decisión 11);
  - excluir `*MapperImpl` en JaCoCo, si hace falta (decisión 12).
- **La máquina no tiene Docker.** Por eso se descartó Testcontainers en la decisión 11.
- **Base de datos:** PostgreSQL 17, con Hibernate 7.4 (el de Spring Boot 4.1.2).

## Goals / Non-Goals

**Goals:**
- Una sola forma de consultar parámetros para todos los módulos: un puerto de entrada con métodos tipados.
- Que la base de datos rechace una definición inválida, para que una migración mal hecha falle al aplicarse.
- Dejar listo el patrón para que cada CU agregue sus parámetros con una migración nueva.

**Non-Goals:**
- **Caché de parámetros.** Se evalúa cuando el administrador pueda cambiar valores (CU-13); ver decisión 9.
- **Traducir errores de dominio a HTTP.** Hoy ningún endpoint usa parámetros. Si un error de parámetros llega a la API, el manejador global responde un 500 con `traza_id`. Un mapeo general de errores de dominio llegará con el primer CU que exponga endpoints.
- **Validar que el nivel de un valor guardado no supere el `nivel_maximo` de su definición.** Solo importa cuando existan los niveles negocio y profesional.

## Decisions

### 1. Estructura del módulo

```
com.reservas.parametros/
├── domain/
│   ├── TipoParametro               ← ENTERO, MINUTOS, HORAS, DIAS, BOOLEANO, LISTA_ENTEROS
│   ├── NivelParametro              ← PLATAFORMA, NEGOCIO, PROFESIONAL
│   ├── ValorParametro              ← tipo sellado: Numero(long), Booleano(boolean), ListaEnteros(List<Long>)
│   ├── DefinicionParametro         ← clave, descripción, tipo, por defecto, mín., máx., nivel máximo; valida el rango
│   ├── CodigoErrorParametro        ← PARAMETRO_NO_EXISTE, PARAMETRO_FUERA_DE_RANGO (código y mensaje constantes)
│   └── ParametroException          ← excepción de dominio con su código y la clave
├── application/port/input/
│   ├── ConsultarParametroUseCase   ← obtenerEntero, obtenerDuracion, obtenerBooleano, obtenerListaEnteros
│   └── ClavesParametro             ← constantes con las claves (las 8 del CU-00)
├── application/port/output/
│   └── CargarParametroPort         ← buscarDefinicion(clave), buscarValorDePlataforma(clave)
├── application/service/
│   └── ConsultarParametroService   ← implementa el caso de uso
└── infrastructure/adapter/output/persistence/
    ├── ParametroEntity, ValorParametroEntity
    ├── ParametroJpaRepository, ValorParametroJpaRepository
    ├── ParametroPersistenceMapper  ← MapStruct
    └── ParametroPersistenceAdapter ← implementa CargarParametroPort
```

- **Por qué:** es la plantilla de AGENTS.md §3, sin la carpeta `input/rest`, porque no hay endpoints.
- **Alternativa descartada:** poner el acceso a datos directamente en el servicio. Rompería la arquitectura hexagonal y no permitiría probar el servicio sin base de datos.

### 2. Puerto de entrada con métodos tipados

`ConsultarParametroUseCase` ofrece cuatro métodos:
- `long obtenerEntero(String clave)`: para el tipo ENTERO.
- `Duration obtenerDuracion(String clave)`: para MINUTOS, HORAS y DIAS. Usa `Duration.ofMinutes`, `ofHours` u `ofDays`.
- `boolean obtenerBooleano(String clave)`: para BOOLEANO.
- `List<Long> obtenerListaEnteros(String clave)`: para LISTA_ENTEROS.

Si se pide un tipo que no corresponde a la clave (por ejemplo, `obtenerEntero` sobre un parámetro en minutos), lanza `IllegalStateException`. No es un error de dominio: es un error de programación del módulo que consulta, y no lleva código propio.

- **Por qué:** quien consulta recibe el valor ya convertido, con su unidad. Con una `Duration`, nadie tiene que acordarse de si `codigo_verificacion.minutos_vigencia` venía en minutos o en segundos, y no aparecen multiplicaciones quemadas en otros módulos.
- **Alternativa descartada 1:** un único método que devuelva `ValorParametro` genérico. Cada consumidor tendría que revisar el tipo y convertirlo.
- **Alternativa descartada 2:** un tercer código de error de dominio para el tipo equivocado. Ese caso no está en el proposal y no es una regla de negocio.

### 3. Claves como constantes

`ClavesParametro` reúne las claves como constantes `String`, por ejemplo `SESION_CLIENTE_DIAS_INACTIVIDAD = "sesion.cliente.dias_inactividad"`. Cada CU agrega las suyas.

- **Por qué:** así un error al escribir una clave se detecta al compilar y no en ejecución. Además, la clase vive en el puerto de entrada, que es lo que otros módulos pueden usar (AGENTS.md §3).
- **Alternativa descartada:** un `enum` de claves. Obligaría a tocar el enum y la migración en el mismo cambio, y su `name()` no coincide con el formato con puntos.

### 4. Valor vigente y validación en el dominio

`ConsultarParametroService` resuelve el valor así:
1. `buscarDefinicion(clave)`. Si no existe, lanza `ParametroException(PARAMETRO_NO_EXISTE)`.
2. `buscarValorDePlataforma(clave)`. Si hay valor, ese es el vigente; si no, el vigente es el valor por defecto de la definición.
3. `definicion.validarRango(valor)` revisa que el valor esté entre el mínimo y el máximo; en una lista revisa cada elemento, y en booleano no hace nada. Si el valor está fuera de rango, lanza `ParametroException(PARAMETRO_FUERA_DE_RANGO)`.
4. Convierte el valor al tipo pedido (decisión 2).

`DefinicionParametro` **no** valida el rango al construirse. Así, un valor por defecto inválido que llegara a leerse produce `PARAMETRO_FUERA_DE_RANGO`, el código aprobado, y no otra excepción.

- **Por qué:** la base de datos no puede comparar un valor guardado con su definición, porque están en tablas distintas (decisión 6). Por eso la validación del valor guardado vive en el dominio y se puede probar sin base de datos.
- **Alternativa descartada:** validar solo al guardar. Hoy no existe la escritura, y un valor cambiado a mano en la base pasaría sin control.

### 5. Errores de dominio sin Spring

`CodigoErrorParametro` es un enum con `codigo` y `mensaje` constantes, sin `HttpStatus`.

`ParametroException` (una `RuntimeException`) lleva su código y la clave:
- `getMessage()` devuelve el mensaje constante del código.
- La clave va en un campo aparte. No es un dato personal.

- **Por qué:** cumple AGENTS.md §3 (dominio sin Spring) y §5 (códigos y mensajes como constantes). El código HTTP se asigna cuando un adaptador REST lo necesite.
- **Alternativa descartada:** agregar estos códigos a `compartido.error.CodigoError`. Ese enum depende de Spring y llevaría el dominio de `parametros` a depender de él.

### 6. Esquema de base de datos

**Tabla `parametro`** (definiciones):

| Columna | Tipo | Nota |
|---|---|---|
| `clave` | VARCHAR(100) | PK |
| `descripcion` | VARCHAR(255) | NOT NULL |
| `tipo` | VARCHAR(20) | NOT NULL |
| `nivel_maximo` | VARCHAR(20) | NOT NULL |
| `minimo`, `maximo` | BIGINT | NULL solo en booleano |
| `defecto_numero` | BIGINT | ENTERO, MINUTOS, HORAS y DIAS |
| `defecto_booleano` | BOOLEAN | BOOLEANO |
| `defecto_lista` | BIGINT[] | LISTA_ENTEROS |

Restricciones CHECK con nombre. Esto es lo que pediste: una migración con un valor por defecto fuera de rango falla al aplicarse.
- `ck_parametro_tipo`: `tipo` es uno de los seis valores válidos.
- `ck_parametro_nivel_maximo`: `nivel_maximo` es PLATAFORMA, NEGOCIO o PROFESIONAL.
- `ck_parametro_defecto_en_rango`, según el tipo:
  - **Numéricos:** solo `defecto_numero` tiene valor; `minimo` y `maximo` no son nulos; `minimo <= maximo`; y `defecto_numero BETWEEN minimo AND maximo`.
  - **Booleano:** solo `defecto_booleano` tiene valor; `minimo` y `maximo` son nulos.
  - **Lista:** solo `defecto_lista` tiene valor, sin elementos nulos; `minimo <= maximo`; `minimo <= ALL(defecto_lista)` y `maximo >= ALL(defecto_lista)`.

**Tabla `valor_parametro`** (valores personalizados; vacía en este cambio):

| Columna | Tipo | Nota |
|---|---|---|
| `id` | BIGINT | identity, PK |
| `clave` | VARCHAR(100) | FK a `parametro(clave)`, NOT NULL |
| `nivel` | VARCHAR(20) | NOT NULL |
| `valor_numero` / `valor_booleano` / `valor_lista` | BIGINT / BOOLEAN / BIGINT[] | exactamente uno con valor |

- `ck_valor_parametro_nivel`: `nivel = 'PLATAFORMA'`. Se amplía cuando lleguen los otros niveles.
- `ck_valor_parametro_un_valor`: exactamente una columna de valor no es nula.
- `uq_valor_parametro_clave_nivel`: único por (`clave`, `nivel`), es decir, un solo valor de plataforma por parámetro.

Decisiones dentro de esta:
- **Columnas tipadas** en vez de un texto. Así los CHECK comparan números sin conversiones, y Hibernate mapea cada columna a su tipo de Java.
  - Alternativa descartada: guardar el valor como `TEXT` o `JSONB`. Los CHECK necesitarían conversiones que, ante un dato mal escrito, fallan con errores poco claros, y un rango por elemento no se puede expresar sin subconsultas (prohibidas en un CHECK).
- **Sin `negocio_id` ni `profesional_id` todavía.** Las agrega la migración que traiga esos niveles, junto con el índice de `negocio_id` que exige AGENTS.md §6. La jerarquía queda prevista en el dominio (`NivelParametro`) y en la columna `nivel`.
  - Alternativa descartada: crearlas desde ya. Hoy no existen las tablas de negocios ni de profesionales, así que no se sabe el tipo de sus ids ni se pueden declarar las FK. Habría que adivinar.
- **Sin trigger** que compare el valor guardado con el rango de su definición. Esa comparación la hace el dominio (decisión 4).
  - Alternativa descartada: un trigger. Duplica la regla en SQL y PL/pgSQL, y es más difícil de probar y de mantener.

### 7. Migraciones Flyway

- `V1__crear_tablas_parametros.sql`: crea las tablas `parametro` y `valor_parametro`, con sus restricciones.
- `V2__agregar_parametros_cu00.sql`: inserta las 8 definiciones del proposal.

- **Por qué en dos archivos:** separa el esquema de los datos y fija el patrón para los próximos CU. Por ejemplo, el CU-05 agregará `V<n>__agregar_parametros_cu05.sql` sin tocar las migraciones existentes (AGENTS.md §6).
- **Alternativa descartada:** un solo `V1`. Mezcla el esquema con los datos del CU-00 y no deja un patrón claro.

### 8. Adaptador de persistencia

- Entidades JPA con `@Enumerated(EnumType.STRING)` para `tipo`, `nivel_maximo` y `nivel`.
- Arreglos `BIGINT[]` mapeados a `List<Long>` con `@JdbcTypeCode(SqlTypes.ARRAY)`. Hibernate 7 lo soporta sin dependencias extra.
- Un mapper MapStruct convierte entidad ⇄ dominio. La conversión de columnas tipadas al tipo sellado `ValorParametro` va en métodos `default` del mapper.
- Las consultas usan métodos derivados de Spring Data (`findById`, `findByClaveAndNivel`), nunca `SELECT *` (AGENTS.md §6).
- El servicio es `@Transactional(readOnly = true)`.
- **Alternativa descartada:** consultas con `JdbcTemplate`. Saldría del stack de AGENTS.md §2 (JPA y MapStruct) sin necesidad.

### 9. Sin caché por ahora

Cada consulta lee la base de datos: son dos lecturas por clave primaria o índice único.

- **Por qué:** sin escrituras no hay problema de valores viejos, pero agregar caché ahora obligaría a decidir cómo invalidarla antes de que exista el CU-13. Spring Cache sin proveedor, con un mapa en memoria, no expira.
- **Alternativa descartada:** `@Cacheable` desde ya. Cuando el administrador cambie un valor, la caché seguiría entregando el anterior sin que nadie lo note.

### 10. Pruebas unitarias (JUnit 5 + Mockito, `@Tag("PARAMETROS")`)

- **`DefinicionParametro`:** valor dentro de rango, en los límites, por debajo del mínimo, por encima del máximo, booleano sin rango y lista con un elemento fuera de rango.
- **`ConsultarParametroService`**, con el puerto simulado:
  - clave inexistente → `PARAMETRO_NO_EXISTE`;
  - sin valor guardado → usa el por defecto;
  - con valor guardado → usa el guardado;
  - valor guardado fuera de rango → `PARAMETRO_FUERA_DE_RANGO`;
  - valor por defecto fuera de rango → `PARAMETRO_FUERA_DE_RANGO`;
  - conversión a `Duration` para minutos, horas y días;
  - tipo equivocado → `IllegalStateException`.
- **Mapper y adaptador:** conversión de cada tipo.
- **`compartido.error`:** `RespuestaError` (fábricas, `traza_id` solo en errores internos) y cada manejador de `ManejadorGlobalErrores`. Incluye una aserción de que el log de error interno no contiene el mensaje de la excepción.

### 11. Pruebas de integración contra una base local `reservas_test`

Hay que probar dos cosas contra un PostgreSQL real:
- que los CHECK rechacen definiciones inválidas;
- que las entidades coincidan con el esquema (`ddl-auto: validate`).

**Cómo se ejecutan:**
- Las pruebas de integración se nombran `*IT` y las ejecuta **maven-failsafe-plugin** (goals `integration-test` y `verify`).
- Las pruebas unitarias siguen con **surefire**.
- Resultado: `.\mvnw.cmd test` no necesita PostgreSQL, y `.\mvnw.cmd verify` corre todo.
- **El `pom.xml` cambia así** (autorizado):
  - se declara `maven-failsafe-plugin` en `<build><plugins>`, con la versión y las ejecuciones que ya gestiona `spring-boot-starter-parent`;
  - no se agregan dependencias.
- **Cobertura:**
  - `jacoco:prepare-agent` define `argLine`, y surefire y failsafe lo usan, así que ambos escriben en `target/jacoco.exec` (se agregan, no se sobrescriben).
  - `report` y `check` corren en la fase `verify` después de `integration-test`, así que la cobertura incluye las dos clases de pruebas.

**Base de datos:**
- La base es `reservas_test`, local. La URL sale de `DB_TEST_URL` (por defecto `jdbc:postgresql://localhost:5432/reservas_test`), y el usuario y la contraseña son los de `.env` (`DB_USER` y `DB_PASSWORD`).
- `DB_TEST_URL` no se documenta en `.env.example` (el archivo no se recrea): su valor por defecto queda en `application-it.yml`.
- La configuración de pruebas vive en `src/test/resources/application-it.yml`, con el perfil `it` que activan las clases `*IT`. Ahí se habilita `spring.flyway.clean-disabled: false` solo para las pruebas.

**Limpieza con protección:**
- Una estrategia de migración de Flyway (`FlywayMigrationStrategy`, en el código de pruebas) hace lo siguiente antes de las pruebas:
  1. Consulta el nombre real de la base con `SELECT current_database()`. No lo deduce de la URL.
  2. Si el nombre **no termina en `_test`**, lanza una excepción y el contexto de pruebas no arranca, **sin limpiar nada**.
  3. Si termina en `_test`, ejecuta `flyway.clean()` y luego `flyway.migrate()`.
- Spring reutiliza el contexto de pruebas, así que la limpieza ocurre una vez por contexto y no en cada prueba. Las pruebas que insertan datos lo hacen dentro de una transacción que se revierte.
- **Alternativa descartada 1:** validar el nombre leyendo la URL. Es más frágil, por ejemplo con parámetros o alias de host.
- **Alternativa descartada 2:** confiar solo en que `DB_TEST_URL` esté bien escrita. Un error de copia borraría la base de desarrollo.

**La base `reservas_test`** se crea con `psql` si no existe. No forma parte de las migraciones.

**Alternativas descartadas:**
- Testcontainers: la máquina no tiene Docker y exigiría dependencias nuevas.
- Prescindir de las pruebas de integración: los CHECK quedarían sin prueba automática.
- H2: no tiene `BIGINT[]` ni el mismo SQL que PostgreSQL.

### 12. Exclusión de `*MapperImpl` en JaCoCo, solo si hace falta

- Primero se prueba el mapper, con valores nulos y no nulos.
- Si después de eso la cobertura de ramas queda por debajo del 80%, se excluye `**/*MapperImpl*` en la configuración de JaCoCo del `pom.xml`.
  - Junto a la exclusión va un comentario que dice que está autorizada y por qué: son ramas por nulos del código generado.
  - La autorización ya está dada.
- Si la cobertura llega al 80% sin excluir nada, no se excluye.
- **Por qué:** el código generado por MapStruct no tiene lógica propia, y la regla del 80% busca medir el código escrito a mano.
- **Alternativa descartada:** excluirlo desde el principio. Ocultaría un mapper mal configurado que nunca se ejecutó.

## Risks / Trade-offs

- [Riesgo] En un CHECK, una expresión que da `NULL` cuenta como aprobada. Por ejemplo, un elemento nulo en una lista haría pasar `minimo <= ALL(...)`. -> Mitigación: condiciones `IS NOT NULL` explícitas y una regla de "sin elementos nulos" en las listas. Una prueba de integración (`*IT`) inserta cada caso inválido y espera el rechazo (decisión 11).
- [Riesgo] Hibernate podría no validar bien `BIGINT[]` contra `List<Long>` con `ddl-auto: validate`. -> Mitigación: la prueba de integración levanta el contexto con `validate`. Si falla, se usa `Long[]` en la entidad y el mapper lo convierte.
- [Riesgo] Las clases que genera MapStruct (`*MapperImpl`) tienen ramas por nulos que bajan la cobertura de ramas. -> Mitigación: probar el mapper con valores nulos y no nulos. Si aun así baja del 80%, se excluye `*MapperImpl` en JaCoCo; esa exclusión ya está autorizada (decisión 12).
- [Riesgo] Las pruebas de integración limpian una base completa: si apuntan a la base equivocada, borran datos. -> Mitigación: la estrategia de Flyway revisa el nombre real de la base (`current_database()`) y se niega a limpiar si no termina en `_test` (decisión 11).
- [Riesgo] `.\mvnw.cmd verify` falla si PostgreSQL no está encendido o si falta `reservas_test`, y un CI futuro necesitará un servicio de PostgreSQL. -> Mitigación: `.\mvnw.cmd test` corre sin base de datos. El error de conexión es explícito, y la URL por defecto de `reservas_test` queda en `application-it.yml`.
- [Riesgo] `Duration.ofDays` cuenta días de 24 horas, no días calendario. -> Mitigación: Colombia no tiene horario de verano, así que en `America/Bogota` un día siempre dura 24 horas. Quien necesite días calendario de agenda lo resolverá en su CU con el puerto de reloj.
- [Riesgo] Un error de parámetros en ejecución llega al cliente como un 500 genérico. -> Mitigación: el `traza_id` en la respuesta y en el log permite encontrar la causa. Solo ocurre si una migración o un dato manual quedaron mal, porque los CHECK impiden casi todos esos casos.
- [Riesgo] Sin caché, cada consulta va a la base de datos. -> Mitigación: son lecturas por clave primaria o índice único, en tablas de pocas filas. Se revisa con el CU-13 (decisión 9).

## Migration Plan

1. Al arrancar, Flyway aplica `V1` y `V2` sobre la base `reservas`, que hoy solo tiene `flyway_schema_history`.
2. Hibernate valida las entidades contra el esquema (`ddl-auto: validate`). Si no coinciden, la app no arranca.
3. **Reversión:** Flyway Community no tiene migraciones hacia atrás. Mientras no haya producción, en desarrollo se puede borrar y volver a crear la base `reservas`. Después de producción, todo arreglo es una migración nueva y nunca se edita `V1` ni `V2` (AGENTS.md §6).

## Endpoints

Ninguno nuevo ni cambiado. El contrato OpenAPI que usa reservas-app no cambia.

## Open Questions

Ninguna.
