package com.reservas.compartido.error;

import org.springframework.http.HttpStatus;

/** Códigos y mensajes de los errores genéricos de la API. */
public enum CodigoError {
  SOLICITUD_INVALIDA(HttpStatus.BAD_REQUEST, "La solicitud tiene datos inválidos."),
  CUERPO_ILEGIBLE(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no se pudo leer."),
  NO_AUTENTICADO(HttpStatus.UNAUTHORIZED, "Debe autenticarse para acceder a este recurso."),
  ACCESO_DENEGADO(HttpStatus.FORBIDDEN, "No tiene permiso para acceder a este recurso."),
  RECURSO_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "El recurso solicitado no existe."),
  METODO_NO_PERMITIDO(HttpStatus.METHOD_NOT_ALLOWED, "El método HTTP no está permitido."),
  ERROR_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado.");

  private final HttpStatus estado;
  private final String mensaje;

  CodigoError(HttpStatus estado, String mensaje) {
    this.estado = estado;
    this.mensaje = mensaje;
  }

  public HttpStatus estado() {
    return estado;
  }

  public String mensaje() {
    return mensaje;
  }
}
