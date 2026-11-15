# Trazabilidad: crear-parametros-de-plataforma

Escenarios de `specs/parametros/spec.md` y la prueba que cubre cada uno. Todas llevan `@Tag("PARAMETROS")`.

**Estado:** sale de la ejecución de `.\mvnw.cmd verify` del 2026-11-15, que terminó en `BUILD SUCCESS`:
- surefire: 65 pruebas unitarias;
- failsafe: 31 pruebas de integración contra `reservas_test`;
- en total, 96 pruebas, 0 fallas, 0 errores y 0 omitidas, según `target/surefire-reports` y `target/failsafe-reports`;
- JaCoCo: 100% de líneas y de ramas.

"Pasa" significa que todas las pruebas de la fila pasaron en esa ejecución.

## Definición de parámetro

| # | Escenario | Prueba | Estado |
|---|---|---|---|
| 1 | Se registra una definición numérica válida | `EsquemaParametrosIT.seRegistraUnaDefinicionNumericaValida` | Pasa |
| 2 | Se registra una definición booleana válida | `EsquemaParametrosIT.seRegistraUnaDefinicionBooleanaValida` | Pasa |
| 3 | Clave duplicada | `EsquemaParametrosIT.claveDuplicadaSeRechaza` (`pk_parametro`) | Pasa |
| 4 | Tipo o nivel máximo no permitido | `EsquemaParametrosIT.tipoNoPermitidoSeRechaza` (`ck_parametro_defecto_en_rango`, y sin él `ck_parametro_tipo`); `EsquemaParametrosIT.nivelMaximoNoPermitidoSeRechaza` (`ck_parametro_nivel_maximo`) | Pasa |

## La base de datos impide un valor por defecto fuera de rango

Todas las filas de rechazo de esta sección comprueban la restricción `ck_parametro_defecto_en_rango`.

| # | Escenario | Prueba | Estado |
|---|---|---|---|
| 5 | Valor por defecto menor que el mínimo | `EsquemaParametrosIT.valorPorDefectoMenorQueElMinimoSeRechaza` | Pasa |
| 6 | Valor por defecto mayor que el máximo | `EsquemaParametrosIT.valorPorDefectoMayorQueElMaximoSeRechaza` | Pasa |
| 7 | Valor por defecto en los límites del rango | `EsquemaParametrosIT.valorPorDefectoEnLosLimitesSeAcepta` | Pasa |
| 8 | Mínimo mayor que el máximo | `EsquemaParametrosIT.minimoMayorQueElMaximoSeRechaza` | Pasa |
| 9 | Lista con un elemento fuera de rango | `EsquemaParametrosIT.listaConUnElementoFueraDeRangoSeRechaza`; control válido: `EsquemaParametrosIT.seRegistraUnaDefinicionDeListaValida` | Pasa |
| 10 | Lista con un elemento nulo | `EsquemaParametrosIT.listaConUnElementoNuloSeRechaza` | Pasa |
| 11 | Booleano con mínimo o máximo | `EsquemaParametrosIT.booleanoConMinimoSeRechaza`; `EsquemaParametrosIT.booleanoConMaximoSeRechaza` | Pasa |
| 12 | Valor por defecto que no corresponde al tipo | `EsquemaParametrosIT.numericoConValorPorDefectoBooleanoSeRechaza`; `EsquemaParametrosIT.numericoConValorPorDefectoDeListaSeRechaza`; `EsquemaParametrosIT.numericoSinValorPorDefectoSeRechaza` | Pasa |

## Valores personalizados por nivel

| # | Escenario | Prueba | Estado |
|---|---|---|---|
| 13 | Valor de plataforma para un parámetro definido | `EsquemaParametrosIT.seGuardaUnValorDePlataformaParaUnParametroDefinido` | Pasa |
| 14 | Valor de un nivel distinto de plataforma | `EsquemaParametrosIT.valorDeNivelNegocioSeRechaza`; `EsquemaParametrosIT.valorDeNivelProfesionalSeRechaza` (`ck_valor_parametro_nivel`) | Pasa |
| 15 | Segundo valor de plataforma para el mismo parámetro | `EsquemaParametrosIT.segundoValorDePlataformaSeRechazaYElPrimeroNoCambia` (`uq_valor_parametro_clave_nivel`) | Pasa |
| 16 | Valor para un parámetro que no existe | `EsquemaParametrosIT.valorParaUnParametroQueNoExisteSeRechaza` (`fk_valor_parametro_parametro`) | Pasa |
| 17 | Valor sin dato o con más de un dato | `EsquemaParametrosIT.valorSinDatoSeRechaza`; `EsquemaParametrosIT.valorConMasDeUnDatoSeRechaza` (`ck_valor_parametro_un_valor`) | Pasa |

## Consulta del valor vigente

| # | Escenario | Prueba | Estado |
|---|---|---|---|
| 18 | Parámetro sin valor personalizado | `ConsultarParametroServiceTest.sinValorGuardadoDevuelveElPorDefecto` | Pasa |
| 19 | Parámetro con valor personalizado de plataforma | `ConsultarParametroServiceTest.conValorGuardadoDevuelveElGuardado`; contra PostgreSQL: `ConsultarParametroIT.devuelveElValorDePlataformaEnLugarDelPorDefecto` | Pasa |

## Valor entregado según su tipo

| # | Escenario | Prueba | Estado |
|---|---|---|---|
| 20 | Parámetro en minutos | `ConsultarParametroServiceTest.minutosSeEntreganComoDuracion` | Pasa |
| 21 | Parámetro en horas | `ConsultarParametroServiceTest.horasSeEntreganComoDuracion` | Pasa |
| 22 | Parámetro en días | `ConsultarParametroServiceTest.diasSeEntreganComoDuracion` | Pasa |
| 23 | Parámetro entero | `ConsultarParametroServiceTest.sinValorGuardadoDevuelveElPorDefecto`; `ConsultarParametroServiceTest.conValorGuardadoDevuelveElGuardado` | Pasa |
| 24 | Parámetro booleano | `ConsultarParametroServiceTest.booleanoDevuelveElValorVigente` | Pasa |
| 25 | Parámetro lista de enteros | `ConsultarParametroServiceTest.listaDevuelveElValorVigente` (en el mismo orden) | Pasa |
| 26 | Tipo pedido que no corresponde | `ConsultarParametroServiceTest.enteroSobreUnParametroEnMinutosLanzaIllegalState`; además `duracionSobreUnEnteroLanzaIllegalState`, `booleanoSobreUnEnteroLanzaIllegalState` y `listaSobreUnEnteroLanzaIllegalState` | Pasa |

## Error cuando la clave no existe

| # | Escenario | Prueba | Estado |
|---|---|---|---|
| 27 | Clave inexistente | `ConsultarParametroServiceTest.claveInexistenteLanzaParametroNoExiste` | Pasa |

## Error cuando el valor vigente está fuera de rango

| # | Escenario | Prueba | Estado |
|---|---|---|---|
| 28 | Valor personalizado fuera de rango | `ConsultarParametroServiceTest.valorGuardadoFueraDeRangoLanzaFueraDeRango` | Pasa |
| 29 | Valor por defecto fuera de rango | `ConsultarParametroServiceTest.valorPorDefectoFueraDeRangoLanzaFueraDeRango` | Pasa |
| 30 | Valor en los límites del rango | `ConsultarParametroServiceTest.valorGuardadoEnLosLimitesDelRangoSeEntrega` (mínimo y máximo); regla de dominio: `DefinicionParametroTest.aceptaLosLimites` | Pasa |
| 31 | Lista con un elemento fuera de rango | `ConsultarParametroServiceTest.listaConUnElementoFueraDeRangoLanzaFueraDeRango`; regla de dominio: `DefinicionParametroTest.rechazaUnaListaConUnElementoFueraDeRango` | Pasa |
| 32 | Booleano sin rango | `ConsultarParametroServiceTest.booleanoDevuelveElValorVigente`; regla de dominio: `DefinicionParametroTest.noValidaRangoEnBooleano` | Pasa |

## Errores de parámetros en la API sin datos internos

| # | Escenario | Prueba | Estado |
|---|---|---|---|
| 33 | Error de parámetro durante una solicitud | `ManejadorGlobalErroresTest.parametroExceptionDevuelve500SinClaveValorRangoNiMensaje` (con `PARAMETRO_NO_EXISTE` y con `PARAMETRO_FUERA_DE_RANGO`) | Pasa |
| 34 | Registro del error en el log | `ManejadorGlobalErroresTest.elLogDeParametroExceptionTieneTrazaIdTipoYTrazaSinElMensaje` | Pasa |

## Parámetros iniciales del CU-00

| # | Escenario | Prueba | Estado |
|---|---|---|---|
| 35 | Base de datos recién migrada | `ParametrosInicialesIT.existenExactamenteLasOchoDefinicionesDelCu00`; `ParametrosInicialesIT.noHayValoresPersonalizados`; `ParametrosInicialesIT.cadaConstanteDeClavesParametroExisteEnLaBase` | Pasa |
| 36 | Consulta de un parámetro inicial | `ParametrosInicialesIT.laSesionDelClienteDuraSesentaDias` | Pasa |

## Sin endpoints de parámetros

| # | Escenario | Prueba | Estado |
|---|---|---|---|
| 37 | Contrato OpenAPI | `ContratoOpenApiIT.ningunaRutaDelContratoEsDeParametros` | Pasa |
