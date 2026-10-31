package com.reservas.parametros.domain;

/** Error de dominio de parámetros. El mensaje es el constante del código; la clave va aparte. */
public class ParametroException extends RuntimeException {

  private final CodigoErrorParametro codigo;
  private final String clave;

  public ParametroException(CodigoErrorParametro codigo, String clave) {
    super(codigo.mensaje());
    this.codigo = codigo;
    this.clave = clave;
  }

  public CodigoErrorParametro codigo() {
    return codigo;
  }

  public String clave() {
    return clave;
  }
}
