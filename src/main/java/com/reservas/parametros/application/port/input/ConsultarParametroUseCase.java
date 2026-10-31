package com.reservas.parametros.application.port.input;

import java.time.Duration;
import java.util.List;

/**
 * Consulta el valor vigente de un parámetro por su clave: el personalizado de plataforma si existe;
 * si no, el valor por defecto. Las claves están en {@link ClavesParametro}.
 *
 * <p>Si la clave no existe, o el valor vigente está fuera de su mínimo y su máximo, lanza {@link
 * com.reservas.parametros.domain.ParametroException}. Si el método no corresponde al tipo del
 * parámetro, lanza {@link IllegalStateException}.
 */
public interface ConsultarParametroUseCase {

  /** Valor de un parámetro de tipo ENTERO. */
  long obtenerEntero(String clave);

  /** Valor de un parámetro de tipo MINUTOS, HORAS o DIAS, ya convertido a su unidad. */
  Duration obtenerDuracion(String clave);

  /** Valor de un parámetro de tipo BOOLEANO. */
  boolean obtenerBooleano(String clave);

  /** Valor de un parámetro de tipo LISTA_ENTEROS. */
  List<Long> obtenerListaEnteros(String clave);
}
