package com.reservas.parametros.application.port.output;

import com.reservas.parametros.domain.DefinicionParametro;
import com.reservas.parametros.domain.ValorParametro;
import java.util.Optional;

/** Lectura de definiciones y valores personalizados de parámetros. */
public interface CargarParametroPort {

  Optional<DefinicionParametro> buscarDefinicion(String clave);

  Optional<ValorParametro> buscarValorDePlataforma(String clave);
}
