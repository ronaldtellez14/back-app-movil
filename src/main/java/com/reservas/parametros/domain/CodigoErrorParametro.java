package com.reservas.parametros.domain;

/** Códigos y mensajes de los errores del módulo de parámetros. */
public enum CodigoErrorParametro {
  PARAMETRO_NO_EXISTE("El parámetro solicitado no existe."),
  PARAMETRO_FUERA_DE_RANGO("El valor vigente del parámetro está fuera de su mínimo y su máximo.");

  private final String mensaje;

  CodigoErrorParametro(String mensaje) {
    this.mensaje = mensaje;
  }

  public String mensaje() {
    return mensaje;
  }
}
