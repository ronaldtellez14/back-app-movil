# Spec Delta: parametros

## Purpose

Centraliza los plazos, límites, tiempos y cantidades configurables de la plataforma. Cada parámetro tiene una definición con su rango válido, y los demás módulos consultan su valor vigente por clave en lugar de escribir números fijos en el código.

## ADDED Requirements

### Requirement: Definición de parámetro

El sistema SHALL guardar cada parámetro con una definición que tenga:
- clave única;
- descripción en español;
- tipo: entero, minutos, horas, días, booleano o lista de enteros;
- valor por defecto, mínimo y máximo;
- nivel máximo de personalización: plataforma, negocio o profesional.

En los tipos numéricos y en lista de enteros, el mínimo y el máximo MUST existir. En booleano MUST NOT existir.

#### Scenario: Se registra una definición numérica válida
- **WHEN** una migración inserta un parámetro de tipo días, con un valor por defecto entre su mínimo y su máximo, y con nivel máximo plataforma
- **THEN** la migración se aplica
- **AND** la definición queda guardada con su clave, descripción, tipo, valor por defecto, mínimo, máximo y nivel máximo

#### Scenario: Se registra una definición booleana válida
- **WHEN** una migración inserta un parámetro de tipo booleano, con valor por defecto y sin mínimo ni máximo
- **THEN** la migración se aplica
- **AND** la definición queda guardada sin mínimo ni máximo

#### Scenario: Clave duplicada
- **WHEN** una migración inserta un parámetro con una clave que ya existe
- **THEN** la base de datos rechaza la inserción
- **AND** la migración falla sin aplicarse

#### Scenario: Tipo o nivel máximo no permitido
- **WHEN** una migración inserta un parámetro con un tipo que no está en la lista, o con un nivel máximo distinto de plataforma, negocio o profesional
- **THEN** la base de datos rechaza la inserción
- **AND** la migración falla sin aplicarse

### Requirement: La base de datos impide un valor por defecto fuera de rango

La base de datos MUST rechazar cualquier definición que no cumpla las siguientes reglas, de modo que una migración mal hecha falle al aplicarse:
- el mínimo debe ser menor o igual que el máximo;
- en los tipos numéricos, el valor por defecto debe estar entre el mínimo y el máximo;
- en lista de enteros, cada elemento del valor por defecto debe estar entre el mínimo y el máximo, y ninguno puede ser nulo;
- el valor por defecto debe corresponder al tipo.

#### Scenario: Valor por defecto menor que el mínimo
- **WHEN** una migración inserta un parámetro numérico cuyo valor por defecto es menor que su mínimo
- **THEN** la base de datos rechaza la inserción
- **AND** la migración falla sin aplicarse

#### Scenario: Valor por defecto mayor que el máximo
- **WHEN** una migración inserta un parámetro numérico cuyo valor por defecto es mayor que su máximo
- **THEN** la base de datos rechaza la inserción
- **AND** la migración falla sin aplicarse

#### Scenario: Valor por defecto en los límites del rango
- **WHEN** una migración inserta un parámetro numérico cuyo valor por defecto es igual a su mínimo, y otro cuyo valor por defecto es igual a su máximo
- **THEN** la migración se aplica
- **AND** las dos definiciones quedan guardadas

#### Scenario: Mínimo mayor que el máximo
- **WHEN** una migración inserta un parámetro cuyo mínimo es mayor que su máximo
- **THEN** la base de datos rechaza la inserción
- **AND** la migración falla sin aplicarse

#### Scenario: Lista con un elemento fuera de rango
- **WHEN** una migración inserta un parámetro de tipo lista de enteros con un elemento por fuera de su mínimo y su máximo
- **THEN** la base de datos rechaza la inserción
- **AND** la migración falla sin aplicarse

#### Scenario: Lista con un elemento nulo
- **WHEN** una migración inserta un parámetro de tipo lista de enteros con un elemento nulo
- **THEN** la base de datos rechaza la inserción
- **AND** la migración falla sin aplicarse

#### Scenario: Booleano con mínimo o máximo
- **WHEN** una migración inserta un parámetro de tipo booleano con mínimo o con máximo
- **THEN** la base de datos rechaza la inserción
- **AND** la migración falla sin aplicarse

#### Scenario: Valor por defecto que no corresponde al tipo
- **WHEN** una migración inserta un parámetro numérico con un valor por defecto booleano o de lista, o sin valor por defecto
- **THEN** la base de datos rechaza la inserción
- **AND** la migración falla sin aplicarse

### Requirement: Valores personalizados por nivel

El sistema SHALL guardar los valores personalizados aparte de la definición, cada uno con su nivel. La jerarquía profesional > negocio > plataforma queda prevista, pero en este cambio solo se MUST aceptar el nivel plataforma, con un único valor por parámetro. Todo valor personalizado MUST pertenecer a un parámetro definido.

#### Scenario: Valor de plataforma para un parámetro definido
- **WHEN** se guarda un valor de nivel plataforma para un parámetro definido, con el valor del tipo de ese parámetro
- **THEN** el valor queda guardado
- **AND** queda asociado a la clave del parámetro y al nivel plataforma

#### Scenario: Valor de un nivel distinto de plataforma
- **WHEN** se intenta guardar un valor de nivel negocio o de nivel profesional
- **THEN** la base de datos lo rechaza
- **AND** no queda ningún valor guardado

#### Scenario: Segundo valor de plataforma para el mismo parámetro
- **WHEN** se intenta guardar un segundo valor de nivel plataforma para un parámetro que ya tiene uno
- **THEN** la base de datos lo rechaza
- **AND** el valor que ya existía no cambia

#### Scenario: Valor para un parámetro que no existe
- **WHEN** se intenta guardar un valor para una clave que no tiene definición
- **THEN** la base de datos lo rechaza
- **AND** no queda ningún valor guardado

#### Scenario: Valor sin dato o con más de un dato
- **WHEN** se intenta guardar un valor personalizado sin ningún dato, o con datos de más de un tipo a la vez
- **THEN** la base de datos lo rechaza
- **AND** no queda ningún valor guardado

### Requirement: Consulta del valor vigente

El sistema SHALL ofrecer a los demás módulos una consulta del valor vigente de un parámetro por su clave. El valor vigente MUST ser el valor personalizado de nivel plataforma si existe; si no, el valor por defecto de la definición.

#### Scenario: Parámetro sin valor personalizado
- **WHEN** un módulo consulta un parámetro que no tiene valor personalizado
- **THEN** recibe el valor por defecto de la definición
- **AND** no ocurre ningún error

#### Scenario: Parámetro con valor personalizado de plataforma
- **WHEN** un módulo consulta un parámetro que tiene un valor personalizado de nivel plataforma, distinto del valor por defecto y dentro del rango
- **THEN** recibe el valor personalizado
- **AND** no recibe el valor por defecto

### Requirement: Valor entregado según su tipo

La consulta SHALL entregar el valor ya convertido según el tipo del parámetro:
- minutos, horas y días como una duración en su unidad;
- entero como número entero;
- booleano como verdadero o falso;
- lista de enteros como una lista de números enteros.

Pedir un parámetro como un tipo que no le corresponde MUST fallar como error de programación, sin código de error de dominio.

#### Scenario: Parámetro en minutos
- **WHEN** un módulo consulta como duración un parámetro de tipo minutos
- **THEN** recibe una duración equivalente a esa cantidad de minutos
- **AND** no tiene que convertir unidades

#### Scenario: Parámetro en horas
- **WHEN** un módulo consulta como duración un parámetro de tipo horas
- **THEN** recibe una duración equivalente a esa cantidad de horas
- **AND** no tiene que convertir unidades

#### Scenario: Parámetro en días
- **WHEN** un módulo consulta como duración un parámetro de tipo días
- **THEN** recibe una duración equivalente a esa cantidad de días
- **AND** no tiene que convertir unidades

#### Scenario: Parámetro entero
- **WHEN** un módulo consulta como entero un parámetro de tipo entero
- **THEN** recibe el número entero vigente
- **AND** no ocurre ningún error

#### Scenario: Parámetro booleano
- **WHEN** un módulo consulta como booleano un parámetro de tipo booleano
- **THEN** recibe verdadero o falso según el valor vigente
- **AND** no ocurre ningún error

#### Scenario: Parámetro lista de enteros
- **WHEN** un módulo consulta como lista un parámetro de tipo lista de enteros
- **THEN** recibe la lista de números enteros vigente, en el mismo orden
- **AND** no ocurre ningún error

#### Scenario: Tipo pedido que no corresponde
- **WHEN** un módulo consulta como entero un parámetro de tipo minutos
- **THEN** la consulta falla como error de programación
- **AND** el error no lleva código de error de dominio

### Requirement: Error cuando la clave no existe

Si se consulta una clave que no tiene definición, el sistema MUST responder con el error de dominio `PARAMETRO_NO_EXISTE`, cuyo mensaje es constante.

#### Scenario: Clave inexistente
- **WHEN** un módulo consulta una clave que no tiene definición
- **THEN** recibe el error `PARAMETRO_NO_EXISTE`
- **AND** el error tiene su mensaje constante y no entrega ningún valor

### Requirement: Error cuando el valor vigente está fuera de rango

Si el valor vigente, sea personalizado o por defecto, está por fuera del mínimo y el máximo, el sistema MUST responder con el error de dominio `PARAMETRO_FUERA_DE_RANGO`, cuyo mensaje es constante. En lista de enteros, la validación MUST revisar cada elemento. En booleano MUST NOT aplicarse.

#### Scenario: Valor personalizado fuera de rango
- **WHEN** un módulo consulta un parámetro cuyo valor personalizado de plataforma está por fuera de su mínimo y su máximo
- **THEN** recibe el error `PARAMETRO_FUERA_DE_RANGO`
- **AND** no recibe ni el valor personalizado ni el valor por defecto

#### Scenario: Valor por defecto fuera de rango
- **WHEN** un módulo consulta un parámetro sin valor personalizado cuyo valor por defecto está por fuera de su mínimo y su máximo
- **THEN** recibe el error `PARAMETRO_FUERA_DE_RANGO`
- **AND** no recibe el valor por defecto

#### Scenario: Valor en los límites del rango
- **WHEN** un módulo consulta un parámetro cuyo valor vigente es igual a su mínimo o igual a su máximo
- **THEN** recibe ese valor
- **AND** no ocurre ningún error

#### Scenario: Lista con un elemento fuera de rango
- **WHEN** un módulo consulta un parámetro de tipo lista de enteros con un solo elemento por fuera de su mínimo y su máximo
- **THEN** recibe el error `PARAMETRO_FUERA_DE_RANGO`
- **AND** no recibe la lista

#### Scenario: Booleano sin rango
- **WHEN** un módulo consulta un parámetro de tipo booleano
- **THEN** recibe su valor vigente sin validación de rango
- **AND** no recibe el error `PARAMETRO_FUERA_DE_RANGO`

### Requirement: Errores de parámetros en la API sin datos internos

Si un error de parámetros llega a una respuesta de la API, el sistema MUST responder HTTP 500 con la respuesta estándar de error interno y su identificador de traza. La respuesta MUST NOT exponer la clave, el valor, el rango ni el mensaje del error de dominio, y el log MUST NOT incluir datos personales.

#### Scenario: Error de parámetro durante una solicitud
- **WHEN** una solicitud a la API dispara `PARAMETRO_NO_EXISTE` o `PARAMETRO_FUERA_DE_RANGO` sin manejo propio
- **THEN** la respuesta es HTTP 500 con `codigo` `ERROR_INTERNO`, su mensaje constante y un `traza_id`
- **AND** la respuesta no contiene la clave, el valor, el mínimo, el máximo ni el mensaje del error de dominio

#### Scenario: Registro del error en el log
- **WHEN** se registra en el log un error de parámetro que llegó a la API
- **THEN** el log contiene el mismo `traza_id` de la respuesta, el tipo del error y su traza
- **AND** el log no contiene el mensaje del error

### Requirement: Parámetros iniciales del CU-00

Al aplicar las migraciones, el sistema SHALL tener definidos los 8 parámetros del CU-00, tabla "Parámetros que usa", con los datos del escenario "Base de datos recién migrada" y sin valores personalizados.

#### Scenario: Base de datos recién migrada
- **WHEN** se aplican las migraciones sobre una base de datos vacía
- **THEN** existen exactamente los 8 parámetros de la tabla siguiente, con su tipo, valor por defecto, mínimo, máximo y nivel máximo, y con la descripción del proposal
- **AND** no existe ningún valor personalizado

| Clave | Tipo | Por defecto | Mín. | Máx. | Nivel máx. |
|---|---|---|---|---|---|
| `sesion.cliente.dias_inactividad` | días | 60 | 1 | 365 | plataforma |
| `sesion.negocio.dias_inactividad` | días | 30 | 1 | 90 | plataforma |
| `sesion.panel.horas` | horas | 8 | 1 | 24 | plataforma |
| `codigo_verificacion.digitos` | entero | 6 | 6 | 8 | plataforma |
| `codigo_verificacion.minutos_vigencia` | minutos | 10 | 5 | 30 | plataforma |
| `codigo_verificacion.intentos` | entero | 5 | 3 | 10 | plataforma |
| `contrasena.intentos_fallidos` | entero | 5 | 3 | 10 | plataforma |
| `contrasena.minutos_bloqueo` | minutos | 15 | 5 | 1440 | plataforma |

#### Scenario: Consulta de un parámetro inicial
- **WHEN** un módulo consulta como duración el parámetro de cierre de sesión por inactividad del cliente, en una base recién migrada
- **THEN** recibe una duración igual al valor por defecto de ese parámetro, en días
- **AND** no ocurre ningún error

### Requirement: Sin endpoints de parámetros

En este cambio el sistema MUST NOT exponer ningún endpoint para consultar ni modificar parámetros. La consulta solo está disponible para los demás módulos, dentro del backend.

#### Scenario: Contrato OpenAPI
- **WHEN** se consulta el contrato OpenAPI publicado
- **THEN** no aparece ninguna ruta de parámetros
- **AND** el contrato que usa reservas-app no cambia
