package com.reservas.parametros.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("PARAMETROS")
class DefinicionParametroTest {

  private static final String CLAVE = "codigo_verificacion.intentos";

  private static DefinicionParametro numerica(Long minimo, Long maximo) {
    return new DefinicionParametro(
        CLAVE,
        "Intentos permitidos",
        TipoParametro.ENTERO,
        new ValorParametro.Numero(5),
        minimo,
        maximo,
        NivelParametro.PLATAFORMA);
  }

  private static DefinicionParametro lista() {
    return new DefinicionParametro(
        "recordatorio.horas",
        "Horas de recordatorio",
        TipoParametro.LISTA_ENTEROS,
        new ValorParametro.ListaEnteros(List.of(1L, 24L)),
        1L,
        48L,
        NivelParametro.PLATAFORMA);
  }

  @Test
  void aceptaUnValorDentroDelRango() {
    assertThatCode(() -> numerica(3L, 10L).validarRango(new ValorParametro.Numero(5)))
        .doesNotThrowAnyException();
  }

  @ParameterizedTest
  @ValueSource(longs = {3, 10})
  void aceptaLosLimites(long limite) {
    assertThatCode(() -> numerica(3L, 10L).validarRango(new ValorParametro.Numero(limite)))
        .doesNotThrowAnyException();
  }

  @ParameterizedTest
  @ValueSource(longs = {2, 11})
  void rechazaUnValorDebajoDelMinimoOEncimaDelMaximo(long valor) {
    assertThatThrownBy(() -> numerica(3L, 10L).validarRango(new ValorParametro.Numero(valor)))
        .isInstanceOfSatisfying(
            ParametroException.class,
            ex -> {
              assertThat(ex.codigo()).isEqualTo(CodigoErrorParametro.PARAMETRO_FUERA_DE_RANGO);
              assertThat(ex.clave()).isEqualTo(CLAVE);
            });
  }

  @Test
  void rechazaUnNumeroSiLaDefinicionNoTieneRango() {
    assertThatThrownBy(() -> numerica(null, 10L).validarRango(new ValorParametro.Numero(5)))
        .isInstanceOf(ParametroException.class);
    assertThatThrownBy(() -> numerica(3L, null).validarRango(new ValorParametro.Numero(5)))
        .isInstanceOf(ParametroException.class);
  }

  @Test
  void noValidaRangoEnBooleano() {
    DefinicionParametro booleana =
        new DefinicionParametro(
            "tablero.huella",
            "Pedir huella",
            TipoParametro.BOOLEANO,
            new ValorParametro.Booleano(true),
            null,
            null,
            NivelParametro.PLATAFORMA);

    assertThatCode(() -> booleana.validarRango(new ValorParametro.Booleano(false)))
        .doesNotThrowAnyException();
  }

  @Test
  void aceptaUnaListaConTodosLosElementosEnRango() {
    assertThatCode(() -> lista().validarRango(new ValorParametro.ListaEnteros(List.of(1L, 48L))))
        .doesNotThrowAnyException();
  }

  @Test
  void rechazaUnaListaConUnElementoFueraDeRango() {
    assertThatThrownBy(
            () -> lista().validarRango(new ValorParametro.ListaEnteros(List.of(1L, 49L, 24L))))
        .isInstanceOfSatisfying(
            ParametroException.class,
            ex -> assertThat(ex.codigo()).isEqualTo(CodigoErrorParametro.PARAMETRO_FUERA_DE_RANGO));
  }

  @Test
  void elMensajeEsElConstanteYNoIncluyeLaClave() {
    ParametroException ex =
        new ParametroException(CodigoErrorParametro.PARAMETRO_FUERA_DE_RANGO, CLAVE);

    assertThat(ex.getMessage())
        .isEqualTo(CodigoErrorParametro.PARAMETRO_FUERA_DE_RANGO.mensaje())
        .doesNotContain(CLAVE);
  }
}
