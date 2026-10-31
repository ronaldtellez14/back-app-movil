# Tasks

Todas las pruebas usan JUnit 5 y Mockito y llevan `@Tag("PARAMETROS")`. Los comandos se corren en la raíz del repositorio. Después de escribir código Java, `.\mvnw.cmd spotless:apply` deja el formato que exige `verify`.

## 1. Migración Flyway

- [x] 1.1 Crear `src/main/resources/db/migration/V1__crear_tablas_parametros.sql` con las tablas `parametro` y `valor_parametro` y las restricciones con nombre de la decisión 6 (`ck_parametro_tipo`, `ck_parametro_nivel_maximo`, `ck_parametro_defecto_en_rango`, `ck_valor_parametro_nivel`, `ck_valor_parametro_un_valor`, `uq_valor_parametro_clave_nivel` y la FK a `parametro(clave)`), sin `SELECT *`. Verificar: al arrancar la app, Flyway aplica `V1` sobre `reservas`, y `\d parametro` y `\d valor_parametro` en `psql` muestran las columnas y las restricciones con esos nombres.
- [x] 1.2 Crear `V2__agregar_parametros_cu00.sql` con las 8 definiciones del proposal (clave, descripción, tipo, por defecto, mínimo, máximo y nivel máximo `PLATAFORMA`). Verificar: al arrancar, Flyway aplica `V2`; una consulta en `psql` devuelve exactamente esas 8 filas con esos valores, y `valor_parametro` está vacía.

## 2. Dominio

- [x] 2.1 Crear `TipoParametro` (ENTERO, MINUTOS, HORAS, DIAS, BOOLEANO, LISTA_ENTEROS) y `NivelParametro` (PLATAFORMA, NEGOCIO, PROFESIONAL) en `com.reservas.parametros.domain`. Verificar: `.\mvnw.cmd compile` en verde.
- [x] 2.2 Crear `ValorParametro` como tipo sellado con `Numero(long)`, `Booleano(boolean)` y `ListaEnteros(List<Long>)`; la lista se guarda como copia inmutable. Verificar: `.\mvnw.cmd compile` en verde.
- [x] 2.3 Crear `CodigoErrorParametro` (`PARAMETRO_NO_EXISTE`, `PARAMETRO_FUERA_DE_RANGO`, con código y mensaje constantes, sin `HttpStatus`) y `ParametroException` (código y clave; `getMessage()` devuelve el mensaje constante). Verificar: `.\mvnw.cmd compile` en verde.
- [x] 2.4 Crear `DefinicionParametro` con clave, descripción, tipo, valor por defecto, mínimo, máximo y nivel máximo, y su método `validarRango(ValorParametro)`. El método no valida nada en booleano, valida cada elemento en una lista y lanza `ParametroException(PARAMETRO_FUERA_DE_RANGO)`. El constructor no valida el rango (decisión 4). Verificar: `.\mvnw.cmd compile` en verde, y una búsqueda de `org.springframework` en `parametros/domain` no devuelve nada.

## 3. Puertos

- [x] 3.1 Crear `ConsultarParametroUseCase` en `application/port/input` con `obtenerEntero`, `obtenerDuracion`, `obtenerBooleano` y `obtenerListaEnteros` (decisión 2). Verificar: `.\mvnw.cmd compile` en verde.
- [x] 3.2 Crear `ClavesParametro` en `application/port/input` con las 8 claves como constantes `String`. Verificar: cada constante coincide, letra por letra, con una clave de `V2__agregar_parametros_cu00.sql`; lo confirma la prueba 9.5.
- [x] 3.3 Crear `CargarParametroPort` en `application/port/output` con `Optional<DefinicionParametro> buscarDefinicion(String clave)` y `Optional<ValorParametro> buscarValorDePlataforma(String clave)`. Verificar: `.\mvnw.cmd compile` en verde.

## 4. Casos de uso

- [x] 4.1 Crear `ConsultarParametroService`, que implementa `ConsultarParametroUseCase` con `@Transactional(readOnly = true)` y en este orden: buscar la definición (si no existe, `PARAMETRO_NO_EXISTE`); tomar el valor de plataforma o, si no hay, el por defecto; ejecutar `validarRango`; convertir al tipo pedido (`Duration.ofMinutes`, `ofHours` u `ofDays`). Un tipo pedido que no corresponde lanza `IllegalStateException`. Verificar: `.\mvnw.cmd compile` en verde; el comportamiento lo prueba la tarea 8.2.

## 5. Adaptadores

- [x] 5.1 Crear `ParametroEntity` y `ValorParametroEntity` en `infrastructure/adapter/output/persistence`, con `@Enumerated(EnumType.STRING)` y los arreglos como `List<Long>` con `@JdbcTypeCode(SqlTypes.ARRAY)` (decisión 8). Verificar: la app arranca con `ddl-auto: validate` contra `reservas` sin errores de esquema. Si Hibernate rechaza `List<Long>`, usar `Long[]`, como dice el riesgo del design.
- [x] 5.2 Crear `ParametroJpaRepository` y `ValorParametroJpaRepository` con métodos derivados (`findById`, `findByClaveAndNivel`), sin consultas con `SELECT *`. Verificar: `.\mvnw.cmd compile` en verde.
- [x] 5.3 Crear `ParametroPersistenceMapper` (MapStruct), con métodos `default` que convierten las columnas tipadas a `ValorParametro`. Verificar: `.\mvnw.cmd compile` genera `ParametroPersistenceMapperImpl` en `target/generated-sources/annotations`.
- [x] 5.4 Crear `ParametroPersistenceAdapter`, que implementa `CargarParametroPort` con los repositorios y el mapper. Verificar: la app arranca y el contexto inyecta `ConsultarParametroUseCase` sin errores.

## 6. API REST y DTOs

No aplica: este cambio no tiene endpoints (spec "Sin endpoints de parámetros").

## 7. OpenAPI

No aplica: el contrato OpenAPI no cambia. Lo comprueba la prueba 9.7.

## 8. Pruebas unitarias

- [ ] 8.1 `DefinicionParametroTest` cubre los casos de la decisión 10: dentro de rango, en los límites, debajo del mínimo, encima del máximo, booleano sin rango y lista con un elemento fuera de rango. Verificar: `.\mvnw.cmd test` en verde.
- [ ] 8.2 `ConsultarParametroServiceTest`, con `CargarParametroPort` simulado en Mockito, cubre:
  - clave inexistente → `PARAMETRO_NO_EXISTE`;
  - sin valor guardado → el por defecto;
  - con valor guardado → el guardado;
  - guardado fuera de rango y por defecto fuera de rango → `PARAMETRO_FUERA_DE_RANGO`;
  - minutos, horas y días → `Duration`;
  - entero, booleano y lista;
  - tipo equivocado → `IllegalStateException`.

  Verificar: `.\mvnw.cmd test` en verde.
- [ ] 8.3 `ParametroPersistenceMapperTest` convierte cada tipo (número, booleano y lista), con columnas nulas y no nulas. Verificar: `.\mvnw.cmd test` en verde.
- [ ] 8.4 `ParametroPersistenceAdapterTest`, con los repositorios simulados, cubre: definición encontrada, no encontrada, valor de plataforma presente y ausente. Verificar: `.\mvnw.cmd test` en verde.
- [ ] 8.5 `RespuestaErrorTest` cubre las fábricas `de`, `de` con campos y `conTraza`, y que `traza_id` solo se serializa cuando tiene valor. Verificar: `.\mvnw.cmd test` en verde.
- [ ] 8.6 `ManejadorGlobalErroresTest` cubre cada manejador (validación con nombres en snake_case, cuerpo ilegible, acceso denegado, recurso no encontrado, método no permitido y error no controlado). Para una `ParametroException`:
  - la respuesta es 500 `ERROR_INTERNO` con `traza_id`, sin la clave, el valor, el rango ni el mensaje;
  - el log, capturado con un `ListAppender` de Logback, ya incluido en Spring Boot, contiene el `traza_id`, el tipo y la traza, pero no el mensaje.

  Verificar: `.\mvnw.cmd test` en verde.
- [ ] 8.7 Confirmar que las pruebas unitarias corren sin PostgreSQL. Verificar: `.\mvnw.cmd test` en verde con `DB_URL=jdbc:postgresql://localhost:1/no_existe` solo para esa ejecución.

## 9. Pruebas de integración

- [ ] 9.1 Agregar `maven-failsafe-plugin` en `<build><plugins>` del `pom.xml`, sin versión ni ejecuciones propias, porque las gestiona el parent (autorizado, decisión 11). Verificar: `.\mvnw.cmd verify` muestra `maven-failsafe-plugin:integration-test` y ejecuta las clases `*IT`.
- [ ] 9.2 Crear `src/test/resources/application-it.yml` (perfil `it`):
  - `spring.datasource.url: ${DB_TEST_URL:jdbc:postgresql://localhost:5432/reservas_test}`, con usuario y contraseña de `.env`; ese valor por defecto es la referencia de `DB_TEST_URL`, sin `.env.example`;
  - `spring.flyway.clean-disabled: false`.

  Verificar: una prueba `*IT` con `@ActiveProfiles("it")` se conecta a `reservas_test`.
- [ ] 9.3 Crear en el código de pruebas la `FlywayMigrationStrategy` con protección: consulta `SELECT current_database()`; si el nombre no termina en `_test`, lanza una excepción sin limpiar; si termina en `_test`, ejecuta `clean()` y luego `migrate()`. Verificar: `FlywayLimpiezaSeguraTest` (unitaria, con Mockito) comprueba que con un nombre como `reservas` se lanza la excepción y nunca se llama `clean()`, y que con `reservas_test` se llama `clean()` y luego `migrate()`.
- [ ] 9.4 `EsquemaParametrosIT` cubre los escenarios de los requisitos "Definición de parámetro", "La base de datos impide un valor por defecto fuera de rango" y "Valores personalizados por nivel", con un caso por escenario:
  - inserciones válidas: numérica, booleana, en los límites y valor de plataforma;
  - inserciones rechazadas, cada una con el nombre de su restricción.

  Cada prueba revierte su transacción. Verificar: `.\mvnw.cmd verify` en verde.
- [ ] 9.5 `ParametrosInicialesIT` comprueba:
  - el contexto arranca con `ddl-auto: validate`;
  - `parametro` tiene exactamente las 8 definiciones de la tabla del spec;
  - `valor_parametro` está vacía;
  - cada constante de `ClavesParametro` existe en la base;
  - `obtenerDuracion(SESION_CLIENTE_DIAS_INACTIVIDAD)` devuelve 60 días.

  Verificar: `.\mvnw.cmd verify` en verde.
- [ ] 9.6 `ConsultarParametroIT` inserta un valor de plataforma dentro de rango y comprueba que `ConsultarParametroUseCase` lo devuelve en lugar del por defecto; luego revierte la transacción. Verificar: `.\mvnw.cmd verify` en verde.
- [ ] 9.7 `ContratoOpenApiIT` consulta `/docs/api-docs` y comprueba que ninguna ruta contiene `parametros`. Verificar: `.\mvnw.cmd verify` en verde.
- [ ] 9.8 Revisar el reporte `target/site/jacoco/index.html`.
  - Si las ramas quedan bajo el 80% por `*MapperImpl` después de la tarea 8.3, excluir `**/*MapperImpl*` en JaCoCo con el comentario de autorización de la decisión 12.
  - Si no, no excluir nada.

  Verificar: el reporte muestra 80% o más en líneas y ramas.

## 10. Validation

- [ ] 10.1 `.\mvnw.cmd verify` en verde: Spotless, surefire, failsafe y JaCoCo (80% en líneas y ramas). Verificar: la salida termina en `BUILD SUCCESS`.
- [ ] 10.2 Revisar que los logs no expongan datos personales ni valores de parámetros: buscar en la salida de `verify` y en el arranque de la app que los errores solo registren `traza_id`, tipo y traza. Verificar: la búsqueda no encuentra mensajes de excepción ni datos.
- [ ] 10.3 Crear `traceability.md` del cambio con una fila por escenario del spec: escenario, prueba que lo cubre y estado. Verificar: los 37 escenarios tienen una prueba, y su estado sale de una ejecución real de `verify`.
- [ ] 10.4 `openspec validate crear-parametros-de-plataforma --strict` en verde. Verificar: la salida dice "is valid".
