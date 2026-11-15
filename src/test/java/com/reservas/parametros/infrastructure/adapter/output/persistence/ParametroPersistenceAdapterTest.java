package com.reservas.parametros.infrastructure.adapter.output.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.reservas.parametros.domain.DefinicionParametro;
import com.reservas.parametros.domain.NivelParametro;
import com.reservas.parametros.domain.TipoParametro;
import com.reservas.parametros.domain.ValorParametro;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("PARAMETROS")
@ExtendWith(MockitoExtension.class)
class ParametroPersistenceAdapterTest {

  private static final String CLAVE = "parametro.de.prueba";

  @Mock private ParametroJpaRepository parametroJpaRepository;
  @Mock private ValorParametroJpaRepository valorParametroJpaRepository;
  @Mock private ParametroPersistenceMapper mapper;

  @InjectMocks private ParametroPersistenceAdapter adaptador;

  @Test
  void devuelveLaDefinicionEncontrada() {
    ParametroEntity entidad = new ParametroEntity();
    DefinicionParametro definicion =
        new DefinicionParametro(
            CLAVE,
            "Parámetro de prueba",
            TipoParametro.ENTERO,
            new ValorParametro.Numero(5),
            1L,
            10L,
            NivelParametro.PLATAFORMA);
    when(parametroJpaRepository.findById(CLAVE)).thenReturn(Optional.of(entidad));
    when(mapper.aDominio(entidad)).thenReturn(definicion);

    assertThat(adaptador.buscarDefinicion(CLAVE)).contains(definicion);
  }

  @Test
  void devuelveVacioSiLaDefinicionNoExiste() {
    when(parametroJpaRepository.findById(CLAVE)).thenReturn(Optional.empty());

    assertThat(adaptador.buscarDefinicion(CLAVE)).isEmpty();
  }

  @Test
  void devuelveElValorDePlataformaPresente() {
    ValorParametroEntity entidad = new ValorParametroEntity();
    ValorParametro valor = new ValorParametro.Numero(7);
    when(valorParametroJpaRepository.findByClaveAndNivel(CLAVE, NivelParametro.PLATAFORMA))
        .thenReturn(Optional.of(entidad));
    when(mapper.aValor(entidad)).thenReturn(valor);

    assertThat(adaptador.buscarValorDePlataforma(CLAVE)).contains(valor);
  }

  @Test
  void devuelveVacioSiNoHayValorDePlataforma() {
    when(valorParametroJpaRepository.findByClaveAndNivel(CLAVE, NivelParametro.PLATAFORMA))
        .thenReturn(Optional.empty());

    assertThat(adaptador.buscarValorDePlataforma(CLAVE)).isEmpty();
  }
}
