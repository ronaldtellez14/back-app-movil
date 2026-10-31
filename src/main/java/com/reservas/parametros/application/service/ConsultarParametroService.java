package com.reservas.parametros.application.service;

import com.reservas.parametros.application.port.input.ConsultarParametroUseCase;
import com.reservas.parametros.application.port.output.CargarParametroPort;
import com.reservas.parametros.domain.CodigoErrorParametro;
import com.reservas.parametros.domain.DefinicionParametro;
import com.reservas.parametros.domain.ParametroException;
import com.reservas.parametros.domain.TipoParametro;
import com.reservas.parametros.domain.ValorParametro;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultarParametroService implements ConsultarParametroUseCase {

  private final CargarParametroPort cargarParametroPort;

  @Override
  public long obtenerEntero(String clave) {
    DefinicionParametro definicion = buscarDefinicion(clave);
    exigirTipo(definicion, TipoParametro.ENTERO);
    return numero(definicion);
  }

  @Override
  public Duration obtenerDuracion(String clave) {
    DefinicionParametro definicion = buscarDefinicion(clave);
    return switch (definicion.tipo()) {
      case MINUTOS -> Duration.ofMinutes(numero(definicion));
      case HORAS -> Duration.ofHours(numero(definicion));
      case DIAS -> Duration.ofDays(numero(definicion));
      case ENTERO, BOOLEANO, LISTA_ENTEROS -> throw tipoEquivocado(definicion, "una duración");
    };
  }

  @Override
  public boolean obtenerBooleano(String clave) {
    DefinicionParametro definicion = buscarDefinicion(clave);
    exigirTipo(definicion, TipoParametro.BOOLEANO);
    if (valorVigente(definicion) instanceof ValorParametro.Booleano booleano) {
      return booleano.valor();
    }
    throw tipoEquivocado(definicion, "un booleano");
  }

  @Override
  public List<Long> obtenerListaEnteros(String clave) {
    DefinicionParametro definicion = buscarDefinicion(clave);
    exigirTipo(definicion, TipoParametro.LISTA_ENTEROS);
    if (valorVigente(definicion) instanceof ValorParametro.ListaEnteros lista) {
      return lista.valores();
    }
    throw tipoEquivocado(definicion, "una lista de enteros");
  }

  private DefinicionParametro buscarDefinicion(String clave) {
    return cargarParametroPort
        .buscarDefinicion(clave)
        .orElseThrow(() -> new ParametroException(CodigoErrorParametro.PARAMETRO_NO_EXISTE, clave));
  }

  /** El valor de plataforma si existe; si no, el por defecto. Siempre se valida su rango. */
  private ValorParametro valorVigente(DefinicionParametro definicion) {
    ValorParametro valor =
        cargarParametroPort
            .buscarValorDePlataforma(definicion.clave())
            .orElse(definicion.valorPorDefecto());
    definicion.validarRango(valor);
    return valor;
  }

  private long numero(DefinicionParametro definicion) {
    if (valorVigente(definicion) instanceof ValorParametro.Numero numero) {
      return numero.valor();
    }
    throw tipoEquivocado(definicion, "un número");
  }

  private static void exigirTipo(DefinicionParametro definicion, TipoParametro tipo) {
    if (definicion.tipo() != tipo) {
      throw tipoEquivocado(definicion, tipo.name());
    }
  }

  // Error de programación del módulo que consulta, no de dominio (design, decisión 2).
  private static IllegalStateException tipoEquivocado(
      DefinicionParametro definicion, String esperado) {
    return new IllegalStateException(
        "El parámetro "
            + definicion.clave()
            + " es de tipo "
            + definicion.tipo()
            + ", no "
            + esperado
            + ".");
  }
}
