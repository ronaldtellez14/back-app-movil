package com.reservas.parametros.application.port.input;

/** Claves de los parámetros. Cada CU agrega las suyas junto con su migración. */
public final class ClavesParametro {

  // CU-00, tabla "Parámetros que usa".
  public static final String SESION_CLIENTE_DIAS_INACTIVIDAD = "sesion.cliente.dias_inactividad";
  public static final String SESION_NEGOCIO_DIAS_INACTIVIDAD = "sesion.negocio.dias_inactividad";
  public static final String SESION_PANEL_HORAS = "sesion.panel.horas";
  public static final String CODIGO_VERIFICACION_DIGITOS = "codigo_verificacion.digitos";
  public static final String CODIGO_VERIFICACION_MINUTOS_VIGENCIA =
      "codigo_verificacion.minutos_vigencia";
  public static final String CODIGO_VERIFICACION_INTENTOS = "codigo_verificacion.intentos";
  public static final String CONTRASENA_INTENTOS_FALLIDOS = "contrasena.intentos_fallidos";
  public static final String CONTRASENA_MINUTOS_BLOQUEO = "contrasena.minutos_bloqueo";

  private ClavesParametro() {}
}
