package com.reservas.parametros.infrastructure.adapter.output.persistence;

import com.reservas.parametros.application.port.output.CargarParametroPort;
import com.reservas.parametros.domain.DefinicionParametro;
import com.reservas.parametros.domain.NivelParametro;
import com.reservas.parametros.domain.ValorParametro;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParametroPersistenceAdapter implements CargarParametroPort {

  private final ParametroJpaRepository parametroJpaRepository;
  private final ValorParametroJpaRepository valorParametroJpaRepository;
  private final ParametroPersistenceMapper mapper;

  @Override
  public Optional<DefinicionParametro> buscarDefinicion(String clave) {
    return parametroJpaRepository.findById(clave).map(mapper::aDominio);
  }

  @Override
  public Optional<ValorParametro> buscarValorDePlataforma(String clave) {
    return valorParametroJpaRepository
        .findByClaveAndNivel(clave, NivelParametro.PLATAFORMA)
        .map(mapper::aValor);
  }
}
