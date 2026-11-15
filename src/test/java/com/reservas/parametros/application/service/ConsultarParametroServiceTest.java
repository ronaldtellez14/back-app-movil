package com.reservas.parametros.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reservas.parametros.application.port.output.CargarParametroPort;
import com.reservas.parametros.domain.CodigoErrorParametro;
import com.reservas.parametros.domain.DefinicionParametro;
import com.reservas.parametros.domain.NivelParametro;
import com.reservas.parametros.domain.ParametroException;
import com.reservas.parametros.domain.TipoParametro;
import com.reservas.parametros.domain.ValorParametro;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("PARAMETROS")
@ExtendWith(MockitoExtension.class)
class ConsultarParametroServiceTest {

  private static final String CLAVE = "parametro.de.prueba";

  @Mock private CargarParametroPort cargarParametroPort;

  @InjectMocks private ConsultarParametroService servicio;

  private static DefinicionParametro definicion(
      TipoParametro tipo, ValorParametro porDefecto, Long minimo, Long maximo) {
    return new DefinicionParametro(
        CLAVE, "Parámetro de prueba", tipo, porDefecto, minimo, maximo, NivelParametro.PLATAFORMA);
  }

  private static DefinicionParametro numerica(TipoParametro tipo, long porDefecto) {
    return definicion(tipo, new ValorParametro.Numero(porDefecto), 1L, 100L);
  }

  private void dadaLaDefinicion(DefinicionParametro definicion) {
    when(cargarParametroPort.buscarDefinicion(CLAVE)).thenReturn(Optional.of(definicion));
  }

  private void sinValorGuardado() {
    when(cargarParametroPort.buscarValorDePlataforma(CLAVE)).thenReturn(Optional.empty());
  }

  private void conValorGuardado(ValorParametro valor) {
    when(cargarParametroPort.buscarValorDePlataforma(CLAVE)).thenReturn(Optional.of(valor));
  }

  @Test
  void claveInexistenteLanzaParametroNoExiste() {
    when(cargarParametroPort.buscarDefinicion(CLAVE)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> servicio.obtenerEntero(CLAVE))
        .isInstanceOfSatisfying(
            ParametroException.class,
            ex -> {
              assertThat(ex.codigo()).isEqualTo(CodigoErrorParametro.PARAMETRO_NO_EXISTE);
              assertThat(ex.getMessage())
                  .isEqualTo(CodigoErrorParametro.PARAMETRO_NO_EXISTE.mensaje());
              assertThat(ex.clave()).isEqualTo(CLAVE);
            });
    verify(cargarParametroPort, never()).buscarValorDePlataforma(CLAVE);
  }

  @ParameterizedTest
  @ValueSource(longs = {1, 100})
  void valorGuardadoEnLosLimitesDelRangoSeEntrega(long limite) {
    dadaLaDefinicion(numerica(TipoParametro.ENTERO, 5));
    conValorGuardado(new ValorParametro.Numero(limite));

    assertThat(servicio.obtenerEntero(CLAVE)).isEqualTo(limite);
  }

  @Test
  void sinValorGuardadoDevuelveElPorDefecto() {
    dadaLaDefinicion(numerica(TipoParametro.ENTERO, 5));
    sinValorGuardado();

    assertThat(servicio.obtenerEntero(CLAVE)).isEqualTo(5);
  }

  @Test
  void conValorGuardadoDevuelveElGuardado() {
    dadaLaDefinicion(numerica(TipoParametro.ENTERO, 5));
    conValorGuardado(new ValorParametro.Numero(7));

    assertThat(servicio.obtenerEntero(CLAVE)).isEqualTo(7);
  }

  @Test
  void valorGuardadoFueraDeRangoLanzaFueraDeRango() {
    dadaLaDefinicion(numerica(TipoParametro.ENTERO, 5));
    conValorGuardado(new ValorParametro.Numero(101));

    assertThatThrownBy(() -> servicio.obtenerEntero(CLAVE))
        .isInstanceOfSatisfying(
            ParametroException.class,
            ex -> assertThat(ex.codigo()).isEqualTo(CodigoErrorParametro.PARAMETRO_FUERA_DE_RANGO));
  }

  @Test
  void valorPorDefectoFueraDeRangoLanzaFueraDeRango() {
    dadaLaDefinicion(numerica(TipoParametro.MINUTOS, 0));
    sinValorGuardado();

    assertThatThrownBy(() -> servicio.obtenerDuracion(CLAVE))
        .isInstanceOfSatisfying(
            ParametroException.class,
            ex -> assertThat(ex.codigo()).isEqualTo(CodigoErrorParametro.PARAMETRO_FUERA_DE_RANGO));
  }

  @Test
  void minutosSeEntreganComoDuracion() {
    dadaLaDefinicion(numerica(TipoParametro.MINUTOS, 10));
    sinValorGuardado();

    assertThat(servicio.obtenerDuracion(CLAVE)).isEqualTo(Duration.ofMinutes(10));
  }

  @Test
  void horasSeEntreganComoDuracion() {
    dadaLaDefinicion(numerica(TipoParametro.HORAS, 8));
    sinValorGuardado();

    assertThat(servicio.obtenerDuracion(CLAVE)).isEqualTo(Duration.ofHours(8));
  }

  @Test
  void diasSeEntreganComoDuracion() {
    dadaLaDefinicion(numerica(TipoParametro.DIAS, 60));
    sinValorGuardado();

    assertThat(servicio.obtenerDuracion(CLAVE)).isEqualTo(Duration.ofDays(60));
  }

  @Test
  void booleanoDevuelveElValorVigente() {
    dadaLaDefinicion(
        definicion(TipoParametro.BOOLEANO, new ValorParametro.Booleano(true), null, null));
    conValorGuardado(new ValorParametro.Booleano(false));

    assertThat(servicio.obtenerBooleano(CLAVE)).isFalse();
  }

  @Test
  void listaDevuelveElValorVigente() {
    dadaLaDefinicion(
        definicion(
            TipoParametro.LISTA_ENTEROS,
            new ValorParametro.ListaEnteros(List.of(1L, 24L)),
            1L,
            48L));
    sinValorGuardado();

    assertThat(servicio.obtenerListaEnteros(CLAVE)).containsExactly(1L, 24L);
  }

  @Test
  void listaConUnElementoFueraDeRangoLanzaFueraDeRango() {
    dadaLaDefinicion(
        definicion(
            TipoParametro.LISTA_ENTEROS,
            new ValorParametro.ListaEnteros(List.of(1L, 24L)),
            1L,
            48L));
    conValorGuardado(new ValorParametro.ListaEnteros(List.of(1L, 49L)));

    assertThatThrownBy(() -> servicio.obtenerListaEnteros(CLAVE))
        .isInstanceOf(ParametroException.class);
  }

  @Test
  void enteroSobreUnParametroEnMinutosLanzaIllegalState() {
    dadaLaDefinicion(numerica(TipoParametro.MINUTOS, 10));

    assertThatThrownBy(() -> servicio.obtenerEntero(CLAVE))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void duracionSobreUnEnteroLanzaIllegalState() {
    dadaLaDefinicion(numerica(TipoParametro.ENTERO, 5));

    assertThatThrownBy(() -> servicio.obtenerDuracion(CLAVE))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void booleanoSobreUnEnteroLanzaIllegalState() {
    dadaLaDefinicion(numerica(TipoParametro.ENTERO, 5));

    assertThatThrownBy(() -> servicio.obtenerBooleano(CLAVE))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void listaSobreUnEnteroLanzaIllegalState() {
    dadaLaDefinicion(numerica(TipoParametro.ENTERO, 5));

    assertThatThrownBy(() -> servicio.obtenerListaEnteros(CLAVE))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void valorGuardadoDeOtroTipoQueElDeclaradoLanzaIllegalState() {
    dadaLaDefinicion(numerica(TipoParametro.ENTERO, 5));
    conValorGuardado(new ValorParametro.Booleano(true));

    assertThatThrownBy(() -> servicio.obtenerEntero(CLAVE))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void booleanoConValorGuardadoDeOtroTipoLanzaIllegalState() {
    dadaLaDefinicion(
        definicion(TipoParametro.BOOLEANO, new ValorParametro.Booleano(true), null, null));
    conValorGuardado(new ValorParametro.ListaEnteros(List.of()));

    assertThatThrownBy(() -> servicio.obtenerBooleano(CLAVE))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void listaConValorGuardadoDeOtroTipoLanzaIllegalState() {
    dadaLaDefinicion(
        definicion(
            TipoParametro.LISTA_ENTEROS, new ValorParametro.ListaEnteros(List.of()), 1L, 48L));
    conValorGuardado(new ValorParametro.Booleano(true));

    assertThatThrownBy(() -> servicio.obtenerListaEnteros(CLAVE))
        .isInstanceOf(IllegalStateException.class);
  }
}
