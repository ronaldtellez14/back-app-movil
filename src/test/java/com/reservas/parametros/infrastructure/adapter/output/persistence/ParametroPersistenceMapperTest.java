package com.reservas.parametros.infrastructure.adapter.output.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reservas.parametros.domain.DefinicionParametro;
import com.reservas.parametros.domain.NivelParametro;
import com.reservas.parametros.domain.TipoParametro;
import com.reservas.parametros.domain.ValorParametro;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mapstruct.factory.Mappers;

@Tag("PARAMETROS")
class ParametroPersistenceMapperTest {

  private final ParametroPersistenceMapper mapper =
      Mappers.getMapper(ParametroPersistenceMapper.class);

  private static ParametroEntity entidad(TipoParametro tipo, Long minimo, Long maximo) {
    ParametroEntity entidad = new ParametroEntity();
    entidad.setClave("parametro.de.prueba");
    entidad.setDescripcion("Parámetro de prueba");
    entidad.setTipo(tipo);
    entidad.setNivelMaximo(NivelParametro.PLATAFORMA);
    entidad.setMinimo(minimo);
    entidad.setMaximo(maximo);
    return entidad;
  }

  @ParameterizedTest
  @EnumSource(
      value = TipoParametro.class,
      names = {"ENTERO", "MINUTOS", "HORAS", "DIAS"})
  void convierteUnaDefinicionNumerica(TipoParametro tipo) {
    ParametroEntity entidad = entidad(tipo, 1L, 365L);
    entidad.setDefectoNumero(60L);

    DefinicionParametro definicion = mapper.aDominio(entidad);

    assertThat(definicion)
        .isEqualTo(
            new DefinicionParametro(
                "parametro.de.prueba",
                "Parámetro de prueba",
                tipo,
                new ValorParametro.Numero(60),
                1L,
                365L,
                NivelParametro.PLATAFORMA));
  }

  @Test
  void convierteUnaDefinicionBooleanaConMinimoYMaximoNulos() {
    ParametroEntity entidad = entidad(TipoParametro.BOOLEANO, null, null);
    entidad.setDefectoBooleano(true);

    DefinicionParametro definicion = mapper.aDominio(entidad);

    assertThat(definicion.valorPorDefecto()).isEqualTo(new ValorParametro.Booleano(true));
    assertThat(definicion.minimo()).isNull();
    assertThat(definicion.maximo()).isNull();
    assertThat(definicion.tipo()).isEqualTo(TipoParametro.BOOLEANO);
  }

  @Test
  void convierteUnaDefinicionDeLista() {
    ParametroEntity entidad = entidad(TipoParametro.LISTA_ENTEROS, 1L, 48L);
    entidad.setDefectoLista(List.of(1L, 24L));

    DefinicionParametro definicion = mapper.aDominio(entidad);

    assertThat(definicion.valorPorDefecto())
        .isEqualTo(new ValorParametro.ListaEnteros(List.of(1L, 24L)));
  }

  @Test
  void unaEntidadNulaDaNulo() {
    assertThat(mapper.aDominio(null)).isNull();
  }

  @Test
  void convierteUnValorNumerico() {
    ValorParametroEntity entidad = new ValorParametroEntity();
    entidad.setValorNumero(7L);

    assertThat(mapper.aValor(entidad)).isEqualTo(new ValorParametro.Numero(7));
  }

  @Test
  void convierteUnValorBooleano() {
    ValorParametroEntity entidad = new ValorParametroEntity();
    entidad.setValorBooleano(false);

    assertThat(mapper.aValor(entidad)).isEqualTo(new ValorParametro.Booleano(false));
  }

  @Test
  void convierteUnValorDeLista() {
    ValorParametroEntity entidad = new ValorParametroEntity();
    entidad.setValorLista(List.of(2L, 3L));

    assertThat(mapper.aValor(entidad)).isEqualTo(new ValorParametro.ListaEnteros(List.of(2L, 3L)));
  }

  @Test
  void unValorSinDatoLanzaIllegalState() {
    ValorParametroEntity entidad = new ValorParametroEntity();
    entidad.setClave("parametro.de.prueba");

    assertThatThrownBy(() -> mapper.aValor(entidad)).isInstanceOf(IllegalStateException.class);
  }
}
